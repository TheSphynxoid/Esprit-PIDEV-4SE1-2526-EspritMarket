import { Component, OnDestroy, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import { ProductService, CartService, AuthService } from '../../../services';
import { MarketplaceApiService } from '../../../services/api';
import { parseProductDescription } from '../../../services/product-meta.util';

@Component({
  selector: 'app-marketplace',
  standalone: true,
  imports: [CommonModule, FormsModule, HeaderComponent, FooterComponent],
  templateUrl: './marketplace.component.html',
    styleUrl: './marketplace.component.css'
})
export class MarketplaceComponent implements OnInit, OnDestroy {
  private static readonly LAST_REQUEST_KEY = 'create_store_last_request';
  private static readonly CREATED_STORE_PROFILE_KEY = 'create_store_latest_shop_profile';
  productService = inject(ProductService);
  cartService = inject(CartService);
  authService = inject(AuthService);
  router = inject(Router);
  marketplaceApi = inject(MarketplaceApiService);

  // Reactive signals for filter state
  searchQuery = signal('');
  minRating = signal(0);
  priceMin = signal(0);
  priceMax = signal(2000);
  isSemanticMode = signal(false);
  semanticResults = signal<any[]>([]);
  isSemanticLoading = signal(false);
  error = '';

  // Computed results that switch between normal and semantic
  displayProducts = computed(() => {
    if (this.isSemanticMode()) {
      return this.semanticResults();
    }
    return this.productService.paginatedProducts();
  });

  // Current page computed from service
  currentPage = computed(() => this.productService.currentPageNumber());
  private countdownTimerId: ReturnType<typeof setInterval> | null = null;
  promotionCountdowns = signal<Record<number, number>>({});
  hasMyStore = signal(false);
  myStoreName = signal('');

  constructor() {
    // Initialize prices
    const priceRange = this.productService.priceRange();
    this.priceMin.set(priceRange.min);
    this.priceMax.set(priceRange.max);

    this.initPromotionCountdowns();
    this.countdownTimerId = setInterval(() => {
      this.promotionCountdowns.update((current) => {
        const next: Record<number, number> = {};
        Object.entries(current).forEach(([id, seconds]) => {
          const remaining = Math.max(0, Number(seconds) - 1);
          if (remaining > 0) {
            next[Number(id)] = remaining;
          }
        });
        return next;
      });
    }, 1000);
  }

  ngOnInit(): void {
    this.loadMyStoreAccess();
  }

  ngOnDestroy(): void {
    if (this.countdownTimerId) {
      clearInterval(this.countdownTimerId);
      this.countdownTimerId = null;
    }
  }

  onSearch(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchQuery.set(target.value);
    this.productService.setSearchQuery(target.value);
  }

  onCategoryChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    const category = target.value || undefined;
    this.productService.setCategory(category);
  }

  onPriceMinChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    const min = parseInt(target.value, 10);
    this.priceMin.set(min);
    this.productService.setPriceRange([min, this.priceMax()]);
  }

  onPriceMaxChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    const max = parseInt(target.value, 10);
    this.priceMax.set(max);
    this.productService.setPriceRange([this.priceMin(), max]);
  }

  onRatingChange(rating: number): void {
    this.minRating.set(this.minRating() === rating ? 0 : rating);
    this.productService.setMinRating(this.minRating() === rating ? 0 : rating);
  }

  onSortChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    this.productService.setSortBy(target.value as any);
  }

  onResetFilters(): void {
    this.searchQuery.set('');
    this.minRating.set(0);
    this.isSemanticMode.set(false);
    this.semanticResults.set([]);
    const priceRange = this.productService.priceRange();
    this.priceMin.set(priceRange.min);
    this.priceMax.set(priceRange.max);
    this.productService.resetFilters();
    this.initPromotionCountdowns();
  }

  onSemanticSearch(): void {
    const query = this.searchQuery().trim();
    if (!query) {
      this.isSemanticMode.set(false);
      return;
    }

    this.isSemanticLoading.set(true);
    this.isSemanticMode.set(true);
    this.error = '';

    this.marketplaceApi.semanticSearch({ query })
      .subscribe({
        next: (res) => {
          this.isSemanticLoading.set(false);
          console.log('Réponse IA reçue:', res);
          const mapped = res.products.map(p => this.mapProductResource(p));
          console.log('Produits mappés:', mapped);
          this.semanticResults.set(mapped);
          
          if (res.correctedQuery && res.correctedQuery !== query) {
             this.searchQuery.set(res.correctedQuery);
          }
        },
        error: (err) => {
          this.isSemanticLoading.set(false);
          this.isSemanticMode.set(false);
          console.error('Erreur IA Sémantique:', err);
        }
      });
  }

  private mapProductResource(p: any): any {
    // Reuse logic from ProductService if possible, or simple mapping
    return {
      ...p,
      image: this.resolveProductImageUrl(p.imageUrl),
      seller: p.store?.name || 'Vendeur Inconnu',
      createdAt: new Date(),
      updatedAt: new Date()
    };
  }

  private resolveProductImageUrl(raw: string | null | undefined): string {
    const value = String(raw || '').trim();
    if (!value) {
      return '';
    }

    if (value.startsWith('http://') || value.startsWith('https://') || value.startsWith('data:image/')) {
      return value;
    }

    if (value.startsWith('/uploads/') || value.startsWith('uploads/')) {
      return `http://localhost:8088${value.startsWith('/') ? '' : '/'}${value}`;
    }

    if (value.startsWith('/products/') || value.startsWith('products/')) {
      const cleaned = value.replace(/^\/?products\//, '/uploads/products/');
      return `http://localhost:8088${cleaned}`;
    }

    return value;
  }

  onAddToCart(product: any): void {
    if (this.isProductDisabled(product)) {
      return;
    }

    const alreadyInCart = this.cartService.getItem(product.id)?.quantity || 0;
    if (alreadyInCart >= Number(product.stock || 0)) {
      return;
    }

    this.cartService.addItem(this.buildCartProduct(product), 1);
  }

  private buildCartProduct(product: any): any {
    const parsedMeta = parseProductDescription(String(product?.description || ''));
    const rawDescription = String(product?.description || '');

    const dimensionsMatch = rawDescription.match(
      /dimensions?\s*[:\-]?\s*(\d+(?:[.,]\d+)?)\s*[x*]\s*(\d+(?:[.,]\d+)?)\s*[x*]\s*(\d+(?:[.,]\d+)?)\s*(cm|mm|m)?/i
    );
    const weightMatch = rawDescription.match(/(?:poids|weight)\s*[:\-]?\s*(\d+(?:[.,]\d+)?)\s*(kg|g)?/i);

    const width = Number(parsedMeta.width || dimensionsMatch?.[1]?.replace(',', '.') || product?.width || 0);
    const height = Number(parsedMeta.height || dimensionsMatch?.[2]?.replace(',', '.') || product?.height || 0);
    const depth = Number(parsedMeta.depth || dimensionsMatch?.[3]?.replace(',', '.') || product?.depth || 0);
    const dimensionUnit = String(parsedMeta.dimensionUnit || dimensionsMatch?.[4] || product?.dimensionUnit || 'cm');
    const weight = Number(parsedMeta.weight || weightMatch?.[1]?.replace(',', '.') || product?.weight || 0);

    const dimensions =
      String(product?.dimensions || '').trim() ||
      ((width > 0 || height > 0 || depth > 0) ? `${width} x ${height} x ${depth} ${dimensionUnit}` : '');

    return {
      ...product,
      width,
      height,
      depth,
      dimensionUnit,
      dimensions,
      weight
    };
  }

  isProductDisabled(product: any): boolean {
    return Number(product?.stock || 0) <= 0;
  }

  onProductCardClick(product: any): void {
    if (this.isProductDisabled(product)) {
      return;
    }

    this.openProduct(product.id);
  }

  openProduct(productId: number): void {
    this.router.navigate(['/product', productId]);
  }

  goToCheckout(): void {
    this.router.navigate(['/checkout']);
  }

  goToVisualSearch(): void {
    this.router.navigate(['/visual-search']);
  }

  goToMyStore(): void {
    const storeName = this.myStoreName().trim();
    if (!storeName) {
      return;
    }

    this.router.navigate(['/store', storeName]);
  }

  canOpenQuickAddProduct(): boolean {
    if (!this.authService.isLoggedIn()) {
      return false;
    }

    const currentEmail = String(this.authService.currentUser()?.email || '').toLowerCase().trim();
    if (!currentEmail) {
      return false;
    }

    if (this.hasCurrentUserStoreProfile(currentEmail)) {
      return false;
    }

    try {
      const raw = localStorage.getItem(MarketplaceComponent.LAST_REQUEST_KEY);
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

  openQuickAddProduct(): void {
    this.router.navigate(['/create-store'], { queryParams: { openProductOnly: '1' } });
  }

  goToStore(sellerName: string, event?: Event): void {
    event?.stopPropagation();
    if (sellerName && sellerName.trim()) {
      this.router.navigate(['/store', sellerName.trim()]);
    }
  }

  isImageSource(value: string | undefined | null): boolean {
    return !!this.resolveProductImageUrl(value);
  }

  renderStars(rating: number): string {
    const fullStars = Math.floor(rating);
    const hasHalfStar = rating % 1 > 0;
    let stars = '⭐'.repeat(fullStars);
    if (hasHalfStar) stars += '✨';
    return stars;
  }

  formatPrice(price: number): string {
    const safePrice = Number.isFinite(Number(price)) ? Number(price) : 0;
    return new Intl.NumberFormat('fr-TN', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2
    }).format(safePrice);
  }

  cleanProductDescription(product: any): string {
    const rawDescription = String(product?.description || '');
    const parsed = parseProductDescription(rawDescription);
    const cleaned = String(parsed?.description || '').trim();
    if (cleaned) {
      return cleaned;
    }

    const [fallback] = rawDescription.split('---product-meta---');
    return String(fallback || '').trim() || 'Description indisponible.';
  }

  getCategoryLabel(product: any): string {
    const explicitLabel = String(product?.categoryLabel || '').trim();
    if (explicitLabel && explicitLabel.toLowerCase() !== 'general') {
      return explicitLabel.toLowerCase().startsWith('categorie:') ? explicitLabel : `Categorie: ${explicitLabel}`;
    }

    const parsed = parseProductDescription(String(product?.description || ''));
    const rawCategory = String(parsed?.category || product?.category || '').trim();
    if (!rawCategory) {
      return 'Categorie: Autre';
    }

    const normalized = rawCategory.toLowerCase().replace(/[_-]+/g, ' ').replace(/\s+/g, ' ').trim();
    const dictionary: Record<string, string> = {
      electronics: 'Informatique',
      books: 'Livres',
      clothing: 'Vetements',
      services: 'Services',
      other: 'Autre',
      'art paintings': 'Art',
      electromenager: 'Electromenager',
      mobilier: 'Mobilier',
      mode: 'Mode',
      sport: 'Sport',
      beaute: 'Beaute',
      auto: 'Auto',
      cuisine: 'Cuisine',
      gaming: 'Gaming'
    };

    const label = dictionary[normalized]
      || normalized
          .split(' ')
          .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
          .join(' ');

    return `Categorie: ${label}`;
  }

  isPromotionVisible(product: any): boolean {
    return Boolean(product?.isPromotionActive) && this.getRemainingSeconds(product) > 0;
  }

  getRemainingSeconds(product: any): number {
    const direct = this.promotionCountdowns()[product.id];
    if (typeof direct === 'number') {
      return direct;
    }
    return Number(product?.remainingSeconds || 0);
  }

  formatRemainingTime(totalSeconds: number): string {
    const safeSeconds = Math.max(0, Number(totalSeconds || 0));
    const hours = Math.floor(safeSeconds / 3600);
    const minutes = Math.floor((safeSeconds % 3600) / 60);
    const seconds = safeSeconds % 60;
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
  }

  get canDeleteProductsAsAdmin(): boolean {
    return this.authService.currentUser()?.role === 'admin_market';
  }

  deleteProductAsAdmin(productId: number, productName: string, event: Event): void {
    event.stopPropagation();

    if (!this.canDeleteProductsAsAdmin || !productId) {
      return;
    }

    const safeName = (productName || 'cet article').trim();
    const confirmed = window.confirm(`Supprimer l'article "${safeName}" ?`);
    if (!confirmed) {
      return;
    }

    this.productService.deleteProduct(productId);
  }

  private loadMyStoreAccess(): void {
    const user = this.authService.currentUser();
    if (!user || !this.authService.isLoggedIn()) {
      this.hasMyStore.set(false);
      this.myStoreName.set('');
      return;
    }

    this.marketplaceApi.getMyStore().subscribe({
      next: (store) => {
        const name = String(store?.name || '').trim();
        if (!name) {
          this.hasMyStore.set(false);
          this.myStoreName.set('');
          return;
        }

        this.hasMyStore.set(true);
        this.myStoreName.set(name);
      },
      error: () => {
        this.hasMyStore.set(false);
        this.myStoreName.set('');
      }
    });
  }

  private hasCurrentUserStoreProfile(currentEmail: string): boolean {
    try {
      const raw = localStorage.getItem(MarketplaceComponent.CREATED_STORE_PROFILE_KEY);
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

  private initPromotionCountdowns(): void {
    const next: Record<number, number> = {};
    this.productService.allProducts().forEach((product: any) => {
      const seconds = Number(product?.remainingSeconds || 0);
      if (seconds > 0) {
        next[product.id] = seconds;
      }
    });
    this.promotionCountdowns.set(next);
  }

  getPageNumbers(): number[] {
    const total = this.productService.totalPages();
    const current = this.productService.currentPageNumber();
    const pages: number[] = [];
    const maxVisible = 5;

    if (total <= maxVisible) {
      for (let i = 1; i <= total; i++) pages.push(i);
    } else {
      const start = Math.max(1, current - 2);
      const end = Math.min(total, current + 2);

      if (start > 1) pages.push(1);
      if (start > 2) pages.push(-1); // Ellipsis

      for (let i = start; i <= end; i++) pages.push(i);

      if (end < total - 1) pages.push(-2); // Ellipsis
      if (end < total) pages.push(total);
    }

    return pages;
  }
}
