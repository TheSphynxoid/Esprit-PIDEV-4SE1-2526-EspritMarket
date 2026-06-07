import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ApiConfigService } from '../../../services/api/api-config.service';
import { EmailService } from '../../../services/api/email.service';
import { IndexedDbService } from '../../../services/indexed-db.service';
import { FooterComponent } from '../../../shared/layout';
import { catchError, finalize, timeout } from 'rxjs/operators';
import { of } from 'rxjs';

import { CommonModule } from '@angular/common';

interface StudentRequest {
  id: number;
  numeroEtudiant: string;
  prenom: string;
  nom: string;
  email: string;
  carteEtudiantUrl?: string;
  statut: string;
  syncedWithBackend?: boolean;
}

@Component({
  selector: 'app-admin-market',
  standalone: true,
  imports: [CommonModule, FooterComponent],
  templateUrl: './admin-market.component.html',
  styleUrls: ['./admin-market.component.css']
})
export class AdminMarketComponent implements OnInit, OnDestroy {
  private static readonly REQUESTS_CACHE_KEY = 'admin_market_requests_cache';
  private static readonly APPROVED_EVENTS_KEY = 'admin_market_approved_events';
  private static readonly LOCAL_CARD_FALLBACK = '/assets/student-card-placeholder.svg';
  private static readonly STUDENT_CARD_REGISTRY_KEY = 'admin_market_student_cards';

