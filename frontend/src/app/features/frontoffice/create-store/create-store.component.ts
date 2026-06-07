import { ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ApiConfigService } from '../../../services/api/api-config.service';
import { IndexedDbService } from '../../../services/indexed-db.service';
import { MarketplaceApiService, ProductCategoryOption, ProductService } from '../../../services';
import { ProductFormComponent } from '../../../shared/components';
import type { ProductFormData } from '../../../shared/components/product-form.component';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-create-store',
  standalone: true,
  imports: [CommonModule, FormsModule, ProductFormComponent, HeaderComponent, FooterComponent],
  templateUrl: './create-store.component.html',
  styleUrls: ['./create-store.component.css']
})
export class CreateStoreComponent implements OnInit, OnDestroy {
  private static readonly ADMIN_REQUESTS_CACHE_KEY = 'admin_market_requests_cache';
  private static readonly APPROVED_EVENTS_KEY = 'admin_market_approved_events';
  private static readonly LAST_REQUEST_KEY = 'create_store_last_request';
  private static readonly ALLOW_STEP2_KEY = 'create_store_allow_step2';
  private statusCheckInterval: any = null;
  private heartbeatInterval: any = null;
  private isStatusEndpointUnavailable = false;
  private step2NavigationDone = false;
  private hasSubmittedCurrentSession = false;
  private hasStartedFormInput = false;
  private currentSubmissionAt = 0;
  private isRecoveringStatusSession = false;

  private readonly onStorageChange = (event: StorageEvent): void => {
    if (
      event.key === CreateStoreComponent.APPROVED_EVENTS_KEY ||
      event.key === CreateStoreComponent.ADMIN_REQUESTS_CACHE_KEY ||
      event.key === CreateStoreComponent.LAST_REQUEST_KEY
    ) {
      this.checkLocalApprovalSignal();
      this.checkRequestStatus();
    }
  };
  private readonly onWindowFocus = (): void => {
    if (this.isRequestSent && !this.isApproved) this.refreshStatus();
  };
  private readonly onVisibilityChange = (): void => {
    if (!document.hidden && this.isRequestSent && !this.isApproved) this.refreshStatus();
  };

  isRequestSent = false;
  status: string = '';
  isApproved = false;
  submitAttempted = false;
  step1Form = {
    studentNumber: '',
    firstName: '',
    lastName: '',
    email: ''
  };
  hasFiveOrMoreArticles = false;
  hasSelectedStudentCard = false;
  studentCardError = '';
  selectedStudentCardFile: File | null = null;
  studentCardPreviewUrl: string | null = null;
  selectedStudentCardDataUrl: string | null = null;
  private lastSubmitPayload: {
    numeroEtudiant: string;
    prenom: string;
    nom: string;
    email: string;
    carteEtudiantUrl: string;
    hasFiveOrMoreArticles: boolean;
  } | null = null;
  private lastTemporaryId: number | null = null;

  @ViewChild(ProductFormComponent) productFormComponent?: ProductFormComponent;
  isSubmittingProduct = false;
  productSubmitMessage = '';
  productSubmitError = '';
  productCategories: ProductCategoryOption[] = [];
  isLoadingProductCategories = false;
  isResolvingProductStore = false;
  productCategoriesError = '';
  storeIdForProductCreation = 0;
  isProductAddedSuccess = false;

  // ─── Variables pour les 2 onglets ───────────────────────────────
  selectedVerificationMethod: 'card' | 'email' = 'card';
  outlookEmail = '';
  verificationCode = '';
  codeSent = false;
  isSendingCode = false;
  isVerifyingCode = false;
  codeError = '';
  codeSentMessage = '';

  constructor(
    private http: HttpClient,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
    private authService: AuthService,
    private apiConfig: ApiConfigService,
    private indexedDb: IndexedDbService,
    private marketplaceApi: MarketplaceApiService,
    private productService: ProductService
  ) { }

  private triggerUiUpdate(): void {
    try { this.cdr.detectChanges(); } catch { }
  }

  get shouldOpenStoreStep2(): boolean {
    return this.lastSubmitPayload?.hasFiveOrMoreArticles === true || this.hasFiveOrMoreArticles === true;
  }

  ngOnInit() {
    // Remplit automatiquement avec les infos de l'utilisateur connecté
    const currentUser = this.authService.currentUser();
    if (currentUser) {
      const fullName = currentUser.name || '';
      const nameParts = fullName.trim().split(' ');
      this.step1Form.firstName = nameParts[0] || '';
      this.step1Form.lastName = nameParts.slice(1).join(' ') || '';
      this.step1Form.email = currentUser.email || '';
      this.outlookEmail = currentUser.email || '';
    }

    this.isStatusEndpointUnavailable = false;
    const isFreshFlow = this.route.snapshot.queryParamMap.get('fresh') === '1';
    if (isFreshFlow) {
      this.isRequestSent = false;
      this.status = '';
      this.isApproved = false;
      this.hasFiveOrMoreArticles = false;
      this.lastSubmitPayload = null;
      this.step2NavigationDone = false;
      localStorage.removeItem(CreateStoreComponent.LAST_REQUEST_KEY);
      localStorage.removeItem(CreateStoreComponent.ALLOW_STEP2_KEY);
    }

    window.addEventListener('storage', this.onStorageChange);
    window.addEventListener('focus', this.onWindowFocus);
    document.addEventListener('visibilitychange', this.onVisibilityChange);

    if (!isFreshFlow) this.restorePendingRequestFromCache();

    // ===== CLÉE: Toujours nettoyer le flag au démarrage =====
    // Ne jamais faire confiance à localStorage seul
    localStorage.removeItem(CreateStoreComponent.ALLOW_STEP2_KEY);
    this.isApproved = false;
    this.status = '';
    this.isRequestSent = false;
    
    // Vérifier STRICTEMENT le serveur
    this.verifyServerApprovalStatusAndUpdate();

    if (this.isRequestSent && !this.isApproved) this.startStatusPolling();
    this.startHeartbeat();
  }

