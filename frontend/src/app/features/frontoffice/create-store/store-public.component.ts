import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ProductService, Product, AuthService, MarketplaceApiService } from '../../../services';
import {
  CREATED_STORE_PROFILE_KEY,
  PublicStoreProfile,
  deletePublicStoreProfile,
  getPublicStoreProfile,
  getStoreTheme,
  StoreTheme
} from './store-personalization';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';


const THEME_HERO_IMAGES: Record<string, string> = {
  'modern-elegant': 'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=1600&q=80',
  'minimal-clean': 'https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?auto=format&fit=crop&w=1600&q=80',
  'creative-colorful': 'https://images.unsplash.com/photo-1513364776144-60967b0f800f?auto=format&fit=crop&w=1600&q=80',
  'chic-fashion': 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=1600&q=80',
  'sweet-bakery': 'https://images.unsplash.com/photo-1519864600265-abb23847ef2c?auto=format&fit=crop&w=1600&q=80',
  'dynamic-energy': 'https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?auto=format&fit=crop&w=1600&q=80'
};

@Component({
  selector: 'app-store-public',
  standalone: true,
  imports: [CommonModule, RouterLink, HeaderComponent, FooterComponent],
  templateUrl: './store-public.component.html',
  styleUrls: ['./store-showcase.component.css']
})
export class StorePublicComponent implements OnInit {
  private static readonly STORE_RATING_STATS_KEY = 'marketplace_store_rating_stats';
  private static readonly STORE_VISITOR_RATINGS_KEY = 'marketplace_store_visitor_ratings';

