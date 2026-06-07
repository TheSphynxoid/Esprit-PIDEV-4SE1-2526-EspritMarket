import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService, MarketplaceApiService, Product, ProductService } from '../../../services';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map, switchMap, timeout } from 'rxjs/operators';
import {
  CREATED_STORE_PROFILE_KEY,
  CreatedStoreProfile,
  StoreTheme,
  getStoreTheme,
  deletePublicStoreProfile
} from './store-personalization';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';


interface AddProductForm {
  name: string;
  description: string;
  price: number;
  stock: number;
  dimensions: string;
  weight: number;
}

interface EditProductForm {
  name: string;
  description: string;
  price: number;
  stock: number;
  discountPercent: number;
  promoEndAt: string;
  dimensionsLabel: string;
  weight: number;
}

const THEME_HERO_IMAGES: Record<string, string> = {
  'modern-elegant': 'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=1600&q=80',
  'minimal-clean': 'https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?auto=format&fit=crop&w=1600&q=80',
  'creative-colorful': 'https://images.unsplash.com/photo-1513364776144-60967b0f800f?auto=format&fit=crop&w=1600&q=80',
  'chic-fashion': 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=1600&q=80',
  'sweet-bakery': 'https://images.unsplash.com/photo-1519864600265-abb23847ef2c?auto=format&fit=crop&w=1600&q=80',
  'dynamic-energy': 'https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?auto=format&fit=crop&w=1600&q=80'
};

@Component({
  selector: 'app-store-showcase',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, HeaderComponent, FooterComponent],
  templateUrl: './store-showcase.component.html',
  styleUrls: ['./store-showcase.component.css']
})
export class StoreShowcaseComponent implements OnInit {
  private static readonly STORE_LOCAL_PRODUCTS_PREFIX = 'store_showcase_products_';

  profile: CreatedStoreProfile | null = null;
  theme: StoreTheme = getStoreTheme('');
  storeProducts: Product[] = [];
  isAddProductOpen = false;
  isEditProductOpen = false;
  isSubmitting = false;
  isSubmittingEdit = false;
  imageError = '';
  editError = '';
  categoryError = '';
  selectedCategoryId: number | null = null;
  categoryOptions: { id: number; name: string }[] = [];
  selectedProductImageDataUrl = '';
  selectedFile: File | null = null;
  editingProduct: Product | null = null;
  private submitGuardTimerId: ReturnType<typeof setTimeout> | null = null;
  private countdownIntervalId: any;


  form: AddProductForm = {
    name: '',
    description: '',
    price: 0,
    stock: 0,
    dimensions: '',
    weight: 0
  };

