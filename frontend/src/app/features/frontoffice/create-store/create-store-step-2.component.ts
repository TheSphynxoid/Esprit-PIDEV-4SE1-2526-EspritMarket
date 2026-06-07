import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { StoreCreatePayload, StoreService } from '../../../services/store.service';
import { ApiConfigService } from '../../../services/api/api-config.service';
import { MarketplaceApiService } from '../../../services';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';

import {
    CREATED_STORE_PROFILE_KEY,
    CreatedStoreProfile,
    STORE_CATEGORY_OPTIONS,
    StoreCategoryId,
    StoreTheme,
    deletePublicStoreProfile,
    getCategoryLabel,
    getStoreTheme,
    savePublicStoreProfile
} from './store-personalization';

@Component({
    selector: 'app-create-store-step-2',
    standalone: true,
    imports: [CommonModule, FormsModule, HeaderComponent, FooterComponent],
    templateUrl: './create-store-step-2.component.html',
    styleUrls: ['./create-store-step-2.component.css']
})
export class CreateStoreStep2Component implements OnInit {
    private static readonly MAX_STORES_PER_SELLER = 3;
    private static readonly ALLOW_STEP2_KEY = 'create_store_allow_step2';
        // Helpers pour l'affichage des erreurs individuels
        get nameError(): string | null {
            const name = this.storeName.trim();
            if (!name) {
                return '✏️ Ajouter le nom (3-60 caractères)';
            }
            if (name.length < 3) {
                return `❌ Minimum 3 caractères. (${name.length}/3)`;
            }
            if (name.length > 60) {
                return `❌ Maximum 60 caractères. (${name.length}/60)`;
            }
            if (!/^[A-Za-z0-9 ]+$/.test(name)) {
                return '❌ Seulement lettres, chiffres et espaces';
            }
            return null;
        }

        get phoneError(): string | null {
            const phone = this.phone.trim();
            if (!phone) {
                return '✏️ Entrer 8 chiffres (ex: 22123456)';
            }
            if (!/^[0-9]+$/.test(phone)) {
                return '❌ Seulement des chiffres, pas d\'espaces';
            }
            if (phone.length < 8) {
                return `❌ ${phone.length}/8 chiffres. Ajouter ${8 - phone.length} chiffre(s)`;
            }
            if (phone.length > 8) {
                return `❌ ${phone.length}/8 chiffres. Enlever ${phone.length - 8} chiffre(s)`;
            }
            return null;
        }

        get addressError(): string | null {
            const address = this.address.trim();
            if (!address) {
                return '✏️ Ajouter l\'adresse (minimum 3 caractères)';
            }
            if (address.length < 3) {
                return `❌ Minimum 3 caractères. (${address.length}/3)`;
            }
            return null;
        }

        get descriptionError(): string | null {
            const desc = this.description.trim();
            if (!desc) {
                return '✏️ Ajouter une description (minimum 10 caractères)';
            }
            if (desc.length < 10) {
                return `❌ Minimum 10 caractères. (${desc.length}/10)`;
            }
            return null;
        }

        get logoFieldError(): string | null {
            if (!this.logoDataUrl) {
                return 'Le logo est obligatoire (image, max 3 Mo).';
            }
            return null;
        }
    storeName = '';
    category: StoreCategoryId | '' = '';
    phone = '';
    address = '';
    description = '';
    logoDataUrl: string | null = null;
    logoError = '';
    submitMessage = '';
    submitError = '';
    createdStoreId: number | null = null;
    isLoading = false;
    submitAttempted = false;
    addressTouched = false;
    private isRecoveringSession = false;
    isEditMode = false;
    private previousStoreName = '';

    readonly categories = STORE_CATEGORY_OPTIONS;

    get selectedTheme(): StoreTheme {
        return getStoreTheme(this.category);
    }