  profile: PublicStoreProfile | null = null;
  theme: StoreTheme = getStoreTheme('');
  storeProducts: Product[] = [];
  storeName: string = '';
  isNotFound = false;
  storeAverageRating = 0;
  storeReviewCount = 0;
  visitorStoreRating = 0;
  ownedStoreName = '';
  localOwnedStoreName = '';
  private countdownIntervalId: any;


  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private productService: ProductService,
    private authService: AuthService,
    private marketplaceApi: MarketplaceApiService
  ) {}

  ngOnInit(): void {
    // Get store name from route params
    this.route.params.subscribe((params) => {
      this.storeName = String(params['storeName'] || '').trim();
      this.loadStore();
    });

    this.productService.refresh();
    setTimeout(() => this.loadStore(), 350);
    this.resolveOwnedStore();
    this.resolveOwnedStoreFromLocalProfile();
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


  loadStore(): void {
    if (!this.storeName) {
      this.isNotFound = true;
      return;
    }

    this.isNotFound = false;

    // Get store profile from localStorage
    this.profile = getPublicStoreProfile(this.storeName);

    if (!this.profile) {
      // Fallback: build a lightweight public profile from seller products.
      // This handles older stores created before the public profile key existed.
      const matchingProducts = this.findProductsForStore();
      if (matchingProducts.length > 0) {
        this.profile = {
          id: this.normalize(this.storeName),
          name: this.storeName,
          categoryId: 'CLOTHING_BRAND',
          categoryLabel: 'Boutique',
          phone: '-',
          description: `Boutique de ${this.storeName}`,
          logoDataUrl: 'https://ui-avatars.com/api/?name=' + encodeURIComponent(this.storeName) + '&background=C8102E&color=fff',
          themeId: 'modern-elegant',
          createdAt: new Date().toISOString(),
          ownerName: this.storeName
        };
        this.storeProducts = matchingProducts;
      } else {
        this.isNotFound = true;
        return;
      }
    }

    // Get theme
    this.theme = getStoreTheme(this.profile.categoryId);
    this.ensureGoogleFontLoaded(this.theme.headingFont);
    this.ensureGoogleFontLoaded(this.theme.bodyFont);

    // Load products for this store
    this.loadStoreProducts();
    this.loadStoreRatings();
  }

  loadStoreProducts(): void {
    if (!this.storeName) {
      this.storeProducts = [];
      return;
    }

    this.storeProducts = this.findProductsForStore();
  }

  get themeVars(): Record<string, string> {
    return {
      '--store-bg': this.theme.background,
      '--store-glow': this.theme.decorativeGlow,
      '--store-hero-overlay': this.theme.heroOverlay,
      '--store-card-bg': this.theme.cardBackground,
      '--store-text': this.theme.text,
      '--store-muted': this.theme.mutedText,
      '--store-primary': this.theme.primary,
      '--store-primary-strong': this.theme.primaryStrong,
      '--store-accent': this.theme.accent,
      '--store-heading-font': this.theme.headingFont,
      '--store-body-font': this.theme.bodyFont
    };
  }

  get heroImage(): string {
    return THEME_HERO_IMAGES[this.theme.id] || THEME_HERO_IMAGES['minimal-clean'];
  }

  /**
   * Check if current user is the store owner
   */
  get isOwner(): boolean {
    if (!this.authService.isLoggedIn()) {
      return false;
    }

    const currentStoreKey = this.normalize(this.profile?.name || this.storeName);
    if (!currentStoreKey) {
      return false;
    }

    const ownedStoreKey = this.normalize(this.ownedStoreName);
    const localOwnedStoreKey = this.normalize(this.localOwnedStoreName);
    if (ownedStoreKey && currentStoreKey === ownedStoreKey) {
      return true;
    }

    if (localOwnedStoreKey && currentStoreKey === localOwnedStoreKey) {
      return true;
    }

    const currentUser = this.authService.currentUser();
    const profileOwnerEmail = this.normalize(this.profile?.ownerEmail);
    const profileOwnerName = this.normalize(this.profile?.ownerName);
    const userEmail = this.normalize(currentUser?.email);
    const userName = this.normalize(currentUser?.name);

    if (profileOwnerEmail && userEmail && profileOwnerEmail === userEmail) {
      return true;
    }

    if (profileOwnerName && userName && profileOwnerName === userName) {
      return true;
    }

    return false;
  }

  get canDeleteStoreAsAdmin(): boolean {
    return this.authService.currentUser()?.role === 'admin_market';
  }

  isImageSource(image?: string): boolean {
    if (!image) return false;
    const src = String(image).toLowerCase();
    return src.startsWith('http://') || src.startsWith('https://') || src.startsWith('data:image/');
  }

  goToMarketplace(): void {
    this.router.navigate(['/marketplace']);
  }

  openAddProductModalForOwner(): void {
    if (!this.isOwner) {
      return;
    }

    this.router.navigate(['/store-showcase'], { queryParams: { openAddProduct: '1' } });
  }

  openEditStoreForOwner(): void {
    if (!this.isOwner) {
      return;
    }

    this.router.navigate(['/create-store-step-2'], { queryParams: { editStore: '1' } });
  }

  onProductClick(product: Product): void {
    if (this.isOwner) {
      // If the user is the owner, redirect them to the showcase page to edit this product
      this.router.navigate(['/store-showcase'], { queryParams: { editProduct: product.id } });
    } else {
      // Otherwise, redirect to the standard product detail page
      this.router.navigate(['/product', product.id]);
    }
  }


  deleteStoreAsAdmin(): void {
    if (!this.canDeleteStoreAsAdmin) {
      return;
    }

    const storeDisplayName = this.profile?.name || this.storeName || 'cette boutique';
    const confirmed = window.confirm(`Supprimer la boutique "${storeDisplayName}" et tous ses articles ?`);
    if (!confirmed) {
      return;
    }

    const productsToDelete = [...this.storeProducts];
    productsToDelete.forEach((product) => {
      if (product?.id) {
        this.productService.deleteProduct(product.id);
      }
    });

    this.deleteStoreProfilesLocally();

    const backendStoreId = this.resolveBackendStoreId(productsToDelete);
    if (!backendStoreId) {
      this.router.navigate(['/marketplace']);
      return;
    }

    this.marketplaceApi.stores.delete(backendStoreId).subscribe({
      next: () => {
        this.router.navigate(['/marketplace']);
      },
      error: () => {
        // Keep local deletion even if backend delete fails to avoid blocking admin workflow.
        this.router.navigate(['/marketplace']);
      }
    });
  }

  rateStore(stars: number): void {
    const storeKey = this.normalize(this.profile?.name || this.storeName);
    if (!storeKey) {
      return;
    }

    const safeRating = Math.max(1, Math.min(5, Math.round(stars)));
    const statsMap = this.readStoreRatingStats();
    const visitorMap = this.readVisitorStoreRatings();

    const currentStats = statsMap[storeKey] ?? { total: 0, count: 0 };
    const previousVisitorRating = Number(visitorMap[storeKey] || 0);

    let total = Number(currentStats.total || 0);
    let count = Number(currentStats.count || 0);

    if (previousVisitorRating > 0) {
      total = total - previousVisitorRating + safeRating;
      count = Math.max(1, count || 1);
    } else {
      total += safeRating;
      count += 1;
    }

    statsMap[storeKey] = { total, count };
    visitorMap[storeKey] = safeRating;

    this.writeStoreRatingStats(statsMap);
    this.writeVisitorStoreRatings(visitorMap);

    this.storeReviewCount = count;
    this.storeAverageRating = count > 0 ? Number((total / count).toFixed(1)) : 0;
    this.visitorStoreRating = safeRating;
  }

  rateProductFromStore(productId: number, stars: number): void {
    this.productService.rateProduct(productId, stars);
    this.loadStoreProducts();
  }

  getVisitorProductRating(productId: number): number {
    return this.productService.getVisitorProductRating(productId);
  }

  getStoreStars(): number[] {
    return [1, 2, 3, 4, 5];
  }

  private findProductsForStore(): Product[] {
    const allProducts = this.productService.allProducts();
    const routeKey = this.normalize(this.storeName);
    const profileStoreKey = this.normalize(this.profile?.name);
    const profileOwnerKey = this.normalize(this.profile?.ownerName);
    const profileOwnerEmailKey = this.normalize(this.profile?.ownerEmail);

    return allProducts.filter((product) => {
      const sellerKey = this.normalize(product.seller);
      return sellerKey === routeKey
        || sellerKey === profileStoreKey
        || sellerKey === profileOwnerKey
        || sellerKey === profileOwnerEmailKey;
    });
  }

  private normalize(value: string | undefined | null): string {
    return String(value || '').trim().toLowerCase();
  }

  private loadStoreRatings(): void {
    const storeKey = this.normalize(this.profile?.name || this.storeName);
    if (!storeKey) {
      this.storeAverageRating = 0;
      this.storeReviewCount = 0;
      this.visitorStoreRating = 0;
      return;
    }

    const stats = this.readStoreRatingStats()[storeKey] ?? { total: 0, count: 0 };
    const visitor = this.readVisitorStoreRatings()[storeKey] ?? 0;

    this.storeReviewCount = Number(stats.count || 0);
    this.storeAverageRating = this.storeReviewCount > 0
      ? Number((Number(stats.total || 0) / this.storeReviewCount).toFixed(1))
      : 0;
    this.visitorStoreRating = Number(visitor || 0);
  }

  private readStoreRatingStats(): Record<string, { total: number; count: number }> {
    try {
      const raw = localStorage.getItem(StorePublicComponent.STORE_RATING_STATS_KEY);
      if (!raw) {
        return {};
      }
      const parsed = JSON.parse(raw);
      if (!parsed || typeof parsed !== 'object') {
        return {};
      }
      return parsed as Record<string, { total: number; count: number }>;
    } catch {
      return {};
    }
  }

  private writeStoreRatingStats(stats: Record<string, { total: number; count: number }>): void {
    try {
      localStorage.setItem(StorePublicComponent.STORE_RATING_STATS_KEY, JSON.stringify(stats));
    } catch {
      // Ignore storage failures.
    }
  }

  private readVisitorStoreRatings(): Record<string, number> {
    try {
      const raw = localStorage.getItem(StorePublicComponent.STORE_VISITOR_RATINGS_KEY);
      if (!raw) {
        return {};
      }
      const parsed = JSON.parse(raw);
      if (!parsed || typeof parsed !== 'object') {
        return {};
      }
      return parsed as Record<string, number>;
    } catch {
      return {};
    }
  }

  private writeVisitorStoreRatings(ratings: Record<string, number>): void {
    try {
      localStorage.setItem(StorePublicComponent.STORE_VISITOR_RATINGS_KEY, JSON.stringify(ratings));
    } catch {
      // Ignore storage failures.
    }
  }

  private static readonly SYSTEM_FONTS = new Set([
    'arial', 'helvetica', 'times new roman', 'courier new', 'georgia',
    'verdana', 'trebuchet ms', 'palatino', 'garamond', 'bookman',
    'comic sans ms', 'impact', 'monotype corsiva', 'lucida console',
    'lucida sans unicode', 'ms serif', 'ms sans serif', 'symbol',
    'wingdings', 'cambria', 'calibri', 'consolas', 'candara',
  ]);

  private ensureGoogleFontLoaded(fontName: string): void {
    if (!fontName) {
      return;
    }

    const cleanFamily = fontName.split(',')[0].replace(/["'/]/g, '').trim();
    if (!cleanFamily) {
      return;
    }

    if (StorePublicComponent.SYSTEM_FONTS.has(cleanFamily.toLowerCase())) {
      return;
    }

    const linkId = `store-public-font-${cleanFamily.toLowerCase().replace(/\s+/g, '-')}`;
    if (document.getElementById(linkId)) {
      return;
    }

    const link = document.createElement('link');
    link.id = linkId;
    link.rel = 'stylesheet';
    link.href = `https://fonts.googleapis.com/css2?family=${encodeURIComponent(cleanFamily).replace(/%20/g, '+')}&display=swap`;
    document.head.appendChild(link);
  }

  private resolveBackendStoreId(products: Product[]): number | null {
    const firstStoreId = products.find((product) => Number(product.storeId) > 0)?.storeId;
    return Number(firstStoreId) > 0 ? Number(firstStoreId) : null;
  }

  private deleteStoreProfilesLocally(): void {
    deletePublicStoreProfile(this.storeName);

    try {
      const rawCurrent = localStorage.getItem(CREATED_STORE_PROFILE_KEY);
      if (!rawCurrent) {
        return;
      }

      const parsedCurrent = JSON.parse(rawCurrent) as { name?: string };
      const currentName = this.normalize(parsedCurrent?.name);
      const targetName = this.normalize(this.storeName);
      if (currentName && targetName && currentName === targetName) {
        localStorage.removeItem(CREATED_STORE_PROFILE_KEY);
      }
    } catch {
      // Ignore local storage parsing errors.
    }
  }

  private resolveOwnedStore(): void {
    if (!this.authService.isLoggedIn()) {
      this.ownedStoreName = '';
      return;
    }

    this.marketplaceApi.getMyStore().subscribe({
      next: (store) => {
        this.ownedStoreName = String(store?.name || '').trim();
      },
      error: () => {
        this.ownedStoreName = '';
      }
    });
  }

  private resolveOwnedStoreFromLocalProfile(): void {
    try {
      const raw = localStorage.getItem(CREATED_STORE_PROFILE_KEY);
      if (!raw) {
        this.localOwnedStoreName = '';
        return;
      }

      const parsed = JSON.parse(raw) as { name?: string };
      this.localOwnedStoreName = String(parsed?.name || '').trim();
    } catch {
      this.localOwnedStoreName = '';
    }
  }
}