  editForm: EditProductForm = {
    name: '',
    description: '',
    price: 0,
    stock: 0,
    discountPercent: 0,
    promoEndAt: '',
    dimensionsLabel: '',
    weight: 0
  };

// Removed duplicate property declarations

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private productService: ProductService,
    private marketplaceApi: MarketplaceApiService
  ) {}

  ngOnInit(): void {
    this.restoreProfile();
    this.route.queryParamMap.subscribe((params) => {
      if (params.get('openAddProduct') === '1') {
        this.openAddProduct();
      }
      const editProductId = params.get('editProduct');
      if (editProductId) {
        // Wait a bit for products to load before trying to open edit modal
        setTimeout(() => {
          const product = this.storeProducts.find(p => p.id === Number(editProductId));
          if (product) {
            this.openEditProduct(product);
          }
        }, 600);
      }
    });

    this.productService.refresh();
    this.loadStoreProducts();
    setTimeout(() => this.loadStoreProducts(), 350);
    this.ensureGoogleFontLoaded(this.theme.headingFont);
    this.ensureGoogleFontLoaded(this.theme.bodyFont);
    this.productService.loadProductCategories().subscribe((categories) => {
      this.categoryOptions = categories;
    });
    this.refreshStoreProfile();
    this.startCountdown();
  }

  ngOnDestroy(): void {
    if (this.countdownIntervalId) {
      clearInterval(this.countdownIntervalId);
    }
  }

  private startCountdown(): void {
    if (this.countdownIntervalId) {
      clearInterval(this.countdownIntervalId);
    }

    this.countdownIntervalId = setInterval(() => {
      this.storeProducts.forEach((product) => {
        if (product.isPromotionActive && product.remainingSeconds && product.remainingSeconds > 0) {
          product.remainingSeconds--;
        }
      });
    }, 1000);
  }

  formatTimeLeft(seconds: number | null | undefined): string {
    if (!seconds || seconds <= 0) {
      return '00:00:00';
    }

    const hrs = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;

    return `${String(hrs).padStart(2, '0')}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }

  formatPromoEndDate(dateStr: string | null | undefined): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'numeric' });
  }


  private refreshStoreProfile(): void {
    if (this.authService.isLoggedIn()) {
      this.marketplaceApi.getMyStores().subscribe({
        next: (stores) => {
          const selectedStore = this.resolveProfileStoreFromList(stores || []);
          if (!selectedStore) {
            return;
          }

          // Update profile with the selected store to keep second/third store context stable.
          this.profile = {
            ...this.profile,
            id: String(selectedStore.id),
            name: String(selectedStore.name || this.profile?.name || '').trim(),
            categoryLabel: this.toCategoryLabel(selectedStore.categories),
            phone: String(selectedStore.phone || this.profile?.phone || '').trim(),
            description: String(selectedStore.description || this.profile?.description || '').trim(),
            balance: selectedStore.balance,
            ownerId: selectedStore.ownerId ? String(selectedStore.ownerId) : undefined
          } as any;
        },
        error: (err) => {
          console.warn('Could not refresh store profile from API', err);
        }
      });
    }
  }

  get isStoreOwner(): boolean {
    const user = this.authService.currentUser();
    if (!user || !this.profile) return false;
    const userId = Number(user.id || 0);
    const ownerId = Number(this.profile.ownerId || this.profile.id || 0);
    if (userId && ownerId && userId === ownerId) return true;
    const userEmail = (user.email || '').toLowerCase();
    const ownerEmail = (this.profile.ownerEmail || '').toLowerCase();
    return !!(userEmail && ownerEmail && userEmail === ownerEmail);
  }

  deleteStore(): void {
    if (!this.profile || !this.profile.id) return;
    const confirmed = confirm(`Supprimer la boutique "${this.profile.name}" ? Cette action est irréversible.`);
    if (!confirmed) return;
    const storeId = Number(this.profile.id);
    if (!storeId || storeId <= 0) return;

    this.isSubmitting = true;
    this.marketplaceApi.stores.delete(storeId).subscribe({
      next: () => {
        this.isSubmitting = false;
        try { deletePublicStoreProfile(this.profile!.name); } catch {}
        this.router.navigate(['/marketplace']);
      },
      error: (err) => {
        this.isSubmitting = false;
        alert('Impossible de supprimer la boutique: ' + (err?.error?.message || err?.message || 'Erreur'));
      }
    });
  }


  get themeVars(): Record<string, string> {
    return {
      '--store-bg': this.theme.background,
      '--store-glow': this.theme.decorativeGlow,
      '--store-hero-overlay': this.theme.heroOverlay,
      '--store-card-bg': this.theme.cardBackground,
      '--store-text': this.theme.text,
      '--store-muted': this.theme.mutedText,
  // Removed misplaced property declaration
      '--store-primary-strong': this.theme.primaryStrong,
      '--store-accent': this.theme.accent,
      '--store-heading-font': this.theme.headingFont,
      '--store-body-font': this.theme.bodyFont
    };
  }

  get heroImage(): string {
    return THEME_HERO_IMAGES[this.theme.id] || THEME_HERO_IMAGES['minimal-clean'];
  }

  get canManageProducts(): boolean {
    const role = this.authService.currentUser()?.role;
    return role === 'seller' || !!this.profile;
  }

  goToVendorDashboard(): void {
    this.router.navigate(['/dashboard/vendor'], {
      queryParams: { tab: 'products' }
    });
  }

  openEditStore(): void {
    this.router.navigate(['/create-store-step-2'], {
      queryParams: { editStore: '1' }
    });
  }

  openAddProduct(): void {
    this.isAddProductOpen = true;
  }

  openEditProduct(product: Product): void {
    if (!this.canManageProducts || !product?.id) {
      return;
    }

    this.editingProduct = product;
    this.editForm = {
      name: product.name,
      description: product.description,
      price: Number(product.price || 0),
      stock: Number(product.stock || 0),
      discountPercent: Number(product.discountPercent || 0),
      promoEndAt: product.promoEndAt ? this.toDateTimeLocal(product.promoEndAt) : '',
      dimensionsLabel: product.dimensionsLabel || '',
      weight: Number(product.weight || 0)
    };
    this.editError = '';
    this.isEditProductOpen = true;
  }

  closeEditProduct(): void {
    this.clearSubmitGuard();
    this.isEditProductOpen = false;
    this.isSubmittingEdit = false;
    this.editError = '';
    this.editingProduct = null;
  }

  confirmEditProduct(): void {
    if (!this.editingProduct) {
      return;
    }

    if (!this.editForm.name.trim() || this.editForm.price <= 0 || this.editForm.stock < 0) {
      this.editError = 'Veuillez verifier le nom, le prix et le stock.';
      return;
    }

    const promoActive = this.editForm.discountPercent > 0;
    if (promoActive && !this.editForm.promoEndAt) {
      this.editError = 'Veuillez definir une date limite pour la promotion.';
      return;
    }

    if (promoActive) {
      const endDate = new Date(this.editForm.promoEndAt);
      if (Number.isNaN(endDate.getTime()) || endDate.getTime() <= Date.now()) {
        this.editError = 'La date de fin de promotion doit etre dans le futur.';
        return;
      }
    }

    this.isSubmittingEdit = true;
    this.startSubmitGuard();
    this.editError = '';

    const imageUrl = this.resolveImageUrlForWrite(this.editingProduct);
    const updatePayload = {
      name: this.editForm.name.trim(),
      description: this.editForm.description.trim(),
      price: Number(this.editForm.price),
      stock: Number(this.editForm.stock),
      dimensionsLabel: this.editForm.dimensionsLabel.trim(),
      weight: Number(this.editForm.weight),
      imageUrl: imageUrl || '',
      store: this.editingProduct.storeId ? { id: this.editingProduct.storeId } : undefined,
      category: this.editingProduct.categoryId ? { id: this.editingProduct.categoryId } : undefined
    };

    this.updateProductWithFallbacks(this.editingProduct.id, updatePayload).pipe(
      timeout(12000)
    ).subscribe({
      next: () => {
        const updatedProductId = this.editingProduct?.id;
        const discountPercent = Number(this.editForm.discountPercent);
        const promoEndAt = this.editForm.promoEndAt;
        const shouldApplyPromotion = discountPercent > 0;

        this.isSubmittingEdit = false;
        this.clearSubmitGuard();
        this.finalizeEditSave();
        
        // Appliquer la promotion EN ARRIÈRE-PLAN (non-bloquant) utilisant l'ID capturé
        if (updatedProductId) {
          this.applyPromotionAsync(updatedProductId, shouldApplyPromotion, discountPercent, promoEndAt);
        }
      },

      error: (error) => {
        this.isSubmittingEdit = false;
        this.clearSubmitGuard();
        this.editError = this.resolveEditError(error, 'Impossible de modifier le produit.');
      }
    });
  }

  deleteCurrentProduct(): void {
    if (!this.editingProduct) {
      return;
    }

    this.isSubmittingEdit = true;
    this.startSubmitGuard();
    this.marketplaceApi.products.delete(this.editingProduct.id).subscribe({
      next: () => {
        this.isSubmittingEdit = false;
        this.clearSubmitGuard();
        this.productService.deleteProduct(this.editingProduct!.id);
        this.loadStoreProducts();
        this.closeEditProduct();
      },
      error: (error) => {
        this.isSubmittingEdit = false;
        this.clearSubmitGuard();
        this.editError = this.resolveEditError(error, 'Impossible de supprimer le produit.');
      }
    });
  }

  closeAddProduct(): void {
    this.isAddProductOpen = false;
    this.form = {
      name: '',
      description: '',
      price: 0,
      stock: 0,
      dimensions: '',
      weight: 0
    };
    this.selectedCategoryId = null;
    this.categoryError = '';
    this.imageError = '';
    this.selectedProductImageDataUrl = '';
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    this.selectedFile = file;
    this.imageError = '';
    if (!file) {
      this.selectedProductImageDataUrl = '';
      return;
    }
    if (!file.type.startsWith('image/')) {
      this.selectedProductImageDataUrl = '';
      this.imageError = 'Veuillez choisir un fichier image valide.';
      return;
    }
    const maxFileSize = 4 * 1024 * 1024;
    if (file.size > maxFileSize) {
      this.selectedProductImageDataUrl = '';
      this.imageError = 'Image trop grande. Taille maximale: 4 MB.';
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      this.selectedProductImageDataUrl = typeof reader.result === 'string' ? reader.result : '';
    };
    reader.onerror = () => {
      this.selectedProductImageDataUrl = '';
      this.imageError = 'Impossible de lire le fichier image.';
    };
    reader.readAsDataURL(file);
  }

  isImageSource(value: string | undefined | null): boolean {
    if (!value) {
      return false;
    }

    return value.startsWith('http://') || 
           value.startsWith('https://') || 
           value.startsWith('data:image/') ||
           value.startsWith('/uploads/');
  }

  getImageUrl(value: string | undefined | null): string {
    if (!value) return '';
    if (value.startsWith('/uploads/')) {
      // Use the injected ApiConfigService or fallback to localhost:8088
      return `http://localhost:8088${value}`;
    }
    return value;
  }

  confirmAddProduct(): void {
    this.categoryError = '';
    if (
      !this.form.name.trim()
      || !this.form.description.trim()
      || !this.form.dimensions.trim()
      || this.form.price <= 0
      || this.form.stock < 0
      || this.form.weight <= 0
      || !this.selectedFile
    ) {
      return;
    }
    if (!this.selectedCategoryId || this.selectedCategoryId <= 0) {
      this.categoryError = 'Veuillez sélectionner une catégorie.';
      return;
    }

    this.isSubmitting = true;

    this.resolveTargetStoreId().subscribe({
      next: (storeId) => {
        if (!storeId || storeId <= 0) {
          this.isSubmitting = false;
          alert('Impossible de récupérer votre boutique. Veuillez créer votre boutique avant d\'ajouter un produit.');
          return;
        }
        const formData = new FormData();
        formData.append('name', this.form.name.trim());
        formData.append('description', this.form.description.trim());
        
        // Ensure price is a valid number string for Spring (e.g. "10.5" not empty)
        const parsedPrice = Number(this.form.price) || 0;
        formData.append('price', parsedPrice.toString());
        
        const parsedStock = Number(this.form.stock);
        console.log('🚀 CREATE PRODUCT: Sending stock to backend =', parsedStock);
        formData.append('stock', isNaN(parsedStock) ? '0' : parsedStock.toString());
        
        const dim = this.form.dimensions.trim();
        if (dim) {
          formData.append('dimensionsLabel', dim);
        }
        
        const parsedWeight = Number(this.form.weight);
        if (!isNaN(parsedWeight)) {
          formData.append('weight', parsedWeight.toString());
        }
        
        formData.append('storeId', storeId.toString());
        
        if (this.selectedCategoryId && this.selectedCategoryId > 0) {
          formData.append('categoryId', this.selectedCategoryId.toString());
        }
        
        formData.append('image', this.selectedFile!);

        this.marketplaceApi.postProductWithFormData(formData)
          .subscribe({
            next: (res) => {
              this.isSubmitting = false;
              this.persistLocalProduct(res as any); // Add to local cache immediately
              this.productService.refresh(); // Fetch updated list from backend
              
              // Slight delay to allow backend to index and signal to update
              setTimeout(() => {
                this.loadStoreProducts();
                this.closeAddProduct();
              }, 100);
            },
            error: (err) => {
              this.isSubmitting = false;
              const detail = String(err?.error?.message || err?.error?.error || '').trim();
              alert(detail
                ? `Erreur lors de la création du produit: ${detail}`
                : 'Erreur lors de la création du produit.');
            }
          });
      },
      error: (err) => {
        this.isSubmitting = false;
        const detail = String(err?.error?.message || err?.error?.error || '').trim();
        alert(detail
          ? `Erreur lors de la récupération de votre boutique: ${detail}`
          : 'Erreur lors de la récupération de votre boutique.');
      }
    });
  }

  private resolveTargetStoreId(): Observable<number> {
    const profileStoreId = Number(this.profile?.id || 0);
    if (profileStoreId > 0) {
      return of(profileStoreId);
    }

    return this.marketplaceApi.getMyStores().pipe(
      map((stores) => {
        const selectedStore = this.resolveProfileStoreFromList(stores || []);
        return Number(selectedStore?.id || 0);
      }),
      catchError(() => this.productService.resolveMyStoreId())
    );
  }

  private resolveProfileStoreFromList(stores: any[]): any | null {
    if (!Array.isArray(stores) || stores.length === 0) {
      return null;
    }

    const profileStoreId = Number(this.profile?.id || 0);
    if (profileStoreId > 0) {
      const byId = stores.find((store) => Number(store?.id || 0) === profileStoreId);
      if (byId) {
        return byId;
      }
    }

    const targetName = this.normalizeStoreName(this.profile?.name);
    if (targetName) {
      const byName = stores.find((store) => this.normalizeStoreName(store?.name) === targetName);
      if (byName) {
        return byName;
      }
    }

    return stores[0] || null;
  }

  private normalizeStoreName(value: unknown): string {
    return String(value || '').trim().toLowerCase();
  }

  private toCategoryLabel(categories: unknown): string {
    if (!Array.isArray(categories) || categories.length === 0) {
      return 'Boutique';
    }

    const labels = categories
      .map((item) => {
        if (typeof item === 'string') {
          return item;
        }
        return String((item as any)?.name || '');
      })
      .map((value) => value.trim())
      .filter((value) => value.length > 0);

    return labels.length > 0 ? labels.join(', ') : 'Boutique';
  }

  private loadStoreProducts(): void {
    const storeId = Number(this.profile?.id || 0);
    if (storeId <= 0) {
      this.storeProducts = [];
      return;
    }

    // Always fetch store-scoped products from backend to keep each store independent.
    this.productService.loadProductsByStoreId(storeId).subscribe({
      next: (backendProducts) => {
        const locallyPersisted = this.readLocalProducts().filter((product) => Number(product.storeId || 0) === storeId);

        // Merge local + backend (backend wins on duplicates)
        const mergedByKey = new Map<string, Product>();

        locallyPersisted.forEach((product) => {
          const key = `${Number(product.id || 0)}|${Number(product.storeId || 0)}`;
          mergedByKey.set(key, product);
        });

        backendProducts.forEach((product) => {
          const key = `${Number(product.id || 0)}|${Number(product.storeId || 0)}`;
          mergedByKey.set(key, product);
        });

        this.storeProducts = Array.from(mergedByKey.values());
      },
      error: () => {
        // Fallback local cache only for this store
        this.storeProducts = this.readLocalProducts().filter((product) => Number(product.storeId || 0) === storeId);
      }
    });
  }

  private persistLocalProduct(product: Product): void {
    const current = this.readLocalProducts();
    const updated = [product, ...current];
    try {
      localStorage.setItem(this.getLocalProductsKey(), JSON.stringify(updated));
    } catch {
      // Best effort persistence.
    }
  }

  private readLocalProducts(): Product[] {
    try {
      const raw = localStorage.getItem(this.getLocalProductsKey());
      if (!raw) {
        return [];
      }

      const parsed = JSON.parse(raw);
      if (!Array.isArray(parsed)) {
        return [];
      }

      return parsed as Product[];
    } catch {
      return [];
    }
  }

  private getLocalProductsKey(): string {
    const name = (this.profile?.name || 'default').toLowerCase().trim().replace(/\s+/g, '-');
    return `${StoreShowcaseComponent.STORE_LOCAL_PRODUCTS_PREFIX}${name}`;
  }

  private restoreProfile(): void {
    try {
      const rawProfile = localStorage.getItem(CREATED_STORE_PROFILE_KEY);
      if (!rawProfile) {
        this.router.navigate(['/create-store-step-2']);
        return;
      }

      const parsed = JSON.parse(rawProfile) as CreatedStoreProfile;
      if (!parsed?.name || !parsed?.categoryId || !parsed?.logoDataUrl) {
        this.router.navigate(['/create-store-step-2']);
        return;
      }

      this.profile = parsed;
      this.theme = getStoreTheme(parsed.categoryId);
    } catch {
      this.router.navigate(['/create-store-step-2']);
    }
  }

  private ensureGoogleFontLoaded(fontFamily: string): void {
    const cleanFamily = (fontFamily || '').split(',')[0].replace(/[\"']/g, '').trim();
    if (!cleanFamily) {
      return;
    }

    const linkId = `store-font-${cleanFamily.toLowerCase().replace(/\s+/g, '-')}`;
    if (document.getElementById(linkId)) {
      return;
    }

    const link = document.createElement('link');
    link.id = linkId;
    link.rel = 'stylesheet';
    link.href = `https://fonts.googleapis.com/css2?family=${encodeURIComponent(cleanFamily).replace(/%20/g, '+')}:wght@400;500;600;700&display=swap`;
    document.head.appendChild(link);
  }

  private applyPromotionAsync(
    productId: number,
    shouldApplyPromotion: boolean,
    discountPercent: number,
    promoEndAt: string
  ): void {

    if (!shouldApplyPromotion) {
      this.marketplaceApi.removeProductPromotion(productId).pipe(
        timeout(10000)
      ).subscribe({
        next: () => this.productService.refresh(),
        error: () => this.productService.refresh()
      });
      return;
    }

    this.marketplaceApi.applyProductPromotion(productId, {
      discountPercent,
      promoEndAt: new Date(promoEndAt).toISOString()
    }).pipe(
      timeout(10000)
    ).subscribe({
      next: () => this.productService.refresh(),
      error: () => this.productService.refresh()
    });
  }

  private finalizeEditSave(): void {
    this.clearSubmitGuard();
    this.productService.refresh();
    this.loadStoreProducts();
    this.closeEditProduct();
  }

  private startSubmitGuard(): void {
    this.clearSubmitGuard();
    this.submitGuardTimerId = setTimeout(() => {
      if (this.isSubmittingEdit) {
        this.isSubmittingEdit = false;
        this.editError = 'Operation trop longue. Verifiez le backend puis reessayez.';
      }
    }, 15000);
  }

  private clearSubmitGuard(): void {
    if (this.submitGuardTimerId) {
      clearTimeout(this.submitGuardTimerId);
      this.submitGuardTimerId = null;
    }
  }

  private resolveImageUrlForWrite(product: Product): string {
    if (this.isImageSource(product.image)) {
      return product.image;
    }
    if (Array.isArray(product.images) && product.images.length > 0 && this.isImageSource(product.images[0])) {
      return product.images[0];
    }
    return '';
  }

  private withOwnerFallback<T>(primary: Observable<T>, fallback: () => Observable<T>): Observable<T> {
    return primary.pipe(
      catchError((error) => {
        const canFallback =
          error?.status === 403 ||
          error?.status === 404 ||
          error?.status === 405 ||
          error?.status === 501;
        if (canFallback) {
          return fallback();
        }
        return throwError(() => error);
      })
    );
  }

  private updateProductWithFallbacks(productId: number, payload: Record<string, unknown>): Observable<unknown> {
    return this.marketplaceApi.updateOwnerProduct(productId, payload as any).pipe(
      catchError((ownerErr) => {
        const canTryMarketplaceUpdate = ownerErr?.status === 403 || ownerErr?.status === 404 || ownerErr?.status === 405 || ownerErr?.status === 501;
        if (!canTryMarketplaceUpdate) {
          return throwError(() => ownerErr);
        }

        return this.marketplaceApi.products.update(productId, payload as any).pipe(
          catchError((marketErr) => {
            const canTryDirectPut = marketErr?.status === 400 || marketErr?.status === 403 || marketErr?.status === 404 || marketErr?.status === 405 || marketErr?.status === 501;
            if (!canTryDirectPut) {
              return throwError(() => marketErr);
            }

            return this.marketplaceApi.updateProductDirect(productId, payload).pipe(
              catchError((directPutErr) => {
                const canTryPatch = directPutErr?.status === 400 || directPutErr?.status === 403 || directPutErr?.status === 404 || directPutErr?.status === 405 || directPutErr?.status === 501;
                if (!canTryPatch) {
                  return throwError(() => directPutErr);
                }

                return this.marketplaceApi.patchProductDirect(productId, payload);
              })
            );
          })
        );
      })
    );
  }

  private resolveEditError(error: any, fallback: string): string {
    if (error?.status === 403) {
      return 'Vous n\'etes pas autorise a modifier ce produit.';
    }
    if (error?.status === 404) {
      return 'Produit introuvable.';
    }
    if (error?.status === 400) {
      return error?.error?.message || 'Donnees invalides (prix/stock/remise/date).';
    }
    return error?.error?.message || fallback;
  }

  private toDateTimeLocal(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return '';
    }

    const pad = (n: number) => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }
}