  ngOnDestroy(): void {
    window.removeEventListener('storage', this.onStorageChange);
    window.removeEventListener('focus', this.onWindowFocus);
    document.removeEventListener('visibilitychange', this.onVisibilityChange);
    this.revokePreviewUrl();
    if (this.statusCheckInterval) clearInterval(this.statusCheckInterval);
    if (this.heartbeatInterval) clearInterval(this.heartbeatInterval);
    
    // Si l'utilisateur quitte Step 1 sans avoir complété et approuvé, nettoyer le flag
    // Cela évite que le flag persiste et permet d'accéder à Step 2 sans avoir complété Step 1
    if (!this.isApproved) {
      localStorage.removeItem(CreateStoreComponent.ALLOW_STEP2_KEY);
    }
  }

  // ─── Sélectionne la méthode de vérification ─────────────────────
  selectMethod(method: 'card' | 'email'): void {
    this.selectedVerificationMethod = method;
    this.codeError = '';
    this.codeSentMessage = '';
    this.codeSent = false;
    this.verificationCode = '';
  }

  // ─── Envoie le code de vérification par email ────────────────────
  sendVerificationCode(): void {
    if (!this.outlookEmail || !this.outlookEmail.endsWith('@esprit.tn')) {
      this.codeError = 'Please enter a valid @esprit.tn email address.';
      return;
    }
    this.isSendingCode = true;
    this.codeError = '';
    this.codeSentMessage = '';

    const url = this.apiConfig.buildUrl('/api/marketplace/seller-requests/send-verification-code');
    this.http.post<any>(url, { email: this.outlookEmail }).subscribe({
      next: () => {
        this.isSendingCode = false;
        this.codeSent = true;
        this.codeSentMessage = `Code sent to ${this.outlookEmail}. Check your inbox.`;
        this.triggerUiUpdate();
      },
      error: (err) => {
        this.isSendingCode = false;
        this.codeError = err?.error?.message || 'Failed to send code. Please try again.';
        this.triggerUiUpdate();
      }
    });
  }

  // ─── Vérifie le code entré par l'étudiant ────────────────────────
  // ✅ FIX : length !== 5 au lieu de !== 6
  verifyEmailCode(): void {
    if (!this.verificationCode || this.verificationCode.trim().length !== 5) {
      this.codeError = 'Please enter the 5-digit code.';
      return;
    }
    this.isVerifyingCode = true;
    this.codeError = '';

    const userId = this.resolveCurrentUserId();
    const url = this.apiConfig.buildUrl(`/api/marketplace/seller-requests/verify-code/${userId}`);

    this.http.post<any>(url, {
      email: this.outlookEmail,
      code: this.verificationCode.trim()
    }).subscribe({
      next: (response) => {
        this.isVerifyingCode = false;
        if (response.success) {
          this.onEmailVerifiedApproved();
        } else {
          this.codeError = response.message || 'Incorrect code.';
        }
        this.triggerUiUpdate();
      },
      error: (err) => {
        this.isVerifyingCode = false;
        this.codeError = err?.error?.message || 'Incorrect or expired code. Please try again.';
        this.triggerUiUpdate();
      }
    });
  }

  // Après vérification d'email, passe directement à l'état approuvé
  private onEmailVerifiedApproved(): void {
    const firstName = this.step1Form.firstName.trim();
    const lastName = this.step1Form.lastName.trim();
    const email = this.step1Form.email.trim();

    if (!firstName || !lastName || !email) {
      this.codeError = 'Please fill in all required fields.';
      return;
    }

    this.codeError = '';
    this.isRequestSent = true;
    this.status = 'APPROUVE';
    this.isApproved = true;
    this.currentSubmissionAt = Date.now();

    const payload = {
      numeroEtudiant: this.step1Form.studentNumber || 'EMAIL-VERIFIED',
      prenom: firstName,
      nom: lastName,
      email: email,
      carteEtudiantUrl: this.buildDefaultStudentCardUrl(),
      hasFiveOrMoreArticles: this.hasFiveOrMoreArticles
    };

    this.clearOldApprovalSignals();
    this.step2NavigationDone = false;
    this.lastSubmitPayload = payload;
    this.persistLastRequest(payload);

    this.saveRequestInAdminCache(
      { id: this.lastTemporaryId ?? Date.now(), statut: 'APPROUVE', syncedWithBackend: false },
      payload
    );

    localStorage.setItem(CreateStoreComponent.ALLOW_STEP2_KEY, 'true');
    this.step2NavigationDone = true;
    this.router.navigate(['/create-store-step-2']);
    this.triggerUiUpdate();
  }