  activeTab = 'overview';
  studentRequests: StudentRequest[] = [];
  loading = true;
  error: string | null = null;
  selectedCard: string | null = null;
  cardLoadError: string | null = null;
  showCardModal = false;
  private selectedCardObjectUrl: string | null = null;
  private requestSequence = 0;
  private lastFetchFailed = false;
  private cachePollingInterval: any;

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private router: Router,
    private apiConfig: ApiConfigService,
    private emailService: EmailService,
    private indexedDb: IndexedDbService
  ) { }

  get pendingRequestsCount(): number {
    return this.studentRequests.filter((request) => request.statut === 'EN_ATTENTE').length;
  }

  get syncedRequestsCount(): number {
    return this.studentRequests.filter((request) => request.syncedWithBackend === true).length;
  }

  get unsyncedRequestsCount(): number {
    return this.studentRequests.filter((request) => request.syncedWithBackend !== true).length;
  }

  ngOnInit(): void {
    this.loadRequestsFromCache();
    // Force refresh to ensure we always have latest state
    setTimeout(() => {
      this.fetchStudentRequests();
    }, 100);
    
    // Poll cache every 2 seconds to detect changes from other components (e.g., create-store)
    this.cachePollingInterval = setInterval(() => {
      const rawCache = localStorage.getItem(AdminMarketComponent.REQUESTS_CACHE_KEY);
      if (rawCache) {
        try {
          const cachedRequests = JSON.parse(rawCache);
          if (Array.isArray(cachedRequests)) {
            const cacheLength = cachedRequests.length;
            const currentLength = this.studentRequests.length;
            
            if (cacheLength > currentLength) {
              console.log(`[POLLING] Cache changed: ${currentLength} -> ${cacheLength} items, reloading...`);
              this.loadRequestsFromCache();
            } else if (cacheLength < currentLength) {
              console.log(`[POLLING] Cache shrunk: ${currentLength} -> ${cacheLength} items, reloading...`);
              this.loadRequestsFromCache();
            }
          }
        } catch (e) {
          console.error('[POLLING] Error parsing cache:', e);
        }
      }
    }, 2000);
  }

  ngOnDestroy(): void {
    if (this.cachePollingInterval) {
      clearInterval(this.cachePollingInterval);
    }
  }

  goToMarketplace(): void {
    this.router.navigate(['/marketplace']);
  }

  fetchStudentRequests(): void {
    const requestId = ++this.requestSequence;
    this.lastFetchFailed = false;
    this.loading = this.studentRequests.length === 0;
    this.error = null;
    const url = this.apiConfig.buildUrl('/api/marketplace/seller-requests');

    const failSafeTimer = window.setTimeout(() => {
      if (requestId !== this.requestSequence) {
        return;
      }
      this.error = 'Le chargement a expiré. Vérifiez la connexion backend et réessayez.';
      this.loading = false;
    }, 12000);

    this.http.get<unknown>(url).pipe(
      timeout(10000),
      catchError((err) => {
        if (requestId !== this.requestSequence) {
          return of([] as unknown);
        }
        this.lastFetchFailed = true;
        this.error = err?.name === 'TimeoutError'
          ? 'Le serveur met trop de temps à répondre (timeout).'
          : (err?.status
            ? `Impossible de récupérer les demandes (code ${err.status}).`
            : 'Impossible de récupérer les demandes.');
        return of([] as unknown);
      }),
      finalize(() => {
        window.clearTimeout(failSafeTimer);
        if (requestId === this.requestSequence) {
          this.loading = false;
        }
      })
    ).subscribe({
      next: (data) => {
        if (requestId !== this.requestSequence) {
          return;
        }

        if (this.lastFetchFailed) {
          // Keep previous list/cache when backend call failed.
          return;
        }

        const requests = this.normalizeRequests(data);
        const mergedRequests = this.mergeWithCachedRequests(requests);
        this.studentRequests = mergedRequests;
        this.saveRequestsToCache(mergedRequests);
        this.error = null;
      },
      error: () => {
        if (requestId !== this.requestSequence) {
          return;
        }
        if (this.studentRequests.length === 0) {
          this.error = this.error ?? 'Aucune donnée disponible.';
        }
      }
    });
  }

  private loadRequestsFromCache(): void {
    try {
      const raw = localStorage.getItem(AdminMarketComponent.REQUESTS_CACHE_KEY);
      if (!raw) {
        console.log('No cached requests found for admin market');
        return;
      }
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed) && parsed.length > 0) {
        this.studentRequests = parsed.map((item) => ({
          ...(item as StudentRequest),
          syncedWithBackend: (item as StudentRequest)?.syncedWithBackend === true
        }));
        console.log(`Loaded ${this.studentRequests.length} cached requests from localStorage`, this.studentRequests);
      } else {
        console.warn('Cached data exists but is not a valid array or is empty', parsed);
      }
    } catch (e) {
      console.error('Error loading cached requests:', e);
      this.studentRequests = [];
    }
  }

  private saveRequestsToCache(requests: StudentRequest[]): void {
    try {
      localStorage.setItem(AdminMarketComponent.REQUESTS_CACHE_KEY, JSON.stringify(requests));
    } catch {
    }
  }

  private normalizeRequests(data: unknown): StudentRequest[] {
    const registry = this.loadStudentCardRegistry();

    const sanitize = (requests: unknown[]): StudentRequest[] =>
      requests
        .filter((request): request is Record<string, unknown> => !!request && typeof request === 'object')
        .map((request) => {
          const rawId = request['id'];
          const normalizedId = typeof rawId === 'number'
            ? rawId
            : typeof rawId === 'string'
              ? Number(rawId)
              : Date.now() + Math.floor(Math.random() * 1000);

          const email = String(request['email'] ?? '');
          const numeroEtudiant = String(request['numeroEtudiant'] ?? '');
          const key = `${email.toLowerCase()}|${numeroEtudiant.toLowerCase()}`;
          const registryCard = registry[key];
          const apiCard = typeof request['carteEtudiantUrl'] === 'string'
            ? request['carteEtudiantUrl'].trim()
            : undefined;

          return {
            id: Number.isFinite(normalizedId) ? normalizedId : Date.now(),
            numeroEtudiant,
            prenom: String(request['prenom'] ?? ''),
            nom: String(request['nom'] ?? ''),
            email,
            carteEtudiantUrl: registryCard || apiCard,
            statut: String(request['statut'] ?? 'EN_ATTENTE'),
            syncedWithBackend: true
          };
        })
        .filter((request) => !!request.email || !!request.numeroEtudiant);

    if (Array.isArray(data)) {
      return sanitize(data);
    }

    if (data && typeof data === 'object') {
      const payload = data as {
        data?: unknown;
        content?: unknown;
        requests?: unknown;
        items?: unknown;
        results?: unknown;
        sellerRequests?: unknown;
      };

      if (Array.isArray(payload.data)) {
        return sanitize(payload.data);
      }

      if (Array.isArray(payload.content)) {
        return sanitize(payload.content);
      }

      if (Array.isArray(payload.requests)) {
        return sanitize(payload.requests);
      }

      if (Array.isArray(payload.items)) {
        return sanitize(payload.items);
      }

      if (Array.isArray(payload.results)) {
        return sanitize(payload.results);
      }

      if (Array.isArray(payload.sellerRequests)) {
        return sanitize(payload.sellerRequests);
      }

      // Fallback: backend may return array under another key.
      for (const value of Object.values(payload)) {
        if (Array.isArray(value)) {
          const candidate = sanitize(value);
          if (candidate.length > 0) {
            return candidate;
          }
        }
      }
    }

    return [];
  }

  private loadStudentCardRegistry(): Record<string, string> {
    try {
      const raw = localStorage.getItem(AdminMarketComponent.STUDENT_CARD_REGISTRY_KEY);
      if (!raw) {
        return {};
      }

      const parsed = JSON.parse(raw);
      return typeof parsed === 'object' && parsed ? parsed as Record<string, string> : {};
    } catch {
      return {};
    }
  }

  private mergeWithCachedRequests(fetchedRequests: StudentRequest[]): StudentRequest[] {
    // Load the latest cache from localStorage (not the old this.studentRequests)
    let cachedRequests: StudentRequest[] = [];
    try {
      const rawCache = localStorage.getItem(AdminMarketComponent.REQUESTS_CACHE_KEY);
      if (rawCache) {
        const parsed = JSON.parse(rawCache);
        if (Array.isArray(parsed)) {
          cachedRequests = parsed;
        }
      }
    } catch (e) {
      console.error('Error loading cache for merge:', e);
    }

    console.log('mergeWithCachedRequests: cached items from localStorage:', cachedRequests.length);
    console.log('mergeWithCachedRequests: fetched items from backend:', fetchedRequests.length);

    const byKey = new Map<string, StudentRequest>();

    const requestKey = (request: StudentRequest): string => {
      const email = (request.email || '').toLowerCase();
      const studentNumber = (request.numeroEtudiant || '').toLowerCase();

      if (email && studentNumber) {
        return `email:${email}|num:${studentNumber}`;
      }

      if (email) {
        return `email:${email}`;
      }

      if (studentNumber) {
        return `num:${studentNumber}`;
      }

      if (typeof request?.id === 'number' && Number.isFinite(request.id) && request.id > 0) {
        return `id:${request.id}`;
      }

      return `tmp:${Math.random()}`;
    };

    const isPlaceholderUrl = (url?: string): boolean => {
      if (!url) {
        return true;
      }

      const value = url.trim().toLowerCase();
      return (
        value.includes('placeholder.com/student-card.jpg') ||
        value.endsWith('/assets/student-card-placeholder.svg') ||
        value.endsWith('assets/student-card-placeholder.svg')
      );
    };

    const pickBetterRequest = (current: StudentRequest, incoming: StudentRequest): StudentRequest => {
      const currentCard = current.carteEtudiantUrl;
      const incomingCard = incoming.carteEtudiantUrl;
      const currentIsBetterCard = !!currentCard && !isPlaceholderUrl(currentCard);
      const incomingIsBetterCard = !!incomingCard && !isPlaceholderUrl(incomingCard);

      if (incomingIsBetterCard && !currentIsBetterCard) {
        return { ...current, ...incoming, carteEtudiantUrl: incomingCard };
      }

      if (currentIsBetterCard && !incomingIsBetterCard) {
        return { ...current, ...incoming, carteEtudiantUrl: currentCard };
      }

      return {
        ...current,
        ...incoming,
        syncedWithBackend: current.syncedWithBackend === true || incoming.syncedWithBackend === true
      };
    };

    const isPendingRequest = (status: string | undefined): boolean => String(status || '').toUpperCase() === 'EN_ATTENTE';

    // First pass: add backend requests
    for (const request of fetchedRequests) {
      if (request) {
        byKey.set(requestKey(request), request);
      }
    }

    // Second pass: merge or add cached requests, preserving local pending items
    for (const cachedRequest of cachedRequests) {
      if (!cachedRequest) {
        continue;
      }

      const key = requestKey(cachedRequest);
      const fetchedRequest = byKey.get(key);
      
      if (fetchedRequest) {
        // Merge: backend data with better cached data (card, etc)
        byKey.set(key, pickBetterRequest(fetchedRequest, { ...cachedRequest, syncedWithBackend: true }));
      } else if (isPendingRequest(cachedRequest.statut)) {
        // Add: local pending request if not in backend
        byKey.set(key, {
          ...cachedRequest,
          syncedWithBackend: false
        });
        console.log('Added pending cached request:', cachedRequest);
      }
    }

    const statusPriority = (request: StudentRequest): number => {
      const status = String(request.statut || '').toUpperCase();
      if (status === 'EN_ATTENTE' && request.syncedWithBackend === false) {
        return 0; // Unsynced pending first
      }
      if (status === 'EN_ATTENTE') {
        return 1; // Synced pending second
      }
      return 2; // Treated last
    };

    const result = Array.from(byKey.values()).sort((a, b) => {
      const priorityDiff = statusPriority(a) - statusPriority(b);
      if (priorityDiff !== 0) {
        return priorityDiff;
      }
      return (b.id ?? 0) - (a.id ?? 0);
    });

    const pendingOnly = result.filter((request) => String(request.statut || '').toUpperCase() === 'EN_ATTENTE');

    console.log(`Merged ${fetchedRequests.length} backend requests with ${this.studentRequests.length} cached. Final pending: ${pendingOnly.length}`, pendingOnly);
    return pendingOnly;
  }

  private removeRequestFromState(request: StudentRequest): void {
    const requestId = typeof request?.id === 'number' ? request.id : NaN;
    const requestEmail = String(request?.email || '').toLowerCase();
    const requestStudentNumber = String(request?.numeroEtudiant || '').toLowerCase();

    this.studentRequests = this.studentRequests.filter((item) => {
      const sameId = Number.isFinite(requestId) && item.id === requestId;
      const sameBusinessKey =
        String(item.email || '').toLowerCase() === requestEmail &&
        String(item.numeroEtudiant || '').toLowerCase() === requestStudentNumber;
      return !sameId && !sameBusinessKey;
    });

    this.saveRequestsToCache(this.studentRequests);

    const cardKey = `${requestEmail}|${requestStudentNumber}`;
    if (cardKey !== '|') {
      this.indexedDb.deleteCard(cardKey).catch(() => {
        // Best effort cleanup only.
      });
    }
  }

  private sendApprovalEmailForStep2(request: StudentRequest): void {
    const recipientEmail = String(request.email || '').trim().toLowerCase();
    if (!recipientEmail) {
      alert('Demande approuvee, mais email etudiant introuvable.');
      return;
    }

    const nextSteps = [
      'Connectez-vous a votre compte Esprit Market',
      'Passez a l\'etape 2: Creation de votre boutique',
      'Completez les informations de votre magasin',
      'Ajoutez vos premiers produits'
    ];

    this.emailService.sendApprovalEmail(
      recipientEmail,
      `${request.prenom} ${request.nom}`,
      nextSteps
    ).subscribe(
      (emailResponse) => {
        if (emailResponse.success) {
          // Silent success: approval has already been completed.
        } else {
          console.warn('Email approbation non envoye:', emailResponse.error || emailResponse.message || 'Echec inconnu');
        }
      },
      (emailError) => {
        console.warn('Notification email indisponible (non bloquant).');
      }
    );
  }

  private markRequestApprovedLocally(request: StudentRequest): void {
    try {
      const raw = localStorage.getItem(AdminMarketComponent.APPROVED_EVENTS_KEY);
      const events = raw ? JSON.parse(raw) as Record<string, number> : {};
      const key = `${String(request.email || '').toLowerCase()}|${String(request.numeroEtudiant || '').toLowerCase()}`;
      if (key !== '|') {
        events[key] = Date.now();
        localStorage.setItem(AdminMarketComponent.APPROVED_EVENTS_KEY, JSON.stringify(events));
      }
    } catch {
      // Best effort only.
    }
  }

  private resolveAndRetryApprove(request: StudentRequest, adminId: number): void {
    const listUrl = this.apiConfig.buildUrl('/api/marketplace/seller-requests');

    this.http.get<unknown>(listUrl).subscribe(
      (data) => {
        const backendRequests = this.normalizeRequests(data);
        const requestEmail = String(request.email || '').toLowerCase();
        const requestStudentNumber = String(request.numeroEtudiant || '').toLowerCase();

        const matched = backendRequests.find((item) =>
          String(item.email || '').toLowerCase() === requestEmail &&
          String(item.numeroEtudiant || '').toLowerCase() === requestStudentNumber
        );

        if (!matched || !matched.id) {
          // Backend entry not yet visible; accept locally to unblock student flow.
          this.approveRequestLocallyFallback(request);
          return;
        }

        const retryUrl = this.apiConfig.buildUrl(`/api/marketplace/seller-requests/${matched.id}/approve/${adminId}`);
        this.http.post(retryUrl, { status: 'APPROVED' }).subscribe(
          () => {
            this.markRequestApprovedLocally(request);
            this.removeRequestFromState(request);
            this.sendApprovalEmailForStep2(request);
          },
          (retryError) => {
            console.error('Erreur lors de la seconde tentative d\'approbation :', retryError);
            if (retryError?.status === 409) {
              // Consider already-treated conflict as approved state.
              this.markRequestApprovedLocally(request);
              this.removeRequestFromState(request);
              this.sendApprovalEmailForStep2(request);
              return;
            }

            // Keep UX unblocked: fallback to local approval signal.
            this.approveRequestLocallyFallback(request);
          }
        );
      },
      (listError) => {
        console.error('Erreur lors de la resolution de la demande backend :', listError);
        this.approveRequestLocallyFallback(request);
      }
    );
  }

  private approveRequestLocallyFallback(request: StudentRequest): void {
    this.markRequestApprovedLocally(request);
    this.removeRequestFromState(request);
    this.sendApprovalEmailForStep2(request);
  }

  approveRequest(index: number): void {
    const request = this.studentRequests[index];
    const admin = this.authService.currentUser();

    if (!admin || admin.id === 0) {
      alert('Session admin expirée. Veuillez vous reconnecter.');
      return;
    }

    if (!request) {
      alert('Demande introuvable.');
      return;
    }

    const looksLikeTemporaryId = request.id > 1000000000000;
    if (looksLikeTemporaryId || !request.syncedWithBackend) {
      // Resolve real backend id from business keys before approval.
      this.resolveAndRetryApprove(request, admin.id);
      return;
    }

    if (!request || !request.id) {
      alert('Erreur : Demande invalide. Impossible de l\'approuver.');
      console.error('Demande invalide pour approbation', request);
      return;
    }

    const url = this.apiConfig.buildUrl(`/api/marketplace/seller-requests/${request.id}/approve/${admin.id}`);
    console.log('URL d\'approbation:', url);
    console.log('Request ID:', request.id);
    console.log('Admin ID:', admin.id);

    this.http.post(url, { status: 'APPROVED' }).subscribe(
      (response) => {
        console.log('Réponse du serveur après approbation:', response);
        this.markRequestApprovedLocally(request);
        this.removeRequestFromState(request);
        this.sendApprovalEmailForStep2(request);
      },
      (error) => {
        console.error('Erreur lors de l\'approbation de la demande :', error);
        console.error('Détails erreur:', error.error);

        if ((error.status === 404 || error.status === 409) && admin?.id) {
          this.resolveAndRetryApprove(request, admin.id);
          return;
        }
        
        let errorMessage = 'Erreur lors de l\'approbation';
        if (error.status === 0) {
          errorMessage = 'Impossible de se connecter au serveur. Vérifiez la connexion.';
        } else if (error.status === 400) {
          errorMessage = 'Erreur 400 : Demande malformée.';
        } else if (error.status === 401) {
          errorMessage = 'Erreur 401 : Non autorisé. Reconnecter.';
        } else if (error.status === 403) {
          errorMessage = 'Erreur 403 : Vous n\'avez pas les droits.';
        } else if (error.status === 404) {
          errorMessage = 'Erreur 404 : Endpoint non trouvé. L\'URL est peut-être incorrecte.';
        } else if (error.status === 409) {
          errorMessage = 'Erreur 409 : Conflit. Déjà traitée.';
        } else if (error.status === 500) {
          const detail = error.error?.message || error.error?.error || JSON.stringify(error.error);
          errorMessage = `Erreur serveur (500). Détail: ${detail || 'Aucun détail'}`;
        } else {
          errorMessage = `Erreur ${error.status}`;
        }

        alert(errorMessage);
      }
    );
  }

  rejectRequest(index: number): void {
    const request = this.studentRequests[index];
    const admin = this.authService.currentUser();

    if (!admin || admin.id === 0) {
      alert('Session admin expirée. Veuillez vous reconnecter.');
      return;
    }

    if (!request) {
      alert('Demande introuvable.');
      return;
    }

    const looksLikeTemporaryId = request.id > 1000000000000;
    if (looksLikeTemporaryId) {
      alert('Cette demande est encore en synchronisation avec le serveur. Reessayez dans quelques secondes.');
      return;
    }

    if (!request.syncedWithBackend) {
      alert('Cette demande est encore en synchronisation avec le serveur. Réessayez dans quelques secondes.');
      return;
    }

    if (!request || !request.id) {
      alert('Erreur : Demande invalide. Impossible de la rejeter.');
      console.error('Demande invalide pour rejet', request);
      return;
    }

    // Confirmation simple
    const confirmed = confirm(`Êtes-vous sûr de vouloir rejeter la demande de ${request.prenom} ${request.nom} (${request.email}) ?`);
    if (!confirmed) {
      return;
    }

    const url = this.apiConfig.buildUrl(`/api/marketplace/seller-requests/${request.id}/refuse/${admin.id}`);
    console.log('URL de rejet:', url);
    console.log('Request ID:', request.id);
    console.log('Admin ID:', admin.id);

    this.http.post(url, { status: 'REFUSED' }).subscribe(
      (response) => {
        console.log('Réponse du serveur après rejet:', response);
        this.removeRequestFromState(request);
        
        // Envoi de l'email de rejet (sans raison)
        this.emailService.sendRejectionEmail(
          request.email,
          `${request.prenom} ${request.nom}`
        ).subscribe(
          (emailResponse) => {
            if (emailResponse.success) {
              alert(`Demande refusée et email envoyé à ${request.email}`);
            } else {
              alert(`Demande refusée. Email non envoyé.`);
            }
          },
          (emailError) => {
            console.error('Erreur lors de l\'envoi de l\'email :', emailError);
            alert('Demande refusée');
          }
        );
      },
      (error) => {
        console.error('Erreur lors du rejet de la demande :', error);
        console.error('Détails erreur:', error.error);
        
        let errorMessage = 'Erreur lors du rejet';
        if (error.status === 0) {
          errorMessage = 'Impossible de se connecter au serveur. Vérifiez la connexion.';
        } else if (error.status === 400) {
          errorMessage = 'Erreur 400 : Demande malformée.';
        } else if (error.status === 401) {
          errorMessage = 'Erreur 401 : Non autorisé. Reconnecter.';
        } else if (error.status === 403) {
          errorMessage = 'Erreur 403 : Vous n\'avez pas les droits.';
        } else if (error.status === 404) {
          errorMessage = 'Erreur 404 : Endpoint non trouvé. L\'URL est peut-être incorrecte.';
        } else if (error.status === 409) {
          errorMessage = 'Erreur 409 : Conflit. Déjà traitée.';
        } else if (error.status === 500) {
          const detail = error.error?.message || error.error?.error || JSON.stringify(error.error);
          errorMessage = `Erreur serveur (500). Détail: ${detail || 'Aucun détail'}`;
        } else {
          errorMessage = `Erreur ${error.status}`;
        }

        if (error.status === 404 || error.status === 409) {
          const removeLocally = confirm('Cette demande semble déjà traitée côté serveur. Voulez-vous la supprimer de la liste locale ?');
          if (removeLocally) {
            this.removeRequestFromState(request);
          }
        }
        
        alert(errorMessage);
      }
    );
  }

  deleteRequest(index: number): void {
    const request = this.studentRequests[index];
    if (!request) {
      return;
    }

    const confirmed = confirm(`Supprimer cette demande de ${request.prenom} ${request.nom} (${request.email}) ?`);
    if (!confirmed) {
      return;
    }

    this.removeRequestFromState(request);
    alert('Demande supprimee de la liste locale.');
  }

  openCardModal(cardUrl: string, request?: StudentRequest): void {
    console.log('Opening card modal with URL:', cardUrl);

    if (!cardUrl || cardUrl.includes('placeholder.com/student-card.jpg')) {
      console.warn('Card URL is placeholder or empty, cannot display');
      alert('Aucune carte valide disponible pour cette demande.');
      return;
    }

    if (request) {
      const key = `${(request.email || '').toLowerCase()}|${(request.numeroEtudiant || '').toLowerCase()}`;
      this.indexedDb.getCard(key)
        .then((file) => {
          if (file) {
            if (this.selectedCardObjectUrl) {
              URL.revokeObjectURL(this.selectedCardObjectUrl);
              this.selectedCardObjectUrl = null;
            }
            const objectUrl = URL.createObjectURL(file);
            this.selectedCardObjectUrl = objectUrl;
            this.selectedCard = objectUrl;
            this.cardLoadError = null;
            this.showCardModal = true;
            return;
          }

          this.openCardFromUrl(cardUrl);
        })
        .catch((err) => {
          console.warn('IndexedDB read failed, fallback URL will be used.', err);
          this.openCardFromUrl(cardUrl);
        });
      return;
    }

    this.openCardFromUrl(cardUrl);
  }

  private openCardFromUrl(cardUrl: string): void {
    const resolved = this.resolveCardUrl(cardUrl);
    console.log('Resolved card URL:', resolved);

    this.selectedCard = resolved;
    this.cardLoadError = null;
    this.showCardModal = true;
  }

  closeCardModal(): void {
    if (this.selectedCardObjectUrl) {
      URL.revokeObjectURL(this.selectedCardObjectUrl);
      this.selectedCardObjectUrl = null;
    }
    this.showCardModal = false;
    this.selectedCard = null;
    this.cardLoadError = null;
  }

  onCardImageError(): void {
    if (this.selectedCard !== AdminMarketComponent.LOCAL_CARD_FALLBACK) {
      this.selectedCard = AdminMarketComponent.LOCAL_CARD_FALLBACK;
      this.cardLoadError = null;
      return;
    }

    this.cardLoadError = 'Impossible de charger cette image. Vérifiez que le backend expose correctement le fichier.';
  }

  private resolveCardUrl(cardUrl: string): string {
    const trimmed = cardUrl.trim();

    if (trimmed.startsWith('data:image/')) {
      return trimmed;
    }

    if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
      return trimmed;
    }

    if (trimmed.startsWith('/assets/')) {
      return trimmed;
    }

    if (trimmed.startsWith('assets/')) {
      return `/${trimmed}`;
    }

    if (trimmed.startsWith('/')) {
      return this.apiConfig.buildUrl(trimmed);
    }

    return this.apiConfig.buildUrl(`/${trimmed}`);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}