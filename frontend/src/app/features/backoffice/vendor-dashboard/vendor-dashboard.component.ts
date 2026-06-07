import { Component, inject, computed, ChangeDetectorRef } from '@angular/core';
import { CommonModule, TitleCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ModalComponent } from '../../../shared/components';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService, MarketplaceApiService, Product, ProductService } from '../../../services';
import { ProductWritePayload, StoreStatsResource } from '../../../services/api';
import {
  ProductVariant,
  buildProductDescriptionWithMeta,
  parseProductDescription
} from '../../../services/product-meta.util';
import { Observable, forkJoin, of, throwError } from 'rxjs';
import { catchError, finalize, switchMap, timeout } from 'rxjs/operators';

@Component({
  selector: 'app-vendor-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalComponent, TitleCasePipe],
  templateUrl: './vendor-dashboard.component.html',
  styleUrls: ['./vendor-dashboard.component.css']
})
export class VendorDashboardComponent {
  private static readonly CREATED_STORE_PROFILE_KEY = 'create_store_latest_shop_profile';
  private static readonly LAST_REQUEST_KEY = 'create_store_last_request';

  private authService = inject(AuthService);
  private marketplaceApi = inject(MarketplaceApiService);
  private productService = inject(ProductService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  activeTab = 'overview';

  isProductModalOpen = false;
  isEditingProduct = false;
  isSubmittingProduct = false;
  editingProductId: number | null = null;

  createProductError = '';
  createProductForm = {
    name: '',
    description: '',
    price: 0,
    stock: 0,
    dimensions: '',
    weight: 0,
    imageDataUrl: ''
  };

  editProductError = '';
  editProductForm = {
    name: '',
    description: '',
    price: 0,
    stock: 0,
    discountPercent: 0,
    promoEndAt: '',
    dimensions: '',
    weight: 0
  };

  isDeleteConfirmOpen = false;
  isSubmittingDelete = false;
  deletingProductId: number | null = null;
  deletingProductName = '';

  isPromotionModalOpen = false;
  isSubmittingPromotion = false;
  promotionError = '';
  promotionProductId: number | null = null;
  promotionProductName = '';
  promotionDiscountPercent = 10;
  promotionEndAt = '';

  myStore: { id: number; name: string; balance?: number; rating?: number } | null = null;
  storeStats: StoreStatsResource | null = null;
  isStoreStatsLoading = false;
  storeStatsError = '';
  myStores: Array<{
    id: number;
    name: string;
    balance?: number;
    rating?: number;
    categories: Array<{ id: number; name: string }>;
  }> = [];
  activeStoreId: number | null = null;
  storeCategories: Array<{ id: number; name: string }> | null = null;
  storeLoading = false;
  creatingStore = false;
  storeError = '';
  isNoStoreProductFlow = false;
  readonly maxStoresPerSeller = 3;

  dashboardTitle = computed(() => {
    const role = this.authService.currentUser()?.role;
    switch (role) {
      case 'seller':
      default:
        return 'Vendor Dashboard';
    }
  });

  products: Array<{
    id: number;
    name: string;
    price: number;
    originalPrice?: number;
    discountedPrice?: number;
    discountPercent?: number;
    promoEndAt?: string;
    remainingSeconds?: number;
    isPromotionActive?: boolean;
    ownerId?: number;
    storeId?: number;
    categoryId?: number;
    stock: number;
    sales: number;
    imageUrl: string;
    images: string[];
    variants: ProductVariant[];
    category: string;
    sku: string;
    width: number;
    height: number;
    depth: number;
    dimensionUnit: 'cm' | 'mm' | 'm';
    weight: number;
    dimensionsLabel: string;
    description: string;
  }> = [];

  themeStyle: Record<string, string> = {};
  private pendingOpenEditProductId: number | null = null;

  private readonly themePaletteMap: Record<string, {
    primary: string; surface: string; text: string; accent: string
  }> = {
    elegant: { primary: '#8B5CF6', surface: '#F5F3FF', text: '#2E1065', accent: '#A78BFA' },
    tech:    { primary: '#06B6D4', surface: '#ECFEFF', text: '#083344', accent: '#0EA5E9' },
    nature:  { primary: '#059669', surface: '#ECFDF5', text: '#064E3B', accent: '#10B981' },
    sport:   { primary: '#EF4444', surface: '#FFF7ED', text: '#7F1D1D', accent: '#F97316' },
    luxury:  { primary: '#D4AF37', surface: '#FFFBEB', text: '#3A2E0D', accent: '#B8860B' },
    vintage: { primary: '#B45309', surface: '#FFF8ED', text: '#78350F', accent: '#F59E0B' },
    minimal: { primary: '#18181B', surface: '#F4F4F5', text: '#09090B', accent: '#52525B' },
    playful: { primary: '#EC4899', surface: '#FDF2F8', text: '#831843', accent: '#F472B6' }
  };

  orders: Array<{
    id: string;
    customer: string;
    amount: number;
    status: string;
    items: string;
    products: Array<{
      name: string;
      quantity: number;
      price: number;
      width: number;
      height: number;
      depth: number;
      dimensionUnit: 'cm' | 'mm' | 'm';
      weight: number;
      dimensionsLabel: string;
    }>;
  }> = [];

  constructor() {
    this.isNoStoreProductFlow = this.resolveNoStoreProductFlow();
    const currentRole = this.authService.currentUser()?.role;
    if (currentRole === 'visitor' && !this.isNoStoreProductFlow) {
      this.router.navigate(['/marketplace']);
      return;
    }

    this.loadMyStores();
    this.loadOrders();
    this.applyStoreTheme();

    const openCreateFromQuery = this.route.snapshot.queryParamMap.get('openCreate') === '1';
    const openEditRaw = this.route.snapshot.queryParamMap.get('openEdit');
    const openEditFromQuery = openEditRaw ? Number(openEditRaw) : NaN;

    if (openCreateFromQuery) {
      this.activeTab = 'products';
      setTimeout(() => { this.openCreateProductModal(); }, 0);
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { openCreate: null },
        queryParamsHandling: 'merge',
        replaceUrl: true
      });
      return;
    }