    get canSubmit(): boolean {
        // Nom obligatoire, 3-60 caractères alphanumériques ou espaces
        const nameValid = /^[A-Za-z0-9 ]{3,60}$/.test(this.storeName.trim());
        // Téléphone obligatoire, exactement 8 chiffres
        const phoneTrimmed = this.phone.trim();
        const phoneValid = /^[0-9]{8}$/.test(phoneTrimmed);
        // Description obligatoire, au moins 10 caractères
        const descriptionValid = this.description.trim().length >= 10;
        // Adresse obligatoire, au moins 3 caractères
        const addressValid = this.address.trim().length >= 3;
        // Logo obligatoire
        const logoValid = !!this.logoDataUrl;

        return nameValid
            && phoneValid
            && descriptionValid
            && addressValid
            && logoValid
            && !this.isLoading;
    }

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private authService: AuthService,
        private storeService: StoreService,
        private marketplaceApi: MarketplaceApiService,
        private http: HttpClient,
        private apiConfig: ApiConfigService
    ) { }

    ngOnInit(): void {
        const user = this.authService.currentUser();
        const allowStep2 = localStorage.getItem(CreateStoreStep2Component.ALLOW_STEP2_KEY) === 'true';
        
        // Gate check local - bloquer IMMÉDIATEMENT si pas d'accès
        if (!user || (!allowStep2 && user.role !== 'seller')) {
            console.log('🚫 Blocking Step 2 access: no flag and not seller');
            localStorage.removeItem(CreateStoreStep2Component.ALLOW_STEP2_KEY);
            this.router.navigate(['/create-store']);
            return;
        }

        // Validation stricte du serveur - vérifier que l'utilisateur a vraiment APPROUVE status
        this.validateServerApprovalBeforeStep2(user.id);

        this.route.queryParamMap.subscribe((params) => {
            this.isEditMode = params.get('editStore') === '1';
            if (this.isEditMode) {
                this.prefillFromExistingProfile();
            }
            this.applyTheme(this.selectedTheme);
        });
    }

    /**
     * Valider strictement avec le serveur que l'utilisateur a APPROUVE status
     * Si pas APPROUVE, rediriger à Step 1 pour éviter bypass
     */
    private validateServerApprovalBeforeStep2(userId: number): void {
        const apiUrl = this.apiConfig.buildUrl(`/api/marketplace/seller-requests/status/${userId}`);
        
        this.http.get(apiUrl).subscribe(
            (response: any) => {
                const status = response?.statut || response?.status || '';
                
                // Si status est APPROUVE, allow access
                if (status === 'APPROUVE') {
                    console.log('✅ Server validated: APPROUVE - allow Step 2');
                    return; // Continue with Step 2
                }
                
                // Si role est seller, allow access (user already promoted)
                const user = this.authService.currentUser();
                if (user?.role === 'seller') {
                    console.log('✅ User is already seller - allow Step 2');
                    return; // Continue with Step 2
                }
                
                // Sinon, nettoyer le flag et rediriger à Step 1
                console.log('🚫 Server denied: status is', status, '- blocking Step 2');
                localStorage.removeItem(CreateStoreStep2Component.ALLOW_STEP2_KEY);
                this.router.navigate(['/create-store']);
            },
            (error: any) => {
                // En cas d'erreur API, nettoyer le flag et rediriger (sécurité stricte)
                console.warn('⚠️ Error validating approval, blocking Step 2');
                localStorage.removeItem(CreateStoreStep2Component.ALLOW_STEP2_KEY);
                this.router.navigate(['/create-store']);
            }
        );
    }

    onCategoryChange(): void {
        this.applyTheme(this.selectedTheme);
    }

    onLogoSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        const file = input.files?.[0] ?? null;

        this.logoError = '';
        if (!file) {
            this.logoDataUrl = null;
            return;
        }

        if (!file.type.startsWith('image/')) {
            this.logoDataUrl = null;
            this.logoError = 'Veuillez choisir un fichier image valide.';
            return;
        }

        const maxFileSize = 3 * 1024 * 1024;
        if (file.size > maxFileSize) {
            this.logoDataUrl = null;
            this.logoError = 'L image est trop grande. Taille maximale: 3 MB.';
            return;
        }

        const reader = new FileReader();
        reader.onload = () => {
            this.logoDataUrl = typeof reader.result === 'string' ? reader.result : null;
        };
        reader.onerror = () => {
            this.logoDataUrl = null;
            this.logoError = 'Impossible de lire le fichier image.';
        };
        reader.readAsDataURL(file);
    }

    onSubmit(form?: NgForm): void {
        if (this.isLoading) {
            return;
        }

        this.submitAttempted = true;
        this.submitMessage = '';
        this.submitError = '';
        console.log('🔵 create-store-step-2: Submit started');
        console.log('📋 create-store-step-2: Validation state', {
            canSubmit: this.canSubmit,
            formValid: form?.valid ?? null,
            name: this.storeName.trim(),
            phone: this.phone.trim(),
            address: this.address.trim(),
            description: this.description.trim(),
            logo: this.logoDataUrl
        });

        if (!this.canSubmit) {
            // Affichage d'un message d'erreur détaillé pour chaque champ manquant ou invalide
            let errorMsg = 'Veuillez corriger le formulaire avant de continuer.';
            if (!/^[A-Za-z0-9 ]{3,60}$/.test(this.storeName.trim())) {
                errorMsg += '\n- Le nom de la boutique est obligatoire (3-60 caractères).';
            }
            if (!/^[0-9]{8}$/.test(this.phone.trim())) {
                errorMsg += '\n- Le numéro de téléphone doit contenir exactement 8 chiffres.';
            }
            if (this.address.trim().length < 3) {
                errorMsg += '\n- L\'adresse est obligatoire (au moins 3 caractères).';
            }
            if (this.description.trim().length < 10) {
                errorMsg += '\n- La description est obligatoire (au moins 10 caractères).';
            }
            if (!this.logoDataUrl) {
                errorMsg += '\n- Le logo de la boutique est obligatoire.';
            }
            this.submitError = errorMsg;
            return;
        }

        const selectedCategory: StoreCategoryId = this.category || 'CLOTHING_BRAND';
        const logoDataUrl: string = this.logoDataUrl || '';

        const user = this.resolveCurrentUser();
        if (!user || user.id === 0) {
            this.recoverSessionAndRetrySubmit();
            return;
        }

        this.isLoading = true;
        const payload = this.buildStorePayload(this.storeName.trim());
        this.activateSellerIfNeeded(user, () => {
            // Preflight check: allow creation while seller has fewer than 3 stores.
            this.marketplaceApi.getMyStores().subscribe({
                next: (stores) => {
                    const ownedStoresCount = (stores || []).filter((store) => Number(store?.id || 0) > 0).length;
                    if (ownedStoresCount >= CreateStoreStep2Component.MAX_STORES_PER_SELLER) {
                        this.isLoading = false;
                        this.submitError = 'Limite atteinte: maximum 3 boutiques par seller.';
                        return;
                    }

                    this.createStoreRequest(user.id, payload, selectedCategory, logoDataUrl, user);
                },
                error: (checkErr) => {
                    if (checkErr?.status === 401 || checkErr?.status === 403) {
                        this.isLoading = false;
                        this.submitError = 'Session expiree. Veuillez vous reconnecter.';
                        this.authService.logout();
                        this.router.navigate(['/login']);
                        return;
                    }

                    // If store-list check is unavailable, proceed with create and let backend decide.
                    this.createStoreRequest(user.id, payload, selectedCategory, logoDataUrl, user);
                }
            });
        });
    }

    private createStoreRequest(
        userId: number,
        payload: StoreCreatePayload,
        selectedCategory: StoreCategoryId,
        logoDataUrl: string,
        user: { id: number; role?: string; name?: string; email?: string }
    ): void {
        console.log('📍 create-store-step-2: Creating store via API');
        console.log('📦 create-store-step-2: Payload', payload);

        this.storeService.createStore(userId, payload).subscribe({
            next: (response) => {
                console.log('✅ create-store-step-2: Network response', response);
                const store = response.body;
                const status = Number(response.status || 0);

                if (status !== 201 && status !== 200) {
                    this.submitError = `Reponse inattendue du serveur (HTTP ${status}).`;
                    this.isLoading = false;
                    return;
                }

                this.storeName = String(store?.name || this.storeName).trim();
                this.createdStoreId = Number(store?.id || 0) > 0 ? Number(store?.id) : null;
                this.persistStoreThemeSelection();
                this.persistCreatedProfile(selectedCategory, logoDataUrl, user, this.createdStoreId);
                if (this.previousStoreName && this.previousStoreName !== this.storeName) {
                    deletePublicStoreProfile(this.previousStoreName);
                }
                this.previousStoreName = this.storeName;
                this.submitMessage = this.createdStoreId
                    ? `Boutique creee avec succes. ID: ${this.createdStoreId}`
                    : 'Boutique creee avec succes.';
                this.isLoading = false;
                localStorage.removeItem(CreateStoreStep2Component.ALLOW_STEP2_KEY);
                setTimeout(() => {
                    this.router.navigate(['/store-showcase']);
                }, 1200);
            },
            error: (err) => {
                console.error('❌ create-store-step-2: API error while creating store', err);
                console.error('Erreur lors de la création de la boutique:', err);
                if (err?.status === 401 || err?.status === 403) {
                    this.isLoading = false;
                    this.submitError = 'Session expiree. Veuillez vous reconnecter.';
                    this.authService.logout();
                    this.router.navigate(['/login']);
                    return;
                }

                if (err?.status === 409) {
                    this.handleExistingStoreConflict(err);
                    return;
                }

                if (err?.status === 400) {
                    this.isLoading = false;
                    this.submitError = String(err?.error?.message || err?.error?.error || 'Donnees invalides. Verifiez les champs du formulaire.');
                    return;
                }

                const detail = String(err?.error?.message || err?.error?.error || '').trim();
                this.isLoading = false;
                this.submitError = `Une erreur est survenue lors de la creation de votre boutique.${detail ? ` Detail: ${detail}` : ''}`;
            },
            complete: () => {
                this.isLoading = false;
            }
        });
    }

    private handleExistingStoreConflict(error?: any): void {
        const backendMessage = String(
            error?.error?.message || error?.error?.error || ''
        ).trim();

        this.marketplaceApi.getMyStores().subscribe({
            next: (stores) => {
                const ownedStoresCount = (stores || []).filter((store) => Number(store?.id || 0) > 0).length;
                this.isLoading = false;

                if (ownedStoresCount >= CreateStoreStep2Component.MAX_STORES_PER_SELLER) {
                    this.submitError = 'Limite atteinte: maximum 3 boutiques par seller.';
                    return;
                }

                this.submitError = backendMessage || 'Conflit lors de la creation de la boutique.';
            },
            error: () => {
                this.isLoading = false;
                this.submitError = backendMessage || 'Conflit lors de la creation de la boutique.';
            }
        });
    }

    private buildStorePayload(storeName: string): StoreCreatePayload {
        const categoryList = this.category ? [this.category] : [];

        return {
            name: storeName,
            description: this.description.trim() || '',
            address: this.address.trim() || '',
            phone: this.phone.trim() || '',
            rating: 0,
            // Ne pas envoyer le logo base64 - géré séparément via profilelocal
            categories: categoryList
        };
    }

    private persistStoreThemeSelection(): void {
        localStorage.setItem('create_store_selected_theme', this.selectedTheme.id);
        localStorage.setItem(
            `store_theme_preview_${this.storeName.trim().toLowerCase()}`,
            JSON.stringify(this.selectedTheme)
        );
    }

    private persistCreatedProfile(
        selectedCategory: StoreCategoryId,
        logoDataUrl: string,
        user: { id: number; role?: string; name?: string; email?: string },
        storeId?: number | null
    ): void {
        const createdProfile: CreatedStoreProfile = {
            id: Number(storeId || 0) > 0 ? String(storeId) : undefined,
            name: this.storeName.trim(),
            categoryId: selectedCategory,
            categoryLabel: getCategoryLabel(selectedCategory),
            phone: this.phone.trim(),
            description: this.description.trim(),
            logoDataUrl,
            themeId: this.selectedTheme.id,
            ownerName: String((user as { name?: string })?.name || '').trim(),
            ownerEmail: String((user as { email?: string })?.email || '').trim(),
            createdAt: new Date().toISOString()
        };

        localStorage.setItem(CREATED_STORE_PROFILE_KEY, JSON.stringify(createdProfile));
        savePublicStoreProfile(createdProfile);
    }

    private activateSellerIfNeeded(
        user: { id: number; role?: string; name?: string; email?: string },
        onSuccess: () => void
    ): void {
        if (user.role === 'seller') {
            onSuccess();
            return;
        }

        const url = this.apiConfig.buildUrl(`/api/marketplace/seller-requests/activate-seller/${user.id}`);
        this.http.post<{ success: boolean; role?: string }>(url, {}).subscribe({
            next: () => {
                this.authService.updateCurrentUserRole('seller');
                onSuccess();
            },
            error: () => {
                this.isLoading = false;
                this.submitError = 'Impossible de mettre a jour votre role. Veuillez reessayer.';
            }
        });
    }

    private resolveCurrentUser(): { id: number; role?: string; name?: string; email?: string } | null {
        const fromSignal = this.authService.currentUser();
        if (fromSignal && Number(fromSignal.id) > 0) {
            return {
                id: Number(fromSignal.id),
                role: fromSignal.role,
                name: String(fromSignal.name || ''),
                email: String(fromSignal.email || '')
            };
        }

        try {
            const raw = localStorage.getItem('crispri_user');
            if (!raw) {
                return null;
            }

            const parsed = JSON.parse(raw);
            const id = Number(parsed?.id || 0);
            if (id <= 0) {
                return null;
            }

            return {
                id,
                role: String(parsed?.role || ''),
                name: String(parsed?.name || ''),
                email: String(parsed?.email || '')
            };
        } catch {
            return null;
        }
    }

    private recoverSessionAndRetrySubmit(): void {
        if (this.isRecoveringSession) {
            return;
        }

        this.isRecoveringSession = true;
        this.authService.repairSession().subscribe({
            next: (repaired) => {
                this.isRecoveringSession = false;
                if (repaired) {
                    this.onSubmit();
                    return;
                }

                alert('Session expirée. Veuillez vous reconnecter.');
                this.authService.logout();
                this.router.navigate(['/login']);
            },
            error: () => {
                this.isRecoveringSession = false;
                alert('Session expirée. Veuillez vous reconnecter.');
                this.authService.logout();
                this.router.navigate(['/login']);
            }
        });
    }

    private applyTheme(theme: StoreTheme): void {
        const root = document.documentElement;
        root.style.setProperty('--shop-primary', theme.primary);
        root.style.setProperty('--shop-secondary', theme.primaryStrong);
        root.style.setProperty('--shop-accent', theme.accent);
        root.style.setProperty('--shop-background', theme.backgroundSoft);
        root.style.setProperty('--shop-surface', '#ffffff');
        root.style.setProperty('--shop-text', theme.text);
        root.style.setProperty('--shop-text-muted', theme.mutedText);
        root.style.setProperty('--shop-font-heading', theme.headingFont);
        root.style.setProperty('--shop-font-body', theme.bodyFont);
        this.ensureGoogleFontLoaded(theme.headingFont);
        this.ensureGoogleFontLoaded(theme.bodyFont);
    }

    private prefillFromExistingProfile(): void {
        try {
            const raw = localStorage.getItem(CREATED_STORE_PROFILE_KEY);
            if (!raw) {
                return;
            }

            const existing = JSON.parse(raw) as CreatedStoreProfile;
            this.storeName = String(existing?.name || '').trim();
            this.previousStoreName = this.storeName;
            this.category = (existing?.categoryId || '') as StoreCategoryId | '';
            this.phone = String(existing?.phone || '').trim();
            this.address = String((existing as any)?.address || '').trim();
            this.description = String(existing?.description || '').trim();
            this.logoDataUrl = existing?.logoDataUrl || null;
        } catch {
            // Ignore malformed local profile.
        }
    }

    private static readonly SYSTEM_FONTS = new Set([
        'arial', 'helvetica', 'times new roman', 'courier new', 'georgia',
        'verdana', 'trebuchet ms', 'palatino', 'garamond', 'bookman',
        'comic sans ms', 'impact', 'monotype corsiva', 'lucida console',
        'lucida sans unicode', 'ms serif', 'ms sans serif', 'symbol',
        'wingdings', 'cambria', 'calibri', 'consolas', 'candara',
    ]);

    private ensureGoogleFontLoaded(fontFamily: string): void {
        const cleanFamily = (fontFamily || '').split(',')[0].replace(/[\"'/]/g, '').trim();
        if (!cleanFamily) {
            return;
        }

        if (CreateStoreStep2Component.SYSTEM_FONTS.has(cleanFamily.toLowerCase())) {
            return;
        }

        const linkId = `shop-theme-font-${cleanFamily.toLowerCase().replace(/\s+/g, '-')}`;
        if (document.getElementById(linkId)) {
            return;
        }

        const link = document.createElement('link');
        link.id = linkId;
        link.rel = 'stylesheet';
        link.href = `https://fonts.googleapis.com/css2?family=${encodeURIComponent(cleanFamily).replace(/%20/g, '+')}:wght@400;500;600;700&display=swap`;
        document.head.appendChild(link);
    }
}