  checkRequestStatus() {
    if (this.isApproved) return;

    if (this.isStatusEndpointUnavailable) {
      this.checkApprovalFromRequestsList();
      return;
    }

    const userId = this.resolveCurrentUserId();
    if (!userId) {
      this.recoverSessionForStatusCheck();
      return;
    }

    const primaryUrl = this.apiConfig.buildUrl(`/api/marketplace/seller-requests/status/${userId}`);
    const fallbackUrl = this.apiConfig.buildUrl(`/api/marketplace/seller-requests/status?userId=${userId}`);

    const applyStatusResponse = (data: any): void => {
      if (!this.isRequestSent && this.hasStartedFormInput && !this.hasSubmittedCurrentSession) return;
      if (data) {
        const normalizedStatus = this.extractStatus(data);
        if (!normalizedStatus) { this.checkApprovalFromRequestsList(); return; }
        if (this.isApproved && normalizedStatus === 'EN_ATTENTE') return;

        const wasApproved = this.isApproved;
        this.isRequestSent = true;
        this.status = normalizedStatus;
        this.isApproved = normalizedStatus === 'APPROUVE';
        this.triggerUiUpdate();

        if (!this.isApproved && normalizedStatus === 'EN_ATTENTE') this.checkApprovalFromRequestsList();
        if (this.isApproved && this.statusCheckInterval) {
          clearInterval(this.statusCheckInterval);
          this.statusCheckInterval = null;
        }
        if (this.isApproved) {
          if (!this.lastSubmitPayload) {
            const restored = this.getPersistedLastRequest();
            if (restored) {
              this.lastSubmitPayload = restored;
              this.hasFiveOrMoreArticles = restored.hasFiveOrMoreArticles;
            }
          }
          this.authService.refreshUser(userId).subscribe();
          if (!wasApproved && !this.step2NavigationDone) this.routeAfterApproval();
          this.triggerUiUpdate();
        }
      }
    };

    const handleStatusError = (error: any, fallbackAttempted: boolean): void => {
      if (error.status === 404 && !fallbackAttempted) {
        this.http.get(fallbackUrl).subscribe(
          (fallbackData: any) => { applyStatusResponse(fallbackData); },
          (fallbackError: any) => { handleStatusError(fallbackError, true); }
        );
        return;
      }
      if (error.status === 404) {
        this.isStatusEndpointUnavailable = true;
        if (this.hasSubmittedCurrentSession || this.lastSubmitPayload || this.isRequestSent) {
          this.isRequestSent = true;
          if (!this.status) this.status = 'EN_ATTENTE';
        }
        this.checkApprovalFromRequestsList();
      } else if (error.status === 401 || error.status === 403) {
        alert('Votre session a expiré. Veuillez vous reconnecter.');
        this.authService.logout();
        this.router.navigate(['/login']);
      } else if (error.status === 500) {
        this.isRequestSent = true;
        if (!this.status) this.status = 'EN_ATTENTE';
        this.checkApprovalFromRequestsList();
      } else {
        this.checkApprovalFromRequestsList();
      }
      this.triggerUiUpdate();
    };

    this.http.get(primaryUrl).subscribe(
      (data: any) => { applyStatusResponse(data); },
      (error: any) => { handleStatusError(error, false); }
    );
  }

  /**
   * Vérifier strictement avec le serveur que la demande est APPROUVE
   * Cela évite les faux positifs où le flag localStorage persiste
   */
  private verifyServerApprovalStatusAndUpdate(): void {
    const userId = this.resolveCurrentUserId();
    if (!userId) return;

    const url = this.apiConfig.buildUrl(`/api/marketplace/seller-requests/status/${userId}`);
    this.http.get(url).subscribe(
      (data: any) => {
        const status = this.extractStatus(data);
        
        // Si le serveur dit que c'est APPROUVE, mettre à jour l'état local
        if (status === 'APPROUVE') {
          console.log('✅ Server verified: Request is APPROUVE');
          this.isRequestSent = true;
          this.status = 'APPROUVE';
          this.isApproved = true;
          
          // Restaurer le payload si nécessaire
          if (!this.lastSubmitPayload) {
            const restored = this.getPersistedLastRequest();
            if (restored) {
              this.lastSubmitPayload = restored;
              this.hasFiveOrMoreArticles = restored.hasFiveOrMoreArticles;
            }
          }
          
          // Seulement remettre le flag si vraiment APPROUVE
          localStorage.setItem(CreateStoreComponent.ALLOW_STEP2_KEY, 'true');
        } else {
          // Si le serveur dit que ce n'est pas APPROUVE, TOUJOURS nettoyer
          console.log('❌ Server verified: Request is NOT APPROUVE, status:', status);
          this.isApproved = false;
          this.status = status || '';
          this.isRequestSent = status === 'EN_ATTENTE';
          localStorage.removeItem(CreateStoreComponent.ALLOW_STEP2_KEY);
        }
        
        this.triggerUiUpdate();
      },
      (error: any) => {
        // En cas d'erreur, nettoyer le flag pour être sûr
        console.warn('⚠️ Error verifying approval status, cleaning flag');
        localStorage.removeItem(CreateStoreComponent.ALLOW_STEP2_KEY);
        this.isApproved = false;
        this.status = '';
        this.isRequestSent = false;
      }
    );
  }

  private checkApprovalFromRequestsList(): void {
    if (this.isApproved) return;
    const userEmail = this.getCurrentUserEmail();
    if (!userEmail) return;

    const url = this.apiConfig.buildUrl(`/api/marketplace/seller-requests?t=${Date.now()}`);
    this.http.get<unknown>(url).subscribe({
      next: (data) => {
        const requests = this.extractRequestsArray(data);
        if (!requests.length) return;
        const match = requests.find((item) =>
          String(item['email'] || '').toLowerCase() === userEmail
        );
        if (!match) return;
        const normalized = this.normalizeStatus(String(match['statut'] ?? match['status'] ?? ''));
        if (normalized !== 'APPROUVE') return;

        const wasApproved = this.isApproved;
        this.isRequestSent = true;
        this.status = 'APPROUVE';
        this.isApproved = true;
        localStorage.setItem(CreateStoreComponent.ALLOW_STEP2_KEY, 'true');
        this.triggerUiUpdate();

        if (!this.lastSubmitPayload) {
          const restored = this.getPersistedLastRequest();
          if (restored) {
            this.lastSubmitPayload = restored;
            this.hasFiveOrMoreArticles = restored.hasFiveOrMoreArticles;
          }
        }
        if (this.statusCheckInterval) {
          clearInterval(this.statusCheckInterval);
          this.statusCheckInterval = null;
        }
        if (!wasApproved && !this.step2NavigationDone) this.routeAfterApproval();
        this.triggerUiUpdate();
      },
      error: () => { }
    });
  }