    if (Number.isFinite(openEditFromQuery) && openEditFromQuery > 0) {
      this.activeTab = this.route.snapshot.queryParamMap.get('tab') || 'products';
      this.pendingOpenEditProductId = openEditFromQuery;
    }
  }

  // ─── Helpers ──────────────────────────────────────────────────────

  /**
   * ✅ Normalise la catégorie : "OTHER" → "Other", "electronics" → "Electronics"
   * Retourne "Other" si vide ou null
   */
  private normalizeCategoryName(raw: string | null | undefined): string {
    if (!raw || !raw.trim()) return 'Other';
    const trimmed = raw.trim();
    return trimmed.charAt(0).toUpperCase() + trimmed.slice(1).toLowerCase();
  }

  private getPrimaryStoreCategoryName(): string {
    const firstCategory = this.storeCategories?.find((category) => {
      const name = String(category?.name || '').trim();
      return name.length > 0;
    });

    const categoryName = String(firstCategory?.name || '').trim();
    return categoryName ? this.normalizeCategoryName(categoryName) : '';
  }

  private resolveProductImageUrl(raw: string | null | undefined): string {
    const value = String(raw || '').trim();
    if (!value) {
      return '';
    }

    if (
      value.startsWith('http://') ||
      value.startsWith('https://') ||
      value.startsWith('data:image/') ||
      value.startsWith('blob:')
    ) {
      return value;
    }

    if (value.startsWith('/uploads/')) {
      return `http://localhost:8088${value}`;
    }

    if (value.startsWith('uploads/')) {
      return `http://localhost:8088/${value}`;
    }

    return value;
  }

  /**
   * ✅ Construit dimensionsLabel uniquement si w/h/d > 0
   * Évite "0 x 0 x 0 cm"
   */
  private buildDimensionsLabel(
    w: number, h: number, d: number,
    unit: string,
    fallback = 'Not specified'
  ): string {
    if (w > 0 || h > 0 || d > 0) {
      return `${w} x ${h} x ${d} ${unit || 'cm'}`;
    }
    return fallback;
  }

  /**
   * ✅ Détecte si un dimensionsLabel vaut "0 x 0 x 0 ..."
   */
  private isZeroDimensionsLabel(label: string | null | undefined): boolean {
    if (!label) return true;
    return /^0\s*[xX×]\s*0\s*[xX×]\s*0/.test(label.trim());
  }

  private isMissingDimensionsLabel(label: string | null | undefined): boolean {
    const normalized = String(label || '').trim();
    if (!normalized) {
      return true;
    }
    if (this.isZeroDimensionsLabel(normalized)) {
      return true;
    }
    return /^(not specified|n\/a)$/i.test(normalized);
  }

  // ─── Navigation ───────────────────────────────────────────────────

  goToMarketplace(): void {
    this.router.navigate(['/marketplace']);
  }

  goToMyShop(): void {
    if (!this.myStore?.name) {
      this.router.navigate(['/dashboard/vendor']);
      return;
    }

    const storeName = String(this.myStore.name || '').trim();
    this.storeError = '';
    this.router.navigate(['/store', storeName]);
  }

  openShop(storeId: number): void {
    const store = this.myStores.find((item) => item.id === storeId);
    if (!store) {
      return;
    }

    this.setActiveStore(store.id);
    this.router.navigate(['/store', store.name]);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  // ─── Create Product ───────────────────────────────────────────────

  openCreateProductModal(): void {
    if (this.isNoStoreProductFlow) {
      this.router.navigate(['/create-store'], { queryParams: { openProductOnly: '1' } });
      return;
    }

    if (!this.myStore) {
      this.storeError = 'Vous devez creer votre boutique avant d\'ajouter un produit.';
      return;
    }
    this.isEditingProduct = false;
    this.createProductError = '';
    this.createProductForm = {
      name: '',
      description: '',
      price: 0,
      stock: 0,
      dimensions: '',
      weight: 0,
      imageDataUrl: ''
    };
    this.isProductModalOpen = true;
  }

  onCreateProductImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    if (!file) {
      this.createProductForm.imageDataUrl = '';
      return;
    }
    if (!file.type.startsWith('image/')) {
      this.createProductError = 'Veuillez choisir un fichier image valide.';
      this.createProductForm.imageDataUrl = '';
      return;
    }
    const maxFileSize = 4 * 1024 * 1024;
    if (file.size > maxFileSize) {
      this.createProductError = 'Image trop grande. Taille maximale: 4 MB.';
      this.createProductForm.imageDataUrl = '';
      return;
    }
    this.createProductError = '';
    const reader = new FileReader();
    reader.onload = () => {
      this.createProductForm.imageDataUrl =
        typeof reader.result === 'string' ? reader.result : '';
      this.cdr.markForCheck();
    };
    reader.onerror = () => {
      this.createProductError = 'Impossible de lire le fichier image.';
      this.createProductForm.imageDataUrl = '';
      this.cdr.markForCheck();
    };
    reader.readAsDataURL(file);
  }

  isCreateProductFormValid(): boolean {
    const name        = (this.createProductForm.name || '').trim();
    const description = (this.createProductForm.description || '').trim();
    const price       = Number(this.createProductForm.price || 0);
    const stock       = Number(this.createProductForm.stock ?? 0);
    const imageUrl    = this.createProductForm.imageDataUrl || '';
    return (
      name.length > 0 &&
      description.length > 0 &&
      !isNaN(price) && price > 0 &&
      !isNaN(stock) && stock >= 0 &&
      imageUrl.length > 0
    );
  }

  onFormInput(): void {
    this.cdr.markForCheck();
  }

  submitCreateProductFromModal(): void {
    if (!this.isCreateProductFormValid()) {
      this.createProductError = 'Veuillez remplir correctement tous les champs requis.';
      return;
    }
    if (!this.myStore) {
      this.createProductError = 'Vous devez creer votre boutique avant d\'ajouter un produit.';
      return;
    }

    const name        = (this.createProductForm.name || '').trim();
    const description = (this.createProductForm.description || '').trim();
    const price       = Number(this.createProductForm.price);
    const stock       = Number(this.createProductForm.stock);
    const imageUrl    = this.createProductForm.imageDataUrl || '';

    const parsedDimensions = this.parseDimensions(this.createProductForm.dimensions);
    const weight = Number(this.createProductForm.weight || 0);

    const hasMeaningfulDimensions =
      parsedDimensions.width > 0 ||
      parsedDimensions.height > 0 ||
      parsedDimensions.depth > 0;

    // ✅ dimensionsLabel uniquement si dimensions réelles
    const rawDimensions = String(this.createProductForm.dimensions || '').trim();
    const dimensionsLabel = hasMeaningfulDimensions
      ? `${parsedDimensions.width} x ${parsedDimensions.height} x ${parsedDimensions.depth} ${parsedDimensions.unit}`
      : (rawDimensions || undefined);

    // ✅ Catégorie héritée du store (choisie à la step 2)
    const firstCategory = this.storeCategories && this.storeCategories.length > 0
      ? this.storeCategories[0]
      : null;

    const payload: ProductWritePayload = {
      name,
      description: description || '',
      price,
      stock,
      imageUrl,
      dimensionsLabel,
      weight: weight > 0 ? weight : undefined,
      categoryId: firstCategory?.id ?? undefined,
      store: this.myStore?.id ? { id: this.myStore.id } : undefined
    };

    // Nettoyer les champs undefined
    Object.keys(payload).forEach(
      key => (payload as any)[key] === undefined && delete (payload as any)[key]
    );

    this.isSubmittingProduct = true;
    this.createProductError = '';

    this.withOwnerFallback(
      this.marketplaceApi.createOwnerProduct(payload),
      () => this.marketplaceApi.products.create(payload)
    ).pipe(
      timeout(12000),
      finalize(() => { this.isSubmittingProduct = false; })
    ).subscribe({
      next: () => {
        this.closeProductModal();
        this.loadProducts();
      },
      error: (error) => {
        this.createProductError = this.resolveHttpErrorMessage(
          error, 'Impossible d\'ajouter le produit.'
        );
      }
    });
  }

  // ─── Edit Product ─────────────────────────────────────────────────

  openEditProductModal(productId: number): void {
    const product = this.products.find(p => p.id === productId);
    if (!product) return;

    this.isEditingProduct = true;
    this.editingProductId = productId;
    this.editProductError = '';
    this.editProductForm = {
      name: product.name,
      description: product.description,
      price: Number(product.price || 0),
      stock: Number(product.stock || 0),
      discountPercent: Number(product.discountPercent || 0),
      promoEndAt: product.promoEndAt ? this.toDateTimeLocal(product.promoEndAt) : '',
      // ✅ Champ vide si "Not specified"
      dimensions: product.dimensionsLabel !== 'Not specified'
        ? (product.dimensionsLabel || '') : '',
      weight: product.weight || 0
    };
    this.isProductModalOpen = true;
  }

  closeProductModal(): void {
    this.isProductModalOpen = false;
    this.isEditingProduct = false;
    this.isSubmittingProduct = false;
    this.editingProductId = null;
    this.editProductError = '';
    this.createProductError = '';
  }

  submitDashboardEditProduct(): void {
    if (!this.isEditingProduct || !this.editingProductId) return;

    const product = this.products.find(p => p.id === this.editingProductId);
    if (!product) {
      this.editProductError = 'Produit introuvable.';
      return;
    }

    if (
      !this.editProductForm.name.trim() ||
      this.editProductForm.price <= 0 ||
      this.editProductForm.stock < 0
    ) {
      this.editProductError = 'Veuillez verifier le nom, le prix et le stock.';
      return;
    }

    if (this.editProductForm.discountPercent > 0 && !this.editProductForm.promoEndAt) {
      this.editProductError = 'Veuillez choisir une date de fin de promotion.';
      return;
    }

    if (this.editProductForm.discountPercent > 0) {
      const endAt = new Date(this.editProductForm.promoEndAt).getTime();
      if (!Number.isFinite(endAt) || endAt <= Date.now()) {
        this.editProductError = 'La date de fin doit etre dans le futur.';
        return;
      }
    }

    this.isSubmittingProduct = true;
    this.editProductError = '';

    // ✅ dimensionsLabel édition : ne pas envoyer "0 x 0 x 0 cm"
    const parsedEditDimensions = this.parseDimensions(this.editProductForm.dimensions);
    const hasEditDimensions =
      parsedEditDimensions.width > 0 ||
      parsedEditDimensions.height > 0 ||
      parsedEditDimensions.depth > 0;
    const editDimensionsLabel = hasEditDimensions
      ? `${parsedEditDimensions.width} x ${parsedEditDimensions.height} x ${parsedEditDimensions.depth} ${parsedEditDimensions.unit}`
      : (this.editProductForm.dimensions.trim() || undefined);

    const payload: ProductWritePayload = {
      name: this.editProductForm.name.trim(),
      description: buildProductDescriptionWithMeta({
        description: this.editProductForm.description.trim(),
        category: product.category,
        sku: product.sku,
        width: product.width,
        height: product.height,
        depth: product.depth,
        dimensionUnit: product.dimensionUnit,
        weight: product.weight,
        images: product.images,
        variants: product.variants
      }),
      price: Number(this.editProductForm.price),
      stock: Number(this.editProductForm.stock),
      dimensionsLabel: editDimensionsLabel,
      weight: Number(this.editProductForm.weight) || undefined,
      imageUrl: product.imageUrl || '',
      store: product.storeId ? { id: product.storeId } : undefined,
      category: product.categoryId ? { id: product.categoryId } : undefined
    };

    const minimalPayload: ProductWritePayload = {
      name: this.editProductForm.name.trim(),
      description: this.editProductForm.description.trim(),
      price: Number(this.editProductForm.price),
      stock: Number(this.editProductForm.stock),
      dimensionsLabel: editDimensionsLabel,
      weight: Number(this.editProductForm.weight) || undefined,
      imageUrl: product.imageUrl || ''
    };

    const idsPayload = {
      ...minimalPayload,
      storeId: product.storeId,
      categoryId: product.categoryId
    };

    this.updateProductWithFallbacks(
      this.editingProductId, payload, minimalPayload, idsPayload
    ).pipe(
      timeout(12000),
      switchMap(() =>
        this.applyEditPromotion(this.editingProductId as number).pipe(
          timeout(10000),
          catchError(() => of(null))
        )
      ),
      finalize(() => { this.isSubmittingProduct = false; })
    ).subscribe({
      next: () => {
        this.closeProductModal();
        this.loadProducts();
      },
      error: (error) => {
        const backendDetail = String(
          error?.error?.message || error?.error?.error || ''
        ).trim();
        this.editProductError =
          `${this.resolveHttpErrorMessage(error, 'Impossible de modifier le produit.')}` +
          `${backendDetail ? ` (${backendDetail})` : ''}`;
      }
    });
  }

  private updateProductWithFallbacks(
    productId: number,
    richPayload: ProductWritePayload,
    minimalPayload: ProductWritePayload,
    idsPayload: unknown
  ): Observable<unknown> {
    return this.marketplaceApi.updateOwnerProduct(productId, richPayload).pipe(
      catchError((ownerErr) => {
        if (![400, 403, 404, 405, 501].includes(ownerErr?.status)) {
          return throwError(() => ownerErr);
        }
        return this.marketplaceApi.products.update(productId, richPayload).pipe(
          catchError((marketErr) => {
            if (![400, 403, 404, 405, 501].includes(marketErr?.status)) {
              return throwError(() => marketErr);
            }
            return this.marketplaceApi.products.update(productId, minimalPayload).pipe(
              catchError((minimalErr) => {
                if (![400, 403, 404, 405, 501].includes(minimalErr?.status)) {
                  return throwError(() => minimalErr);
                }
                return this.marketplaceApi.products.update(
                  productId, idsPayload as ProductWritePayload
                ).pipe(
                  catchError((idsErr) => {
                    if (![400, 403, 404, 405, 501].includes(idsErr?.status)) {
                      return throwError(() => idsErr);
                    }
                    return this.marketplaceApi.updateProductDirect(
                      productId, minimalPayload
                    ).pipe(
                      catchError((directErr) => {
                        if (![400, 403, 404, 405, 501].includes(directErr?.status)) {
                          return throwError(() => directErr);
                        }
                        return this.marketplaceApi.patchProductDirect(
                          productId, minimalPayload
                        );
                      })
                    );
                  })
                );
              })
            );
          })
        );
      })
    );
  }

  deleteEditedProductFromModal(): void {
    if (!this.editingProductId) return;
    const idToDelete = this.editingProductId;
    this.isSubmittingProduct = true;
    this.editProductError = '';

    this.withOwnerFallback(
      this.marketplaceApi.deleteOwnerProduct(idToDelete),
      () => this.marketplaceApi.products.delete(idToDelete)
    ).subscribe({
      next: () => {
        this.closeProductModal();
        this.loadProducts();
      },
      error: (error) => {
        this.editProductError = this.resolveHttpErrorMessage(
          error, 'Impossible de supprimer le produit.'
        );
        this.isSubmittingProduct = false;
      },
      complete: () => { this.isSubmittingProduct = false; }
    });
  }

  private applyEditPromotion(productId: number): Observable<unknown> {
    const discount = Number(this.editProductForm.discountPercent || 0);
    if (discount > 0) {
      return this.marketplaceApi.applyProductPromotion(productId, {
        discountPercent: discount,
        promoEndAt: new Date(this.editProductForm.promoEndAt).toISOString()
      });
    }
    return this.marketplaceApi.removeProductPromotion(productId).pipe(
      catchError(() => of(null))
    );
  }

  private parseDimensions(raw: string): {
    width: number; height: number; depth: number; unit: 'cm' | 'mm' | 'm'
  } {
    const text = String(raw || '').trim().toLowerCase();
    const unitMatch = text.match(/(cm|mm|m)\b/);
    const unit = (unitMatch?.[1] as 'cm' | 'mm' | 'm') || 'cm';
    const numbers = text
      .replace(',', '.')
      .match(/\d+(?:\.\d+)?/g)
      ?.map(n => Number(n))
      .filter(n => Number.isFinite(n)) || [];
    if (numbers.length >= 3) {
      return { width: numbers[0], height: numbers[1], depth: numbers[2], unit };
    }
    return { width: 0, height: 0, depth: 0, unit };
  }

  // ─── Delete ───────────────────────────────────────────────────────

  confirmDeleteProduct(productId: number, productName: string): void {
    this.deletingProductId = productId;
    this.deletingProductName = productName;
    this.isDeleteConfirmOpen = true;
  }

  closeDeleteConfirm(): void {
    this.isDeleteConfirmOpen = false;
    this.deletingProductId = null;
    this.deletingProductName = '';
  }

  confirmDelete(): void {
    if (!this.deletingProductId) return;
    this.isSubmittingDelete = true;

    this.withOwnerFallback(
      this.marketplaceApi.deleteOwnerProduct(this.deletingProductId),
      () => this.marketplaceApi.products.delete(this.deletingProductId as number)
    ).subscribe({
      next: () => {
        this.products = this.products.filter(p => p.id !== this.deletingProductId);
        this.closeDeleteConfirm();
      },
      error: (error) => {
        this.storeError = this.resolveHttpErrorMessage(
          error, 'Impossible de supprimer le produit.'
        );
        this.isSubmittingDelete = false;
      },
      complete: () => { this.isSubmittingDelete = false; }
    });
  }

  private withOwnerFallback<T>(
    primary: Observable<T>,
    fallback: () => Observable<T>
  ): Observable<T> {
    return primary.pipe(
      catchError((error) => {
        if ([400, 403, 404, 405, 501].includes(error?.status)) {
          return fallback();
        }
        return throwError(() => error);
      })
    );
  }

  // ─── Promotion ────────────────────────────────────────────────────

  openPromotionModal(productId: number): void {
    const product = this.products.find(item => item.id === productId);
    if (!product) return;
    this.promotionProductId = product.id;
    this.promotionProductName = product.name;
    this.promotionDiscountPercent = product.discountPercent || 10;
    this.promotionEndAt = product.promoEndAt
      ? this.toDateTimeLocal(product.promoEndAt) : '';
    this.promotionError = '';
    this.isPromotionModalOpen = true;
  }

  closePromotionModal(): void {
    this.isPromotionModalOpen = false;
    this.isSubmittingPromotion = false;
    this.promotionError = '';
    this.promotionProductId = null;
    this.promotionProductName = '';
  }

  submitPromotion(): void {
    if (!this.promotionProductId) return;
    if (!this.promotionEndAt) {
      this.promotionError = 'Veuillez choisir une date limite de promotion.';
      return;
    }
    const endDate = new Date(this.promotionEndAt);
    if (Number.isNaN(endDate.getTime()) || endDate.getTime() <= Date.now()) {
      this.promotionError = 'La date de fin doit etre dans le futur.';
      return;
    }
    if (this.promotionDiscountPercent <= 0 || this.promotionDiscountPercent > 100) {
      this.promotionError = 'Le discount doit etre entre 1% et 100%.';
      return;
    }

    this.isSubmittingPromotion = true;
    this.promotionError = '';

    this.marketplaceApi.applyProductPromotion(this.promotionProductId, {
      discountPercent: this.promotionDiscountPercent,
      promoEndAt: endDate.toISOString()
    }).subscribe({
      next: () => {
        this.closePromotionModal();
        this.loadProducts();
      },
      error: (error) => {
        this.isSubmittingPromotion = false;
        this.promotionError = this.resolveHttpErrorMessage(
          error, 'Impossible d\'appliquer la promotion.'
        );
      },
      complete: () => { this.isSubmittingPromotion = false; }
    });
  }

  removePromotion(productId: number): void {
    this.marketplaceApi.removeProductPromotion(productId).subscribe({
      next: () => this.loadProducts(),
      error: (error) => {
        this.storeError = this.resolveHttpErrorMessage(
          error, 'Impossible de supprimer la promotion.'
        );
      }
    });
  }

  // ─── Store ────────────────────────────────────────────────────────

  createMyStore(): void {
    if (!this.canCreateMoreStores) {
      this.storeError = 'Limite atteinte: maximum 3 boutiques par seller.';
      return;
    }

    this.storeError = '';
    this.router.navigate(['/create-store-step-2'], {
      queryParams: { fromDashboard: '1' }
    });
  }

  // ─── Load data ────────────────────────────────────────────────────

  private loadProducts(): void {
    this.marketplaceApi.products.list().subscribe({
      next: (products) => {
        const currentUserId = this.authService.currentUser()?.id;
        const ownedStoreIds = new Set(
          this.myStores
            .map((store) => Number(store.id || 0))
            .filter((id) => id > 0)
        );

        const backendScopedProducts = products.map((product) => {
          const parsedMeta = parseProductDescription(product.description || '');
          const backendStoreId = this.toNumber((product as any).storeId || product.store?.id);
          const rawSoldQuantity = (product as any).soldQuantity;
          const hasSoldQuantity = rawSoldQuantity !== null && rawSoldQuantity !== undefined;
          const resolvedSoldQuantity = hasSoldQuantity
            ? Math.max(0, this.toNumber(rawSoldQuantity))
            : (product.orderLines?.reduce((total, line) => total + line.quantity, 0) ?? 0);

          const discountPercent  = this.toNumber((product as any).discountPercent);
          const discountedPrice  = this.toNumber((product as any).discountedPrice);
          const originalPrice    = this.toNumber((product as any).originalPrice || product.price);
          const remainingSeconds = this.toNumber((product as any).remainingSeconds);
          const isPromotionActive =
            Boolean((product as any).isPromotionActive) ||
            (discountPercent > 0 && remainingSeconds > 0);

          // ✅ Fix category : backend prioritaire, puis parsedMeta, puis 'Other'
          const rawCategory =
            product.categoryName ||
            String((product as any).categoryName || '') ||
            product.category?.name ||
            parsedMeta.category ||
            '';
          const categoryName = this.normalizeCategoryName(rawCategory);

          // ✅ Fix imageUrl : backend TOUJOURS prioritaire
          const imageUrl = this.resolveProductImageUrl(product.imageUrl)
            || this.resolveProductImageUrl(parsedMeta.images[0]);

          // ✅ Fix images tableau : inclure imageUrl si parsedMeta.images vide
          const images = parsedMeta.images
            .map((image) => this.resolveProductImageUrl(image))
            .filter((image) => image.length > 0);

          if (!images.length && imageUrl) {
            images.push(imageUrl);
          }

          // ✅ Fix dimensionsLabel : ignorer "0 x 0 x 0 cm"
          const backendLabel = product.dimensionsLabel || '';
          const dimensionsLabel = (() => {
            if (backendLabel && !this.isZeroDimensionsLabel(backendLabel)) {
              return backendLabel;
            }
            const w = parsedMeta.width || 0;
            const h = parsedMeta.height || 0;
            const d = parsedMeta.depth || 0;
            return this.buildDimensionsLabel(w, h, d, parsedMeta.dimensionUnit || 'cm');
          })();

          return {
            // parsedMeta spread en premier — les valeurs backend écrasent ensuite
            ...parsedMeta,
            id: product.id,
            name: product.name,
            category: categoryName,
            price: isPromotionActive && discountedPrice > 0
              ? discountedPrice : product.price,
            originalPrice,
            discountedPrice: discountedPrice > 0 ? discountedPrice : undefined,
            discountPercent: discountPercent > 0 ? discountPercent : undefined,
            promoEndAt: (product as any).promoEndAt || undefined,
            remainingSeconds: remainingSeconds > 0 ? remainingSeconds : undefined,
            isPromotionActive,
            ownerId: product.store?.owner?.id,
            storeId: backendStoreId > 0 ? backendStoreId : undefined,
            categoryId: product.categoryId || product.category?.id,
            stock: product.stock,
            sales: resolvedSoldQuantity,
            imageUrl,
            images,
            weight: this.toNumber(product.weight ?? parsedMeta.weight),
            description: parsedMeta.description || product.description || '',
            dimensionsLabel
          };
        }).filter((product) => {
          const productStoreId = Number((product as any).storeId || 0);

          // Show all products from all shops owned by this seller.
          if (productStoreId > 0 && ownedStoreIds.has(productStoreId)) return true;

          // Fallback when store linkage is unavailable but ownership is present.
          if (currentUserId && product.ownerId === currentUserId) return true;

          return false;
        });

        this.products = this.mergeWithLocalStudentProducts(backendScopedProducts);

        if (this.pendingOpenEditProductId) {
          const productExists = this.products.some(
            p => p.id === this.pendingOpenEditProductId
          );
          if (productExists) {
            const productId = this.pendingOpenEditProductId;
            this.pendingOpenEditProductId = null;
            setTimeout(() => this.openEditProductModal(productId), 0);
            this.router.navigate([], {
              relativeTo: this.route,
              queryParams: { openEdit: null, tab: null },
              queryParamsHandling: 'merge',
              replaceUrl: true
            });
          }
        }
      },
      error: () => { this.products = []; }
    });
  }

  private loadMyStores(): void {
    this.storeLoading = true;
    this.storeError = '';

    this.marketplaceApi.getMyStores().subscribe({
      next: (stores) => {
        this.myStores = (stores || []).map((store) => ({
          id: Number(store.id || 0),
          name: String(store.name || '').trim(),
          balance: store.balance,
          rating: store.rating,
          categories: this.mapStoreCategories(store.categories)
        })).filter((store) => store.id > 0 && !!store.name);

        if (!this.myStores.length) {
          this.myStore = null;
          this.activeStoreId = null;
          this.storeCategories = null;
          this.storeStats = null;
          this.storeStatsError = '';
          this.isStoreStatsLoading = false;
          this.products = [];
          return;
        }

        const routeStoreId = Number(this.route.snapshot.queryParamMap.get('storeId') || 0);
        const desiredStoreId = (this.activeStoreId && this.activeStoreId > 0)
          ? this.activeStoreId
          : (routeStoreId > 0 ? routeStoreId : this.myStores[0].id);

        this.setActiveStore(desiredStoreId);
      },
      error: (error) => {
        if (error?.status === 404) {
          this.myStores = [];
          this.myStore = null;
          this.activeStoreId = null;
          this.storeCategories = null;
          this.storeStats = null;
          this.storeStatsError = '';
          this.isStoreStatsLoading = false;
          this.products = [];
          return;
        }
        this.storeError = this.resolveHttpErrorMessage(
          error, 'Impossible de charger vos boutiques.'
        );
      },
      complete: () => { this.storeLoading = false; }
    });
  }

  private setActiveStore(storeId: number): void {
    const selectedStore = this.myStores.find((item) => item.id === storeId) || this.myStores[0];
    if (!selectedStore) {
      this.myStore = null;
      this.activeStoreId = null;
      this.storeCategories = null;
      this.storeStats = null;
      this.storeStatsError = '';
      this.isStoreStatsLoading = false;
      return;
    }

    this.activeStoreId = selectedStore.id;
    this.myStore = {
      id: selectedStore.id,
      name: selectedStore.name,
      balance: selectedStore.balance,
      rating: selectedStore.rating
    };
    this.storeCategories = selectedStore.categories;
    this.loadProducts();
    this.loadStoreStats();
  }

  private mapStoreCategories(categories: unknown): Array<{ id: number; name: string }> {
    if (!Array.isArray(categories)) {
      return [];
    }

    return categories.map((cat) => {
      if (typeof cat === 'string') {
        return { id: 0, name: cat };
      }
      return {
        id: Number((cat as any)?.id || 0),
        name: String((cat as any)?.name || '').trim()
      };
    }).filter((cat) => !!cat.name);
  }

  private mergeWithLocalStudentProducts(
    backendProducts: typeof this.products
  ): typeof this.products {
    const localProducts = this.readLocalStoreProductsFromCreation();
    if (!localProducts.length) return backendProducts;

    const result = [...backendProducts];

    for (const product of localProducts) {
      const localId   = Number((product as any)?.id   || 0);
      const localName = String((product as any)?.name || '').trim().toLowerCase();

      const existingIndex = result.findIndex((item) => {
        const itemId = Number(item.id || 0);
        const itemName = String(item.name || '').trim().toLowerCase();
        return (localId > 0 && itemId === localId) || (!!localName && itemName === localName);
      });

      if (existingIndex >= 0) {
        const localMapped = this.mapLocalProductToDashboardProduct(product);
        const current = result[existingIndex];

        result[existingIndex] = {
          ...current,
          imageUrl: current.imageUrl || localMapped.imageUrl,
          images: current.images?.length ? current.images : localMapped.images,
          weight: this.toNumber(current.weight) > 0 ? current.weight : localMapped.weight,
          dimensionsLabel: this.isMissingDimensionsLabel(current.dimensionsLabel)
            ? localMapped.dimensionsLabel
            : current.dimensionsLabel,
          width: this.toNumber(current.width) > 0 ? current.width : localMapped.width,
          height: this.toNumber(current.height) > 0 ? current.height : localMapped.height,
          depth: this.toNumber(current.depth) > 0 ? current.depth : localMapped.depth,
          dimensionUnit: current.dimensionUnit || localMapped.dimensionUnit,
          category: current.category || localMapped.category
        };
        continue;
      }

      result.push(this.mapLocalProductToDashboardProduct(product));
    }
    return result;
  }

  private readLocalStoreProductsFromCreation(): Product[] {
    const currentStoreName = String(this.myStore?.name || '').toLowerCase().trim();
    if (!currentStoreName) return [];

    const fromProductService = this.productService.allProducts().filter(product => {
      const seller = String(product.seller || '').toLowerCase().trim();
      return seller === currentStoreName;
    });

    const storageKey =
      `store_showcase_products_${currentStoreName.replace(/\s+/g, '-')}`;
    let fromLocalStorage: Product[] = [];
    try {
      const raw = localStorage.getItem(storageKey);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) fromLocalStorage = parsed as Product[];
      }
    } catch {
      fromLocalStorage = [];
    }

    const merged = [...fromProductService, ...fromLocalStorage];
    const unique = new Map<string, Product>();
    for (const item of merged) {
      const id            = Number((item as any)?.id || 0);
      const normalizedName = String(item.name || '').trim().toLowerCase();
      const key           = id > 0 ? `id:${id}` : `name:${normalizedName}`;
      unique.set(key, item);
    }
    return Array.from(unique.values());
  }

  private mapLocalProductToDashboardProduct(product: Product): typeof this.products[0] {
    const parsedMeta = parseProductDescription(product.description || '');
    const width  = Number(parsedMeta.width  || 0);
    const height = Number(parsedMeta.height || 0);
    const depth  = Number(parsedMeta.depth  || 0);

    // ✅ "Not specified" si dimensions à zéro
    const localRawDimensions = String((product as any)?.dimensionsLabel || '').trim();
    const dimensionsLabel = !this.isMissingDimensionsLabel(localRawDimensions)
      ? localRawDimensions
      : this.buildDimensionsLabel(width, height, depth, parsedMeta.dimensionUnit || 'cm');

    // ✅ Category normalisée
    const categoryName = this.normalizeCategoryName(
      this.getPrimaryStoreCategoryName() || parsedMeta.category || product.category || ''
    );

    // ✅ imageUrl : champ direct du produit local ou parsedMeta
    const imageUrl =
      this.resolveProductImageUrl((product as any).imageUrl) ||
      this.resolveProductImageUrl(product.image) ||
      this.resolveProductImageUrl(parsedMeta.images[0]);

    const weight = this.toNumber(product.weight ?? parsedMeta.weight);

    return {
      id: Number(product.id || Date.now()),
      name: String(product.name || ''),
      price: Number(product.price || 0),
      originalPrice: product.originalPrice,
      discountedPrice: product.discountedPrice,
      discountPercent: product.discountPercent,
      promoEndAt: product.promoEndAt,
      remainingSeconds: product.remainingSeconds,
      isPromotionActive: product.isPromotionActive,
      ownerId: this.authService.currentUser()?.id,
      storeId: this.myStore?.id,
      categoryId: product.categoryId,
      stock: Number(product.stock || 0),
      sales: 0,
      imageUrl,
      images: parsedMeta.images.length > 0
        ? parsedMeta.images
        : (imageUrl ? [imageUrl] : []),
      variants: parsedMeta.variants,
      category: categoryName,
      sku: parsedMeta.sku || '',
      width,
      height,
      depth,
      dimensionUnit: parsedMeta.dimensionUnit || 'cm',
      weight,
      dimensionsLabel,
      description: parsedMeta.description || product.description || ''
    };
  }

  private loadOrders(): void {
    this.marketplaceApi.orders.list().subscribe({
      next: (orders) => {
        this.orders = orders.map(order => {
          const products = (order.orderLines || []).map(line => {
            const product    = line.product;
            const parsedMeta = product
              ? parseProductDescription(product.description || '') : null;
            const lineDimensionsLabel =
              String((line as any)?.dimensionsLabel || '').trim();
            const parsedLineDimensions = lineDimensionsLabel
              ? this.parseDimensions(lineDimensionsLabel) : null;

            const width  = Number(parsedLineDimensions?.width  || parsedMeta?.width  || 0);
            const height = Number(parsedLineDimensions?.height || parsedMeta?.height || 0);
            const depth  = Number(parsedLineDimensions?.depth  || parsedMeta?.depth  || 0);
            const dimensionUnit = String(
              parsedLineDimensions?.unit || parsedMeta?.dimensionUnit || 'cm'
            ) as 'cm' | 'mm' | 'm';
            const weight = Number((line as any)?.weight || parsedMeta?.weight || 0);

            // ✅ "N/A" si dimensions à zéro pour les orders
            const dimensionsLabel = (() => {
              if (
                lineDimensionsLabel &&
                !this.isZeroDimensionsLabel(lineDimensionsLabel)
              ) {
                return lineDimensionsLabel;
              }
              return this.buildDimensionsLabel(
                width, height, depth, dimensionUnit, 'N/A'
              );
            })();

            return {
              name: product?.name || 'Product',
              quantity: line.quantity,
              price: line.price,
              width, height, depth,
              dimensionUnit,
              weight,
              dimensionsLabel
            };
          });

          return {
            id: `ORD${order.id}`,
            customer: order.user?.name ?? 'Customer',
            amount: order.totalAmount,
            status: order.status,
            items: `${order.orderLines?.length ?? 0} item(s)`,
            products
          };
        });
      },
      error: () => { this.orders = []; }
    });
  }

  private loadStoreStats(): void {
    const storeIds = this.myStores
      .map((store) => Number(store.id || 0))
      .filter((id) => id > 0);

    if (!storeIds.length) {
      this.storeStats = null;
      this.storeStatsError = '';
      this.isStoreStatsLoading = false;
      return;
    }

    this.isStoreStatsLoading = true;
    this.storeStatsError = '';

    const statsRequests = storeIds.map((storeId) =>
      this.marketplaceApi.getStoreStats(storeId).pipe(
        catchError((error) => {
          // Keep dashboard available even if one store stats call fails.
          console.warn(`[VendorDashboard] Stats unavailable for store ${storeId}`, error);
          return of(null);
        })
      )
    );

    forkJoin(statsRequests).subscribe({
      next: (statsResults) => {
        const validStats = (statsResults || []).filter((item) => item !== null) as StoreStatsResource[];
        if (!validStats.length) {
          this.storeStats = null;
          this.storeStatsError = 'Impossible de charger les statistiques de vos boutiques.';
          return;
        }

        this.storeStats = this.aggregateStoreStats(validStats);
      },
      error: (error: unknown) => {
        const httpError = error as { status?: number; error?: { message?: string } };
        if (httpError?.status === 403) {
          this.storeStatsError = 'Vous n\'etes pas autorise a consulter ces statistiques.';
        } else {
          this.storeStatsError = this.resolveHttpErrorMessage(
            httpError,
            'Impossible de charger les statistiques de la boutique.'
          );
        }
        this.storeStats = null;
      },
      complete: () => {
        this.isStoreStatsLoading = false;
      }
    });
  }

  private aggregateStoreStats(statsList: StoreStatsResource[]): StoreStatsResource {
    const totalRevenue = statsList.reduce(
      (sum, stats) => sum + Number(stats?.totalRevenue || 0),
      0
    );

    const totalOrders = statsList.reduce(
      (sum, stats) => sum + Number(stats?.totalOrders || 0),
      0
    );

    const monthlyRevenueMap = new Map<string, number>();
    for (const stats of statsList) {
      const monthly = Array.isArray(stats?.monthlyRevenue) ? stats.monthlyRevenue : [];
      for (const item of monthly) {
        const monthKey = String(item?.month || '').trim();
        if (!monthKey) {
          continue;
        }
        const current = monthlyRevenueMap.get(monthKey) || 0;
        monthlyRevenueMap.set(monthKey, current + Number(item?.revenue || 0));
      }
    }

    const monthlyRevenue = Array.from(monthlyRevenueMap.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([month, revenue]) => ({ month, revenue }));

    const bestSellerMap = new Map<string, {
      productId?: number | null;
      productName?: string | null;
      totalQuantity: number;
      revenue: number;
    }>();

    for (const stats of statsList) {
      const bestSeller = stats?.bestSeller;
      if (!bestSeller) {
        continue;
      }

      const keyById = Number(bestSeller.productId || 0);
      const keyByName = String(bestSeller.productName || '').trim().toLowerCase();
      const key = keyById > 0 ? `id:${keyById}` : `name:${keyByName}`;
      if (!key || key === 'name:') {
        continue;
      }

      const previous = bestSellerMap.get(key);
      if (!previous) {
        bestSellerMap.set(key, {
          productId: bestSeller.productId,
          productName: bestSeller.productName,
          totalQuantity: Number(bestSeller.totalQuantity || 0),
          revenue: Number(bestSeller.revenue || 0)
        });
        continue;
      }

      previous.totalQuantity += Number(bestSeller.totalQuantity || 0);
      previous.revenue += Number(bestSeller.revenue || 0);
      if (!previous.productName && bestSeller.productName) {
        previous.productName = bestSeller.productName;
      }
      if (!previous.productId && bestSeller.productId) {
        previous.productId = bestSeller.productId;
      }
      bestSellerMap.set(key, previous);
    }

    const bestSeller = Array.from(bestSellerMap.values())
      .sort((a, b) => {
        if (b.totalQuantity !== a.totalQuantity) {
          return b.totalQuantity - a.totalQuantity;
        }
        return b.revenue - a.revenue;
      })[0] || null;

    return {
      totalRevenue,
      totalOrders,
      bestSeller: bestSeller
        ? {
            productId: bestSeller.productId,
            productName: bestSeller.productName,
            totalQuantity: bestSeller.totalQuantity,
            revenue: bestSeller.revenue
          }
        : null,
      monthlyRevenue
    };
  }

  private applyStoreTheme(): void {
    const selectedThemeId =
      localStorage.getItem('create_store_selected_theme') || 'elegant';
    const palette =
      this.themePaletteMap[selectedThemeId] || this.themePaletteMap['elegant'];
    this.themeStyle = {
      '--vendor-primary': palette.primary,
      '--vendor-surface': palette.surface,
      '--vendor-text':    palette.text,
      '--vendor-accent':  palette.accent
    };
  }

  get overviewTotalSales(): number {
    if (this.storeStats) {
      return Number(this.storeStats.totalRevenue || 0);
    }

    return this.myStores.reduce((sum, store) => {
      return sum + Number(store.balance || 0);
    }, 0);
  }

  get overviewTotalOrders(): number {
    if (this.storeStats) {
      return Number(this.storeStats.totalOrders || 0);
    }

    return this.orders.length;
  }

  get overviewAverageRating(): number {
    const ratings = this.myStores
      .map((store) => Number(store.rating || 0))
      .filter((rating) => rating > 0);

    if (!ratings.length) {
      return 0;
    }

    const total = ratings.reduce((sum, rating) => sum + rating, 0);
    return total / ratings.length;
  }

  canAccessMyShop(): boolean {
    return !this.isNoStoreProductFlow && !!String(this.myStore?.name || '').trim();
  }

  get canCreateMoreStores(): boolean {
    return this.myStores.length < this.maxStoresPerSeller;
  }

  private resolveNoStoreProductFlow(): boolean {
    const currentEmail = String(this.authService.currentUser()?.email || '').toLowerCase().trim();
    if (!currentEmail) {
      return false;
    }

    if (this.hasCurrentUserStoreProfile(currentEmail)) {
      return false;
    }

    try {
      const raw = localStorage.getItem(VendorDashboardComponent.LAST_REQUEST_KEY);
      if (!raw) {
        return false;
      }

      const parsed = JSON.parse(raw) as { email?: string; hasFiveOrMoreArticles?: boolean };
      const requestEmail = String(parsed?.email || '').toLowerCase().trim();
      if (!requestEmail || requestEmail !== currentEmail) {
        return false;
      }

      return parsed?.hasFiveOrMoreArticles === false;
    } catch {
      return false;
    }
  }

  private hasCurrentUserStoreProfile(currentEmail: string): boolean {
    try {
      const raw = localStorage.getItem(VendorDashboardComponent.CREATED_STORE_PROFILE_KEY);
      if (!raw) {
        return false;
      }

      const profile = JSON.parse(raw) as {
        ownerEmail?: string;
        name?: string;
        categoryId?: string;
        logoDataUrl?: string;
      };

      const ownerEmail = String(profile?.ownerEmail || '').toLowerCase().trim();
      if (ownerEmail && ownerEmail !== currentEmail) {
        return false;
      }

      return !!String(profile?.name || '').trim()
        && !!String(profile?.categoryId || '').trim()
        && !!String(profile?.logoDataUrl || '').trim();
    } catch {
      return false;
    }
  }

  // ─── Utils ────────────────────────────────────────────────────────

  private resolveHttpErrorMessage(error: any, fallback: string): string {
    if (error?.status === 403) return 'Vous n\'etes pas proprietaire de ce produit.';
    if (error?.status === 404) return 'Produit/Boutique introuvable.';
    if (error?.status === 400) {
      return error?.error?.message || 'Donnees invalides (prix/stock/discount/date).';
    }
    return error?.error?.message || fallback;
  }

  private toNumber(value: unknown): number {
    const numeric = Number(value);
    return Number.isFinite(numeric) ? numeric : 0;
  }

  private toDateTimeLocal(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    const pad = (n: number) => String(n).padStart(2, '0');
    return (
      `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
      `T${pad(date.getHours())}:${pad(date.getMinutes())}`
    );
  }
}