  private extractRequestsArray(data: unknown): Array<Record<string, unknown>> {
    if (Array.isArray(data)) {
      return data.filter((item): item is Record<string, unknown> => !!item && typeof item === 'object');
    }
    if (data && typeof data === 'object') {
      const payload = data as Record<string, unknown>;
      const candidates = [payload['data'], payload['content'], payload['requests'],
        payload['items'], payload['results'], payload['sellerRequests']];
      for (const candidate of candidates) {
        if (Array.isArray(candidate)) {
          return candidate.filter((item): item is Record<string, unknown> => !!item && typeof item === 'object');
        }
      }
    }
    return [];
  }

  private resolveCurrentUserId(): number {
    const fromSignal = Number(this.authService.currentUser()?.id || 0);
    if (fromSignal > 0) return fromSignal;
    try {
      const raw = localStorage.getItem('crispri_user');
      if (!raw) return 0;
      const parsed = JSON.parse(raw);
      const fromStorage = Number(parsed?.id || 0);
      return fromStorage > 0 ? fromStorage : 0;
    } catch { return 0; }
  }

  private recoverSessionForStatusCheck(): void {
    if (this.isRecoveringStatusSession || !this.authService.isLoggedIn()) return;
    this.isRecoveringStatusSession = true;
    this.authService.repairSession().subscribe({
      next: () => { this.isRecoveringStatusSession = false; },
      error: () => { this.isRecoveringStatusSession = false; }
    });
  }

  onSubmit() {
    this.submitAttempted = true;
    this.hasSubmittedCurrentSession = true;
    const userId = this.resolveCurrentUserId();
    if (!userId) {
      alert('Veuillez vous connecter avant d\'envoyer la demande de creation de boutique.');
      this.router.navigate(['/login'], { queryParams: { returnUrl: '/create-store' } });
      return;
    }

    const studentNumber = this.step1Form.studentNumber.trim();
    const firstName = this.step1Form.firstName.trim();
    const lastName = this.step1Form.lastName.trim();
    const email = this.step1Form.email.trim();
    const hasFiveOrMoreArticles = this.hasFiveOrMoreArticles === true;
    this.hasFiveOrMoreArticles = hasFiveOrMoreArticles;

    const studentNumberRegex = /^[0-9]{3}[A-Za-z]{3}[0-9]{4}$/;
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!studentNumber || !studentNumberRegex.test(studentNumber)) return;
    if (!firstName) return;
    if (!lastName) return;
    if (!email || !emailRegex.test(email)) return;
    if (!this.hasSelectedStudentCard) {
      this.studentCardError = 'Student card photo is required.';
      return;
    }

    this.studentCardError = '';
    this.isRequestSent = true;
    this.status = 'EN_ATTENTE';
    this.isApproved = false;
    this.currentSubmissionAt = Date.now();

    const payload = {
      numeroEtudiant: studentNumber,
      prenom: firstName,
      nom: lastName,
      email: email,
      carteEtudiantUrl: this.buildDefaultStudentCardUrl(),
      hasFiveOrMoreArticles
    };

    this.clearOldApprovalSignals();
    this.step2NavigationDone = false;
    this.lastSubmitPayload = payload;
    this.persistLastRequest(payload);
    this.lastTemporaryId = Date.now();

    this.saveRequestInAdminCache(
      { id: this.lastTemporaryId, statut: 'EN_ATTENTE', syncedWithBackend: false },
      payload
    );

    if (this.selectedStudentCardFile) {
      this.submitRequestWithMultipart(userId, payload, this.selectedStudentCardFile);
      return;
    }
    this.submitRequestAsJson(userId, payload);
  }

  private submitRequestWithMultipart(userId: number, payload: any, cardFile: File): void {
    const multipartUrl = this.apiConfig.buildUrl(`/api/marketplace/seller-requests/submit-multipart/${userId}`);

    const flatMultipartData = new FormData();
    flatMultipartData.append('numeroEtudiant', payload.numeroEtudiant);
    flatMultipartData.append('prenom', payload.prenom);
    flatMultipartData.append('nom', payload.nom);
    flatMultipartData.append('email', payload.email);
    flatMultipartData.append('carteEtudiantFile', cardFile);

    const requestPartMultipartData = new FormData();
    const requestBlob = new Blob([JSON.stringify({
      numeroEtudiant: payload.numeroEtudiant,
      prenom: payload.prenom,
      nom: payload.nom,
      email: payload.email,
      carteEtudiantUrl: payload.carteEtudiantUrl,
    })], { type: 'application/json' });
    requestPartMultipartData.append('request', requestBlob);
    requestPartMultipartData.append('carteEtudiantFile', cardFile);
    requestPartMultipartData.append('studentCardFile', cardFile);

    this.http.post<any>(multipartUrl, flatMultipartData).subscribe(
      (response) => { this.onRequestSubmitted(response, payload); },
      () => {
        this.http.post<any>(multipartUrl, requestPartMultipartData).subscribe(
          (response) => { this.onRequestSubmitted(response, payload); },
          () => { this.submitRequestAsJson(userId, payload); }
        );
      }
    );
  }

  private submitRequestAsJson(userId: number, payload: any): void {
    const url = this.apiConfig.buildUrl(`/api/marketplace/seller-requests/submit/${userId}`);
    const apiPayload = {
      numeroEtudiant: payload.numeroEtudiant,
      prenom: payload.prenom,
      nom: payload.nom,
      email: payload.email,
      carteEtudiantUrl: payload.carteEtudiantUrl
    };
    this.http.post<any>(url, apiPayload).subscribe(
      (response) => { this.onRequestSubmitted(response, payload); },
      (error: any) => { this.handleSubmitError(error); }
    );
  }

  onStudentCardSelected(event: Event): void {
    this.onFormInteraction();
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      this.hasSelectedStudentCard = false;
      this.studentCardError = 'Student card photo is required.';
      this.selectedStudentCardFile = null;
      this.revokePreviewUrl();
      return;
    }
    if (!file.type.startsWith('image/')) {
      this.studentCardError = 'Please select a valid image file.';
      input.value = '';
      this.hasSelectedStudentCard = false;
      this.selectedStudentCardFile = null;
      this.revokePreviewUrl();
      return;
    }
    this.revokePreviewUrl();
    this.studentCardPreviewUrl = URL.createObjectURL(file);
    this.selectedStudentCardFile = file;
    this.studentCardError = '';
    this.hasSelectedStudentCard = true;
  }

  private onRequestSubmitted(response: any, payload: any): void {
    const rawBackendId = response?.id;
    const backendId = typeof rawBackendId === 'number' ? rawBackendId
      : typeof rawBackendId === 'string' ? Number(rawBackendId) : NaN;
    const hasBackendId = Number.isFinite(backendId) && backendId > 0;

    this.saveRequestInAdminCache({
      ...response,
      syncedWithBackend: hasBackendId,
      statut: response?.statut ?? 'EN_ATTENTE'
    }, payload);

    this.isRequestSent = true;
    this.status = this.normalizeStatus(response?.statut ?? response?.status ?? 'EN_ATTENTE') || 'EN_ATTENTE';
    this.isApproved = this.status === 'APPROUVE';

    if (!this.isApproved) this.startStatusPolling();
    if (this.isApproved && !this.step2NavigationDone) this.routeAfterApproval();
    alert('Votre demande a été envoyée avec succès. Veuillez attendre la validation de l\'Admin Market.');
  }

  private handleSubmitError(error: any): void {
    if (error.status === 0) {
      alert('Impossible de se connecter au serveur backend.');
      return;
    }
    const backendMessage = (error?.error?.message || error?.error?.error || error?.message || '').toString();
    if (error.status === 409) {
      if (this.lastSubmitPayload && this.lastTemporaryId) {
        this.saveRequestInAdminCache(
          { id: this.lastTemporaryId, statut: 'EN_ATTENTE', syncedWithBackend: true },
          this.lastSubmitPayload
        );
      }
      this.isRequestSent = true;
      this.status = 'EN_ATTENTE';
      this.isApproved = false;
      alert('Une demande est deja en attente pour cet email.');
      this.startStatusPolling();
      return;
    }
    if (error.status === 500 && this.lastSubmitPayload) {
      this.saveRequestInAdminCache(
        { id: this.lastTemporaryId ?? Date.now(), statut: 'EN_ATTENTE', syncedWithBackend: false },
        this.lastSubmitPayload
      );
      this.isRequestSent = true;
      this.status = 'EN_ATTENTE';
      this.isApproved = false;
      this.startStatusPolling();
      return;
    }
    alert(`Une erreur est survenue (code ${error.status}).${backendMessage ? `\nDétail: ${backendMessage}` : ''}`);
  }

  private revokePreviewUrl(): void {
    if (this.studentCardPreviewUrl) {
      URL.revokeObjectURL(this.studentCardPreviewUrl);
      this.studentCardPreviewUrl = null;
    }
  }

  private buildDefaultStudentCardUrl(): string {
    return '/assets/student-card-placeholder.svg';
  }

  editRequest(): void {
    this.isRequestSent = false;
    this.hasSubmittedCurrentSession = false;
    this.submitAttempted = false;
  }

  onFormInteraction(): void {
    this.hasStartedFormInput = true;
  }

  refreshStatus(): void {
    if (this.isApproved) return;
    this.checkLocalApprovalSignal();
    this.checkRequestStatus();
    this.checkApprovalFromRequestsList();
  }

  private extractStatus(data: any): string {
    const rawStatus = String(
      data?.statut ?? data?.status ?? data?.data?.statut
      ?? data?.data?.status ?? data?.request?.statut ?? ''
    ).toUpperCase();
    return this.normalizeStatus(rawStatus);
  }

  private normalizeStatus(status: string | undefined | null): string {
    const rawStatus = String(status || '').toUpperCase();
    if (!rawStatus) return '';
    if (['APPROUVE', 'APPROUVEE', 'APPROVED', 'ACCEPTE', 'ACCEPTEE', 'ACCEPTED'].includes(rawStatus)) return 'APPROUVE';
    if (['EN_ATTENTE', 'PENDING', 'WAITING', 'IN_PROGRESS', 'EN_COURS'].includes(rawStatus)) return 'EN_ATTENTE';
    if (['REFUSE', 'REFUSEE', 'REFUSED', 'REJECTED', 'DECLINED'].includes(rawStatus)) return 'REFUSE';
    return '';
  }

  private restorePendingRequestFromCache(): void {
    try {
      const raw = localStorage.getItem(CreateStoreComponent.ADMIN_REQUESTS_CACHE_KEY);
      if (!raw) return;
      const list = JSON.parse(raw);
      if (!Array.isArray(list) || list.length === 0) return;
      const currentEmail = this.getCurrentUserEmail();
      if (!currentEmail) return;

      const userRequests = list.filter((item: any) =>
        String(item?.email || '').toLowerCase() === currentEmail
      );
      if (userRequests.length === 0) return;

      const approvedRequest = userRequests.find((item: any) =>
        this.normalizeStatus(String(item?.statut ?? item?.status ?? '')) === 'APPROUVE'
      );
      if (approvedRequest) {
        this.isRequestSent = true;
        this.status = 'APPROUVE';
        this.isApproved = true;
        this.hasFiveOrMoreArticles = approvedRequest?.hasFiveOrMoreArticles === true;
        this.lastSubmitPayload = {
          numeroEtudiant: String(approvedRequest?.numeroEtudiant || ''),
          prenom: String(approvedRequest?.prenom || ''),
          nom: String(approvedRequest?.nom || ''),
          email: String(approvedRequest?.email || ''),
          carteEtudiantUrl: String(approvedRequest?.carteEtudiantUrl || this.buildDefaultStudentCardUrl()),
          hasFiveOrMoreArticles: approvedRequest?.hasFiveOrMoreArticles === true
        };
        this.persistLastRequest(this.lastSubmitPayload);
        return;
      }

      const request = userRequests.find((item: any) =>
        this.normalizeStatus(String(item?.statut ?? item?.status ?? 'EN_ATTENTE')) !== 'APPROUVE'
      );
      if (!request) {
        this.isRequestSent = false;
        this.status = '';
        this.isApproved = false;
        this.lastSubmitPayload = null;
        localStorage.removeItem(CreateStoreComponent.LAST_REQUEST_KEY);
        return;
      }

      const status = this.normalizeStatus(String(request?.statut ?? request?.status ?? 'EN_ATTENTE')) || 'EN_ATTENTE';
      this.isRequestSent = true;
      this.status = status;
      this.isApproved = status === 'APPROUVE';
      this.hasFiveOrMoreArticles = request?.hasFiveOrMoreArticles === true;
      this.lastSubmitPayload = {
        numeroEtudiant: String(request?.numeroEtudiant || ''),
        prenom: String(request?.prenom || ''),
        nom: String(request?.nom || ''),
        email: String(request?.email || ''),
        carteEtudiantUrl: String(request?.carteEtudiantUrl || this.buildDefaultStudentCardUrl()),
        hasFiveOrMoreArticles: request?.hasFiveOrMoreArticles === true
      };
      this.persistLastRequest(this.lastSubmitPayload);
    } catch (e) {
      console.warn('Impossible de restaurer la demande locale:', e);
    }
  }

  private checkLocalApprovalSignal(): void {
    try {
      if (!this.isRequestSent && this.hasStartedFormInput && !this.hasSubmittedCurrentSession) return;
      if (!this.isRequestSent || this.isApproved) return;

      const wasApproved = this.isApproved;
      const currentEmail = this.getCurrentUserEmail();
      const lastRequest = this.getPersistedLastRequest();
      const lastRequestKey = lastRequest
        ? `${String(lastRequest.email || '').toLowerCase()}|${String(lastRequest.numeroEtudiant || '').toLowerCase()}`
        : '';

      const raw = localStorage.getItem(CreateStoreComponent.APPROVED_EVENTS_KEY);
      if (!raw) return;

      const events = JSON.parse(raw) as Record<string, number>;
      const eventKeys = Object.keys(events || {});
      const hasEmailApproval = currentEmail
        ? eventKeys.some((key) => key.startsWith(`${currentEmail}|`)) : false;
      const lastRequestApprovalTime = !!lastRequestKey ? Number(events[lastRequestKey] || 0) : 0;
      const hasLastRequestApproval = !!lastRequestKey && lastRequestApprovalTime > 0;
      const hasAnyApprovalForUser = hasEmailApproval || hasLastRequestApproval;

      if (hasLastRequestApproval && this.currentSubmissionAt > 0 && lastRequestApprovalTime < this.currentSubmissionAt) return;

      if (hasAnyApprovalForUser) {
        if (!this.lastSubmitPayload) {
          const restored = this.getPersistedLastRequest();
          if (restored) {
            this.lastSubmitPayload = restored;
            this.hasFiveOrMoreArticles = restored.hasFiveOrMoreArticles;
          }
        }
        this.isRequestSent = true;
        this.status = 'APPROUVE';
        this.isApproved = true;
        this.triggerUiUpdate();
        if (this.statusCheckInterval) {
          clearInterval(this.statusCheckInterval);
          this.statusCheckInterval = null;
        }
        if (!wasApproved && !this.step2NavigationDone) this.routeAfterApproval();
      }
    } catch { }
  }

  private persistLastRequest(payload: any): void {
    try {
      localStorage.setItem(CreateStoreComponent.LAST_REQUEST_KEY, JSON.stringify(payload));
    } catch { }
  }

  private getPersistedLastRequest(): any {
    try {
      const raw = localStorage.getItem(CreateStoreComponent.LAST_REQUEST_KEY);
      if (!raw) return null;
      const parsed = JSON.parse(raw);
      if (!parsed || typeof parsed !== 'object') return null;
      return {
        numeroEtudiant: String(parsed.numeroEtudiant || ''),
        prenom: String(parsed.prenom || ''),
        nom: String(parsed.nom || ''),
        email: String(parsed.email || '').toLowerCase(),
        carteEtudiantUrl: String(parsed.carteEtudiantUrl || this.buildDefaultStudentCardUrl()),
        hasFiveOrMoreArticles: parsed.hasFiveOrMoreArticles === true
      };
    } catch { return null; }
  }

  private getCurrentUserEmail(): string {
    const fromSignal = String(this.authService.currentUser()?.email || '').toLowerCase();
    if (fromSignal) return fromSignal;
    try {
      const raw = localStorage.getItem('crispri_user');
      if (!raw) return '';
      const parsed = JSON.parse(raw);
      return String(parsed?.email || '').toLowerCase();
    } catch { return ''; }
  }

  private clearOldApprovalSignals(): void {
    try {
      const approvedRaw = localStorage.getItem(CreateStoreComponent.APPROVED_EVENTS_KEY);
      if (approvedRaw) {
        const approved = JSON.parse(approvedRaw) as Record<string, number>;
        const currentEmail = this.getCurrentUserEmail();
        const keysToDelete = Object.keys(approved || {}).filter(key => key.startsWith(`${currentEmail}|`));
        keysToDelete.forEach(key => { delete approved[key]; });
        localStorage.setItem(CreateStoreComponent.APPROVED_EVENTS_KEY, JSON.stringify(approved));
      }
    } catch (e) { console.error('Error clearing old approval signals:', e); }
  }

  private hasApprovalSignalForRequest(email: string, numeroEtudiant: string): boolean {
    try {
      const raw = localStorage.getItem(CreateStoreComponent.APPROVED_EVENTS_KEY);
      if (!raw) return false;
      const events = JSON.parse(raw) as Record<string, number>;
      const normalizedEmail = String(email || '').toLowerCase();
      const normalizedNumber = String(numeroEtudiant || '').toLowerCase();
      const exactKey = `${normalizedEmail}|${normalizedNumber}`;
      if (Number(events?.[exactKey] || 0) > 0) return true;
      return Object.keys(events || {}).some((key) => key.startsWith(`${normalizedEmail}|`));
    } catch { return false; }
  }

  clearCachedRequest(): void {
    try {
      const raw = localStorage.getItem(CreateStoreComponent.ADMIN_REQUESTS_CACHE_KEY);
      if (raw) {
        const list = JSON.parse(raw);
        const filtered = Array.isArray(list)
          ? list.filter((item: any) => {
              if (!this.lastSubmitPayload) return true;
              const itemEmail = String(item?.email || '').toLowerCase();
              const itemNumber = String(item?.numeroEtudiant || '').toLowerCase();
              const userEmail = (this.lastSubmitPayload.email || '').toLowerCase();
              const userNumber = (this.lastSubmitPayload.numeroEtudiant || '').toLowerCase();
              return !(itemEmail === userEmail && itemNumber === userNumber);
            }) : [];
        localStorage.setItem(CreateStoreComponent.ADMIN_REQUESTS_CACHE_KEY, JSON.stringify(filtered));
      }

      const approvedRaw = localStorage.getItem(CreateStoreComponent.APPROVED_EVENTS_KEY);
      if (approvedRaw && this.lastSubmitPayload) {
        const approved = JSON.parse(approvedRaw) as Record<string, number>;
        const email = String(this.lastSubmitPayload.email || '').toLowerCase();
        const num = String(this.lastSubmitPayload.numeroEtudiant || '').toLowerCase();
        delete approved[`${email}|${num}`];
        localStorage.setItem(CreateStoreComponent.APPROVED_EVENTS_KEY, JSON.stringify(approved));
      }
      localStorage.removeItem(CreateStoreComponent.LAST_REQUEST_KEY);
    } catch (e) { console.error('Error clearing cache:', e); }

    this.isRequestSent = false;
    this.status = '';
    this.isApproved = false;
    this.hasFiveOrMoreArticles = false;
    this.lastSubmitPayload = null;
    this.lastTemporaryId = null;
    this.isStatusEndpointUnavailable = false;
    this.step2NavigationDone = false;
    this.hasSubmittedCurrentSession = false;
    this.hasStartedFormInput = false;
    this.selectedStudentCardFile = null;
    this.selectedStudentCardDataUrl = null;
    this.studentCardPreviewUrl = null;
    this.codeSent = false;
    this.verificationCode = '';
    this.codeError = '';
    this.codeSentMessage = '';
    this.revokePreviewUrl();

    const currentUser = this.authService.currentUser();
    if (currentUser) {
      const fullName = currentUser.name || '';
      const nameParts = fullName.trim().split(' ');
      this.step1Form.firstName = nameParts[0] || '';
      this.step1Form.lastName = nameParts.slice(1).join(' ') || '';
      this.step1Form.email = currentUser.email || '';
      this.outlookEmail = currentUser.email || '';
    }
    this.step1Form.studentNumber = '';
  }

  private saveRequestInAdminCache(response: any, payload: any): void {
    this.saveStudentCardToIndexedDb(payload);
    try {
      const raw = localStorage.getItem(CreateStoreComponent.ADMIN_REQUESTS_CACHE_KEY);
      let list: any[] = [];
      if (raw) {
        try { const existing = JSON.parse(raw); list = Array.isArray(existing) ? existing : []; }
        catch (e) { list = []; }
      }
      const rawId = response?.id;
      const resolvedId = typeof rawId === 'number' ? rawId : typeof rawId === 'string' ? Number(rawId) : NaN;
      const hasBackendId = Number.isFinite(resolvedId) && resolvedId > 0;
      const normalizedRequest = {
        id: hasBackendId ? resolvedId : (response?.id ?? Date.now()),
        numeroEtudiant: response?.numeroEtudiant ?? payload.numeroEtudiant,
        prenom: response?.prenom ?? payload.prenom,
        nom: response?.nom ?? payload.nom,
        email: response?.email ?? payload.email,
        carteEtudiantUrl: this.selectedStudentCardDataUrl || response?.carteEtudiantUrl || payload.carteEtudiantUrl,
        hasFiveOrMoreArticles: payload.hasFiveOrMoreArticles === true,
        statut: response?.statut ?? 'EN_ATTENTE',
        syncedWithBackend: hasBackendId
      };
      const normalizedEmail = (normalizedRequest.email || '').toLowerCase();
      const normalizedStudentNumber = (normalizedRequest.numeroEtudiant || '').toLowerCase();
      const withoutDuplicate = list.filter((item: any) => {
        const sameId = item?.id === normalizedRequest.id;
        const sameBusinessKey =
          (String(item?.email || '').toLowerCase() === normalizedEmail) &&
          (String(item?.numeroEtudiant || '').toLowerCase() === normalizedStudentNumber);
        return !sameId && !sameBusinessKey;
      });
      localStorage.setItem(CreateStoreComponent.ADMIN_REQUESTS_CACHE_KEY,
        JSON.stringify([normalizedRequest, ...withoutDuplicate]));
    } catch { }
  }

  private saveStudentCardToIndexedDb(payload: any): void {
    if (!this.selectedStudentCardFile) return;
    const key = `${(payload.email || '').toLowerCase()}|${(payload.numeroEtudiant || '').toLowerCase()}`;
    if (!key || key === '|') return;
    this.indexedDb.saveCard(key, this.selectedStudentCardFile)
      .then(() => { console.log('✓ Student card saved to IndexedDB'); })
      .catch((err) => { console.error('Error saving student card:', err); });
  }

  proceedToNextStep() {
    if (!this.isApproved) { alert('Votre demande est toujours en attente.'); return; }
    if (!this.shouldOpenStoreStep2) { alert('Vous avez choisi de ne pas ouvrir de boutique maintenant.'); return; }
    // Important: do not promote role here.
    // SELLER role is granted only when user clicks "Open my store" in Step 2.
    localStorage.setItem(CreateStoreComponent.ALLOW_STEP2_KEY, 'true');
    this.router.navigate(['/create-store-step-2']);
  }

  goToVendorDashboard(): void {
    if (!this.isApproved) { alert('Votre demande doit etre approuvee par l\'admin.'); return; }
    this.router.navigate(['/dashboard/vendor'], { queryParams: { openCreate: '1' } });
  }

  private startStatusPolling(): void {
    if (this.statusCheckInterval) clearInterval(this.statusCheckInterval);
    this.refreshStatus();
    this.statusCheckInterval = setInterval(() => { this.refreshStatus(); }, 1000);
  }

  private startHeartbeat(): void {
    if (this.heartbeatInterval) clearInterval(this.heartbeatInterval);
    this.heartbeatInterval = setInterval(() => {
      if (this.isRequestSent && !this.isApproved) this.refreshStatus();
    }, 1000);
  }

  private routeAfterApproval(): void {
    if (this.step2NavigationDone) return;
    this.step2NavigationDone = true;
    this.isRequestSent = true;
    this.status = 'APPROUVE';
    this.isApproved = true;
    if (this.productCategories.length === 0) this.loadProductCreationContext();
  }

  private loadProductCreationContext(): void {
    this.isLoadingProductCategories = true;
    this.productCategoriesError = '';
    this.productService.loadProductCategories().subscribe({
      next: (categories) => {
        this.productCategories = categories;
        if (categories.length === 0) this.productCategoriesError = 'No categories found.';
      },
      error: (error) => {
        this.productCategories = [];
        this.productCategoriesError = this.productService.resolveApiErrorMessage(error, 'Unable to load categories.');
      },
      complete: () => { this.isLoadingProductCategories = false; }
    });
  }

  private resolveStoreForProductCreation(onResolved?: (storeId: number) => void): void {
    this.isResolvingProductStore = true;
    this.productService.resolveMyStoreId().subscribe({
      next: (storeId) => {
        this.storeIdForProductCreation = Number(storeId || 0);
        if (this.storeIdForProductCreation > 0 && onResolved) {
          this.isResolvingProductStore = false;
          onResolved(this.storeIdForProductCreation);
        } else { this.createDefaultStore(onResolved); }
      },
      error: () => { this.createDefaultStore(onResolved); }
    });
  }

  private createDefaultStore(onResolved?: (storeId: number) => void): void {
    const storeName = this.step1Form.firstName ? `Boutique de ${this.step1Form.firstName}` : 'Ma Boutique';
    this.marketplaceApi.createMyStore({ name: storeName, description: 'Créée automatiquement', address: '', phone: '' }).subscribe({
      next: (store) => {
        this.storeIdForProductCreation = Number(store.id);
        this.isResolvingProductStore = false;
        if (onResolved) onResolved(this.storeIdForProductCreation);
      },
      error: () => {
        this.isResolvingProductStore = false;
        this.storeIdForProductCreation = 0;
        this.productSubmitError = 'Unable to create default store context.';
        if (onResolved) onResolved(0);
      }
    });
  }

  private submitProductWithStore(formData: ProductFormData, storeId: number): void {
    const category = this.productCategories.find((item) => item.id === Number(formData.categoryId || 0));
    this.productService.createMarketplaceProduct(formData, { storeId, categoryName: category?.name }).subscribe({
      next: () => {
        this.productSubmitMessage = 'Produit ajoute avec succes.';
        this.isProductAddedSuccess = true;
        this.productFormComponent?.reset();
        this.triggerUiUpdate();
      },
      error: (error) => {
        this.productSubmitError = this.productService.resolveApiErrorMessage(error, 'Erreur lors de l\'ajout du produit.');
      },
      complete: () => { this.isSubmittingProduct = false; }
    });
  }

  submitProductWithoutStore(): void {
    if (!this.isApproved || this.shouldOpenStoreStep2) {
      this.productSubmitError = 'Ce formulaire est reserve au cas sans ouverture de boutique.';
      this.productSubmitMessage = '';
      return;
    }
    const formData = this.productFormComponent?.getFormData();
    if (!formData) {
      this.productSubmitError = 'Veuillez remplir les champs obligatoires du produit.';
      this.productSubmitMessage = '';
      return;
    }
    const selectedCategoryId = Number(formData.categoryId || 0);
    if (selectedCategoryId <= 0) {
      this.productSubmitError = 'Veuillez choisir une categorie valide.';
      this.productSubmitMessage = '';
      return;
    }
    this.isSubmittingProduct = true;
    this.productSubmitError = '';
    this.productSubmitMessage = '';
    this.isProductAddedSuccess = false;

    if (this.storeIdForProductCreation > 0) {
      this.submitProductWithStore(formData, this.storeIdForProductCreation);
      return;
    }
    this.resolveStoreForProductCreation((storeId) => {
      if (storeId <= 0) {
        this.productSubmitError = 'Aucune boutique trouvee pour votre compte.';
        this.productSubmitMessage = '';
        this.isSubmittingProduct = false;
        return;
      }
      this.submitProductWithStore(formData, storeId);
    });
  }

  goToMarketplace(): void {
    this.router.navigate(['/marketplace']);
  }
}