import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

import { ApiConfigService, MarketplaceApiService, ProductResource, ProductWritePayload, ReviewResource, CategoryResource } from './api';
import { AuthService } from './auth.service';
import { ProductVariant, buildProductDescriptionWithMeta, parseCsvImages, parseProductDescription, parseVariantRows } from './product-meta.util';

export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  originalPrice?: number;
  discountedPrice?: number;
  discountPercent?: number;
  promoEndAt?: string;
  remainingSeconds?: number;
  isPromotionActive?: boolean;
  promotionStatus?: string;
  currency: string;
  image: string;
  images?: string[];
  variants?: ProductVariant[];
  seller: string;
  storeId?: number;
  ownerId?: number;
  categoryId?: number;
  rating: number;
  reviews: number;
  stock: number;
  ///////////////////////////////hethi masta//////////
  category: 'electronics' | 'books' | 'clothing' | 'services' | 'other';
  categoryLabel?: string;
  dimensionsLabel?: string;
  weight?: number;
  createdAt: Date;
  updatedAt: Date;
}

type ProductCategory = Product['category'];

export interface ProductFilters {
  category?: string;
  priceRange: [number, number];
  minRating: number;
  searchQuery: string;
  sortBy: 'popular' | 'price-asc' | 'price-desc' | 'newest' | 'rating';
}

export interface ProductCategoryOption {
  id: number;
  name: string;
  description: string;
}

export interface ProductCreationFormValue {
  name: string;
  phone?: string;
  price: number;
  stock: number;
  description: string;
  imageUrl: string;
  imagesCsv: string;
  categoryId: number;
  sku: string;
  width: number;
  height: number;
  depth: number;
  dimensionUnit: 'cm' | 'mm' | 'm';
  weight: number;
  variantsRaw: string;
}

export interface ProductCreationContext {
  storeId: number;
  categoryName?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private static readonly PENDING_PRODUCTS_STORAGE_KEY = 'marketplace_pending_products';
  private static readonly PRODUCT_RATING_STATS_STORAGE_KEY = 'marketplace_product_rating_stats';
  private static readonly PRODUCT_VISITOR_RATINGS_STORAGE_KEY = 'marketplace_product_visitor_ratings';
  private static readonly PRODUCT_STOCK_OVERRIDES_STORAGE_KEY = 'marketplace_product_stock_overrides';
  private readonly marketplaceApi = inject(MarketplaceApiService);
  private readonly apiConfig = inject(ApiConfigService);
  private readonly authService = inject(AuthService);

  // Reactive state
  private products = signal<Product[]>([]);
  private filters = signal<ProductFilters>({
    category: undefined,
    priceRange: [0, 2000],
    minRating: 0,
    searchQuery: '',
    sortBy: 'popular'
  });

  private currentPage = signal(1);
  private itemsPerPage = signal(9);

  // Computed values
  filteredProducts = computed(() => {
    let result = [...this.products()];
    const f = this.filters();

    // Filter by search query
    if (f.searchQuery) {
      const query = f.searchQuery.toLowerCase();
      result = result.filter(p =>
        p.name.toLowerCase().includes(query) ||
        p.description.toLowerCase().includes(query) ||
        p.seller.toLowerCase().includes(query)
      );
    }

    // Filter by category
    if (f.category && f.category !== 'all') {
      result = result.filter(p => p.category === f.category);
    }

    // Filter by price range
    result = result.filter(p => {
      const currentPrice = this.getCurrentPrice(p);
      return currentPrice >= f.priceRange[0] && currentPrice <= f.priceRange[1];
    });

    // Filter by rating
    result = result.filter(p => p.rating >= f.minRating);

    // Sort
    switch (f.sortBy) {
      case 'price-asc':
        result.sort((a, b) => this.getCurrentPrice(a) - this.getCurrentPrice(b));
        break;
      case 'price-desc':
        result.sort((a, b) => this.getCurrentPrice(b) - this.getCurrentPrice(a));
        break;
      case 'newest':
        result.sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime());
        break;
      case 'rating':
        result.sort((a, b) => b.rating - a.rating);
        break;
      case 'popular':
      default:
        result.sort((a, b) => b.reviews - a.reviews);
    }

    return result;
  });

  totalResults = computed(() => this.filteredProducts().length);

  paginatedProducts = computed(() => {
    const filtered = this.filteredProducts();
    const page = this.currentPage();
    const perPage = this.itemsPerPage();
    const start = (page - 1) * perPage;
    return filtered.slice(start, start + perPage);
  });

  totalPages = computed(() => {
    return Math.ceil(this.totalResults() / this.itemsPerPage());
  });

  // Expose currentPage as computed
  currentPageNumber = computed(() => this.currentPage());

  // Get all raw products without any filtering (for use cases like store pages)
  allProducts = computed(() => [...this.products()]);

  categories = computed(() => {
    const cats = new Set(this.products().map(p => p.category));
    return Array.from(cats);
  });

  priceRange = computed(() => {
    const prices = this.products().map(p => this.getCurrentPrice(p));

    if (prices.length === 0) {
      return { min: 0, max: 2000 };
    }

    return {
      min: Math.min(...prices),
      max: Math.max(...prices)
    };
  });

  constructor() {
    this.loadProductsFromBackend();
  }

  consumeStockFromPurchase(items: Array<{ productId: number; quantity: number }>): void {
    if (!Array.isArray(items) || items.length === 0) {
      return;
    }

    const overrides = this.readProductStockOverrides();
    const stockById = new Map<number, number>();

    for (const item of items) {
      const productId = Number(item?.productId);
      const quantity = Math.max(0, Number(item?.quantity || 0));

      if (!productId || quantity <= 0) {
        continue;
      }

      const currentProduct = this.getProductById(productId);
      const currentStock = Number(stockById.get(productId) ?? currentProduct?.stock ?? overrides[String(productId)] ?? 0);
      const nextStock = Math.max(0, currentStock - quantity);

      stockById.set(productId, nextStock);
      overrides[String(productId)] = nextStock;
    }

    if (stockById.size === 0) {
      return;
    }

    this.products.update((current) => current.map((product) => {
      const nextStock = stockById.get(product.id);
      if (typeof nextStock !== 'number') {
        return product;
      }

      return {
        ...product,
        stock: nextStock,
        updatedAt: new Date()
      };
    }));

    this.writeProductStockOverrides(overrides);

    stockById.forEach((stock, productId) => {
      this.marketplaceApi.patchProductDirect(productId, { stock }).subscribe({
        next: () => {
          // Backend accepted stock update.
        },
        error: () => {
          // Keep local stock sync even if backend endpoint is unavailable.
        }
      });
    });
  }

  refresh(): void {
    this.loadProductsFromBackend();
  }

  loadProductsByStoreId(storeId: number): Observable<Product[]> {
    const normalizedStoreId = Number(storeId || 0);
    if (normalizedStoreId <= 0) {
      return of([]);
    }

    return this.marketplaceApi.listProductsByStoreId(normalizedStoreId).pipe(
      map((products) => this.applyLocalRatings(products.map((product) => this.mapProductResource(product))))
    );
  }

  loadProductCategories(): Observable<ProductCategoryOption[]> {
    return this.marketplaceApi.categories.list().pipe(
      map((categories) => categories
        .filter((item): item is CategoryResource => Number(item?.id || 0) > 0)
        .map((item) => ({
          id: Number(item.id),
          name: String(item.name || '').trim(),
          description: String(item.description || '').trim()
        }))
        .sort((a, b) => a.name.localeCompare(b.name))
      ),
      catchError(() => of([]))
    );
  }

  resolveMyStoreId(): Observable<number> {
    return this.marketplaceApi.getMyStore().pipe(
      map((store) => Number(store?.id || 0)),
      catchError(() => of(0))
    );
  }

  createMarketplaceProduct(formValue: ProductCreationFormValue, context: ProductCreationContext): Observable<ProductResource> {
    if (Number(context.storeId || 0) <= 0) {
      return throwError(() => new Error('A valid store is required before creating products.'));
    }

    if (Number(formValue.categoryId || 0) <= 0) {
      return throwError(() => new Error('Please select a valid category before creating the product.'));
    }

    const payload = this.mapFormValueToWritePayload(formValue, context);
    const image = String(formValue.imageUrl || '').trim();

    const request$ = image.startsWith('data:image/')
      ? this.marketplaceApi.createProductMultipart(
          (() => {
            const fd = new FormData();
            fd.append('name', payload.name || '');
            fd.append('description', payload.description || '');
            fd.append('price', String(payload.price));
            fd.append('stock', String(payload.stock));
            fd.append('storeId', String(context.storeId));
            if (formValue.categoryId != null) {
              fd.append('categoryId', String(formValue.categoryId));
            }
            if (payload.dimensionsLabel) {
              fd.append('dimensionsLabel', payload.dimensionsLabel);
            }
            if (payload.weight !== undefined) {
              fd.append('weight', String(payload.weight));
            }
            fd.append('image', this.dataUriToBlob(image), 'product.png');
            return fd;
          })()
        )
      : this.marketplaceApi.products.create({ ...payload, imageUrl: image });

    return request$.pipe(
      tap(() => this.loadProductsFromBackend()),
      catchError((error) => throwError(() => new Error(this.resolveApiErrorMessage(error, 'Failed to create product.'))))
    );
  }

  resolveApiErrorMessage(error: unknown, fallback: string): string {
    const err = error as {
      error?: { message?: string; error?: string };
      message?: string;
      status?: number;
      statusText?: string;
    };

    const backendMessage = String(err?.error?.message || err?.error?.error || err?.message || '').trim();
    if (backendMessage) {
      return backendMessage;
    }

    if (Number(err?.status || 0) > 0) {
      return `${fallback} (HTTP ${err?.status}${err?.statusText ? ` ${err.statusText}` : ''})`;
    }

    return fallback;
  }

  // Public methods
  setFilters(filters: Partial<ProductFilters>): void {
    this.filters.update(current => ({
      ...current,
      ...filters
    }));
    this.currentPage.set(1); // Reset to first page
  }

  setSearchQuery(query: string): void {
    this.setFilters({ searchQuery: query });
  }

  setCategory(category: string | undefined): void {
    this.setFilters({ category });
  }

  setPriceRange(range: [number, number]): void {
    this.setFilters({ priceRange: range });
  }

  setMinRating(rating: number): void {
    this.setFilters({ minRating: rating });
  }

  setSortBy(sortBy: ProductFilters['sortBy']): void {
    this.setFilters({ sortBy });
  }

  setPage(page: number): void {
    if (page >= 1 && page <= this.totalPages()) {
      this.currentPage.set(page);
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages()) {
      this.currentPage.update(p => p + 1);
    }
  }

  previousPage(): void {
    if (this.currentPage() > 1) {
      this.currentPage.update(p => p - 1);
    }
  }

  getProductById(id: number): Product | undefined {
    return this.products().find(p => p.id === id);
  }

  rateProduct(productId: number, rating: number): void {
    const safeRating = Math.max(1, Math.min(5, Math.round(rating)));
    const product = this.getProductById(productId);
    if (!product) {
      return;
    }

    const statsMap = this.readProductRatingStats();
    const visitorRatings = this.readVisitorProductRatings();

    const key = String(productId);
    const previousVisitorRating = Number(visitorRatings[key] || 0);
    const currentStats = statsMap[key];

    const baseCount = Number(currentStats?.count ?? product.reviews ?? 0);
    const baseTotal = Number(currentStats?.total ?? ((product.rating || 0) * (product.reviews || 0)));

    let nextCount = baseCount;
    let nextTotal = baseTotal;

    if (previousVisitorRating > 0) {
      nextTotal = baseTotal - previousVisitorRating + safeRating;
      nextCount = Math.max(1, baseCount || 1);
    } else {
      nextTotal = baseTotal + safeRating;
      nextCount = baseCount + 1;
    }

    statsMap[key] = { total: nextTotal, count: nextCount };
    visitorRatings[key] = safeRating;

    this.writeProductRatingStats(statsMap);
    this.writeVisitorProductRatings(visitorRatings);

    const nextAvg = nextCount > 0 ? Number((nextTotal / nextCount).toFixed(1)) : 0;
    this.products.update((current) => current.map((item) => (
      item.id === productId
        ? { ...item, rating: nextAvg, reviews: nextCount, updatedAt: new Date() }
        : item
    )));
  }

  getVisitorProductRating(productId: number): number {
    const ratings = this.readVisitorProductRatings();
    return Number(ratings[String(productId)] || 0);
  }

  addProduct(product: Omit<Product, 'id' | 'createdAt' | 'updatedAt'>): Product {
    // Validation stricte des champs obligatoires
    const name = String(product.name || '').trim();
    const description = String(product.description || '').trim();
    const price = Number(product.price);
    const stock = Number(product.stock);
    const image = String(product.image || '').trim();
    const storeId = product.storeId;
    const categoryId = product.categoryId;

    // Vérification imageUrl (http(s) ou data:image/*)
    const isValidImageUrl =
      image.startsWith('http://') ||
      image.startsWith('https://') ||
      image.startsWith('data:image/');

    let errorMsg = '';
    if (!name || name.length > 200) {
      errorMsg = 'Nom du produit obligatoire (max 200 caractères).';
    } else if (description.length > 1000) {
      errorMsg = 'Description trop longue (max 1000 caractères).';
    } else if (isNaN(price) || price <= 0) {
      errorMsg = 'Prix obligatoire (> 0).';
    } else if (isNaN(stock) || stock < 0) {
      errorMsg = 'Stock obligatoire (>= 0).';
    } else if (!isValidImageUrl) {
      errorMsg = 'Image obligatoire (URL http(s) ou data:image/*).';
    } else if (!storeId) {
      errorMsg = 'storeId obligatoire.';
    } else if (!categoryId) {
      errorMsg = 'categoryId obligatoire.';
    }

    if (errorMsg) {
      // eslint-disable-next-line no-console
      console.error('❌ Produit invalide, payload non envoyé:', { name, description, price, stock, image, storeId, categoryId });
      alert(errorMsg);
      throw new Error(errorMsg);
    }

    const optimisticProduct: Product = {
      ...product,
      id: Math.max(0, ...this.products().map(p => p.id)) + 1,
      createdAt: new Date(),
      updatedAt: new Date()
    };

    this.products.update(current => [...current, optimisticProduct]);
    this.persistPendingProduct(optimisticProduct);

    // Use multipart upload if image is a data URI, otherwise use JSON
    if (image.startsWith('data:image/')) {
      const formData = new FormData();
      formData.append('name', name || '');
      formData.append('description', description || '');
      formData.append('price', String(price));
      formData.append('stock', String(stock));
      if (storeId) {
        formData.append('storeId', String(storeId));
      }
      if (categoryId) {
        formData.append('categoryId', String(categoryId));
      }
      if (product.dimensionsLabel) {
        formData.append('dimensionsLabel', product.dimensionsLabel);
      }
      if (product.weight) {
        formData.append('weight', String(product.weight));
      }

      // Convert data URI to Blob
      const blob = this.dataUriToBlob(image);
      formData.append('image', blob, 'product-image.png');

      // eslint-disable-next-line no-console
      console.log('✅ Sending multipart upload to /upload endpoint');

      this.marketplaceApi.createProductMultipart(formData).subscribe({
        next: () => this.loadProductsFromBackend(),
        error: () => this.loadProductsFromBackend()
      });
    } else {
      const payload = this.mapProductToWritePayload(product);
      // eslint-disable-next-line no-console
      console.log('✅ Payload envoyé au backend:', payload);

      this.marketplaceApi.products.create(payload).subscribe({
        next: () => this.loadProductsFromBackend(),
        error: () => this.loadProductsFromBackend()
      });
    }

    return optimisticProduct;
  }

  private dataUriToBlob(dataUri: string): Blob {
    const parts = dataUri.split(',');
    const mimeMatch = parts[0].match(/:(.*?);/);
    const mime = mimeMatch ? mimeMatch[1] : 'image/png';
    const byteString = atob(parts[1]);
    const arrayBuffer = new ArrayBuffer(byteString.length);
    const intArray = new Uint8Array(arrayBuffer);
    for (let i = 0; i < byteString.length; i++) {
      intArray[i] = byteString.charCodeAt(i);
    }
    return new Blob([intArray], { type: mime });
  }

  updateProduct(id: number, updates: Partial<Omit<Product, 'id' | 'createdAt'>>): void {
    const currentProduct = this.getProductById(id);
    if (!currentProduct) {
      return;
    }

    this.products.update(current =>
      current.map(p =>
        p.id === id
          ? { ...p, ...updates, updatedAt: new Date() }
          : p
      )
    );

      const merged = { ...currentProduct, ...updates };
      const payload = this.mapProductToWritePayload(merged);

      this.marketplaceApi.products.update(id, payload).subscribe({
        next: () => this.loadProductsFromBackend(),
        error: () => this.loadProductsFromBackend()
      });
  }

  deleteProduct(id: number): void {
    this.products.update(current => current.filter(p => p.id !== id));
      this.marketplaceApi.products.delete(id).subscribe({
        error: () => this.loadProductsFromBackend()
      });
  }

  resetFilters(): void {
    this.filters.set({
      category: undefined,
      priceRange: [0, 2000],
      minRating: 0,
      searchQuery: '',
      sortBy: 'popular'
    });
    this.currentPage.set(1);
  }

  private loadProductsFromBackend(): void {
    this.marketplaceApi.listMarketProducts().pipe(
      catchError(() => this.marketplaceApi.listMarketProducts()) // Fallback to same for now
    ).subscribe({
      next: (products) => {
        const mappedProducts = products.map((product) => this.mapProductResource(product));
        const mergedProducts = this.mergeWithPendingProducts(mappedProducts);
        // We removed applyStockOverrides here because it was causing stale local data to overwrite real backend stock
        this.products.set(this.applyLocalRatings(mergedProducts));
      },
      error: () => {
        this.products.set(this.applyLocalRatings(this.readPendingProducts()));
      }
    });
  }

  private applyStockOverrides(products: Product[]): Product[] {
    const overrides = this.readProductStockOverrides();
    if (!products.length || !Object.keys(overrides).length) {
      return products;
    }

    return products.map((product) => {
      const stock = overrides[String(product.id)];
      if (typeof stock !== 'number') {
        return product;
      }

      return {
        ...product,
        stock: Math.max(0, Number(stock))
      };
    });
  }

  private applyLocalRatings(products: Product[]): Product[] {
    const statsMap = this.readProductRatingStats();
    if (!products.length || !Object.keys(statsMap).length) {
      return products;
    }

    return products.map((product) => {
      const stats = statsMap[String(product.id)];
      if (!stats || Number(stats.count || 0) <= 0) {
        return product;
      }

      const count = Number(stats.count);
      const avg = Number((Number(stats.total) / count).toFixed(1));
      return {
        ...product,
        rating: avg,
        reviews: count
      };
    });
  }

  private mergeWithPendingProducts(serverProducts: Product[]): Product[] {
    const pending = this.readPendingProducts();
    if (pending.length === 0) {
      return serverProducts;
    }

    const serverKeys = new Set(serverProducts.map((product) => this.buildProductKey(product)));
    const stillPending = pending.filter((product) => !serverKeys.has(this.buildProductKey(product)));

    this.writePendingProducts(stillPending);

    const combined = [...serverProducts];
    stillPending.forEach((pendingProduct) => {
      const alreadyExists = combined.some((item) => item.id === pendingProduct.id || this.buildProductKey(item) === this.buildProductKey(pendingProduct));
      if (!alreadyExists) {
        combined.unshift(pendingProduct);
      }
    });

    return combined;
  }

  private buildProductKey(product: Product): string {
    return [
      String(product.name || '').trim().toLowerCase(),
      Number(product.price || 0).toFixed(2),
      String(product.seller || '').trim().toLowerCase(),
      Number(product.stock || 0)
    ].join('|');
  }

  private persistPendingProduct(product: Product): void {
    const currentPending = this.readPendingProducts();
    const key = this.buildProductKey(product);
    const nextPending = [product, ...currentPending.filter((item) => this.buildProductKey(item) !== key)];
    this.writePendingProducts(nextPending);
  }

  private readPendingProducts(): Product[] {
    try {
      const raw = localStorage.getItem(ProductService.PENDING_PRODUCTS_STORAGE_KEY);
      if (!raw) {
        return [];
      }

      const parsed = JSON.parse(raw);
      if (!Array.isArray(parsed)) {
        return [];
      }

      return parsed
        .filter((item): item is Product => !!item && typeof item === 'object' && typeof item.name === 'string')
        .map((item) => ({
          ...item,
          createdAt: new Date(item.createdAt),
          updatedAt: new Date(item.updatedAt)
        }));
    } catch {
      return [];
    }
  }

  private writePendingProducts(products: Product[]): void {
    try {
      localStorage.setItem(ProductService.PENDING_PRODUCTS_STORAGE_KEY, JSON.stringify(products));
    } catch {
      // Ignore storage failures.
    }
  }

  private readProductRatingStats(): Record<string, { total: number; count: number }> {
    try {
      const raw = localStorage.getItem(ProductService.PRODUCT_RATING_STATS_STORAGE_KEY);
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

  private writeProductRatingStats(stats: Record<string, { total: number; count: number }>): void {
    try {
      localStorage.setItem(ProductService.PRODUCT_RATING_STATS_STORAGE_KEY, JSON.stringify(stats));
    } catch {
      // Ignore storage failures.
    }
  }

  private readVisitorProductRatings(): Record<string, number> {
    try {
      const raw = localStorage.getItem(ProductService.PRODUCT_VISITOR_RATINGS_STORAGE_KEY);
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

  private writeVisitorProductRatings(ratings: Record<string, number>): void {
    try {
      localStorage.setItem(ProductService.PRODUCT_VISITOR_RATINGS_STORAGE_KEY, JSON.stringify(ratings));
    } catch {
      // Ignore storage failures.
    }
  }

  private readProductStockOverrides(): Record<string, number> {
    try {
      const raw = localStorage.getItem(ProductService.PRODUCT_STOCK_OVERRIDES_STORAGE_KEY);
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

  private writeProductStockOverrides(overrides: Record<string, number>): void {
    try {
      localStorage.setItem(ProductService.PRODUCT_STOCK_OVERRIDES_STORAGE_KEY, JSON.stringify(overrides));
    } catch {
      // Ignore storage failures.
    }
  }

  private buildReviewMap(reviews: ReviewResource[]): Map<number, { count: number; avg: number }> {
    const grouped = new Map<number, { count: number; total: number }>();

    for (const review of reviews) {
      const productId = review.product?.id;
      if (!productId) {
        continue;
      }

      const current = grouped.get(productId) ?? { count: 0, total: 0 };
      grouped.set(productId, {
        count: current.count + 1,
        total: current.total + review.rating
      });
    }

    const normalized = new Map<number, { count: number; avg: number }>();
    grouped.forEach((value, key) => {
      normalized.set(key, {
        count: value.count,
        avg: value.count > 0 ? Number((value.total / value.count).toFixed(1)) : 0
      });
    });

    return normalized;
  }

  private mapProductResource(product: ProductResource, reviewStats?: { count: number; avg: number }): Product {
    const parsedMeta = parseProductDescription(product.description || '');
    const categoryRaw = parsedMeta.category || product.category?.name || product.name;
    const normalizedPrice = this.toNumber(product.price);
    const normalizedOriginalPrice = this.toNumber((product as ProductResource & { originalPrice?: number | null }).originalPrice);
    const normalizedDiscountedPrice = this.toNumber((product as ProductResource & { discountedPrice?: number | null }).discountedPrice);
    const normalizedDiscountPercent = this.toNumber((product as ProductResource & { discountPercent?: number | null }).discountPercent);
    const normalizedRemaining = this.toNumber((product as ProductResource & { remainingSeconds?: number | null }).remainingSeconds);
    const promoEndAt = (product as ProductResource & { promoEndAt?: string | null }).promoEndAt ?? undefined;
    const promotionStatus = (product as ProductResource & { promotionStatus?: string | null }).promotionStatus ?? undefined;
    const isPromotionActive = Boolean((product as ProductResource & { isPromotionActive?: boolean }).isPromotionActive)
      || (normalizedRemaining > 0 && normalizedDiscountPercent > 0);
    const effectivePrice = isPromotionActive && normalizedDiscountedPrice > 0 ? normalizedDiscountedPrice : normalizedPrice;

    return {
      id: product.id,
      name: product.name,
      description: parsedMeta.description || product.description,
      price: effectivePrice,
      originalPrice: normalizedOriginalPrice > 0 ? normalizedOriginalPrice : normalizedPrice,
      discountedPrice: normalizedDiscountedPrice > 0 ? normalizedDiscountedPrice : undefined,
      discountPercent: normalizedDiscountPercent > 0 ? normalizedDiscountPercent : undefined,
      promoEndAt,
      remainingSeconds: normalizedRemaining > 0 ? normalizedRemaining : undefined,
      isPromotionActive,
      promotionStatus,
      currency: 'DT',
      image: this.resolveProductImage(product, parsedMeta.images),
      images: parsedMeta.images,
      variants: parsedMeta.variants,
      seller: (product as any).storeName || product.store?.name || 'ESPRIT Store',
      storeId: (product as any).storeId || product.store?.id,
      ownerId: product.store?.owner?.id,
      categoryId: (product as any).categoryId || product.category?.id,
      rating: reviewStats?.avg ?? 0,
      reviews: reviewStats?.count ?? 0,
      stock: product.stock,
      category: this.toUiCategory(categoryRaw),
      categoryLabel: this.toCategoryLabel(categoryRaw),
      dimensionsLabel: (product.dimensionsLabel || `${parsedMeta.width} x ${parsedMeta.height} x ${parsedMeta.depth} ${parsedMeta.dimensionUnit}`).trim() || 'N/A',
      weight: product.weight || parsedMeta.weight || 0,
      createdAt: new Date(),
      updatedAt: new Date()
    };
  }

  private getCurrentPrice(product: Product): number {
    if (product.isPromotionActive && Number(product.discountedPrice || 0) > 0) {
      return Number(product.discountedPrice);
    }
    return Number(product.price);
  }

  private toNumber(value: unknown): number {
    const n = Number(value);
    return Number.isFinite(n) ? n : 0;
  }

  private resolveProductImage(product: ProductResource, gallery: string[]): string {
    const fromGallery = gallery[0];
    if (fromGallery) {
      if (fromGallery.startsWith('/uploads/')) return this.apiConfig.buildAssetUrl(fromGallery);
      return fromGallery;
    }

    if (product.imageUrl?.startsWith('http')) {
      return product.imageUrl;
    }

    if (product.imageUrl) {
      if (product.imageUrl.startsWith('/uploads/')) return this.apiConfig.buildAssetUrl(product.imageUrl);
      return product.imageUrl;
    }

    const category = this.toUiCategory(product.category?.name ?? product.name);
    switch (category) {
      case 'electronics':
        return '💻';
      case 'books':
        return '📚';
      case 'clothing':
        return '👕';
      case 'services':
        return '🛠️';
      default:
        return '🛍️';
    }
  }

  private toUiCategory(raw: string): ProductCategory {
    const normalized = raw.toLowerCase();
    if (normalized.includes('accessoire') || normalized.includes('bag') || normalized.includes('sac')) {
      return 'clothing';
    }
    if (normalized.includes('sport')) {
      return 'clothing';
    }
    if (normalized.includes('pastry') || normalized.includes('gateau') || normalized.includes('patis')) {
      return 'other';
    }
    if (normalized.includes('art') || normalized.includes('tableau')) {
      return 'other';
    }
    if (normalized.includes('elect')) {
      return 'electronics';
    }
    if (normalized.includes('book') || normalized.includes('livre')) {
      return 'books';
    }
    if (normalized.includes('cloth') || normalized.includes('fashion') || normalized.includes('vêt')) {
      return 'clothing';
    }
    if (normalized.includes('service') || normalized.includes('cours') || normalized.includes('tutor')) {
      return 'services';
    }
    return 'other';
  }

  private toCategoryLabel(raw: string): string {
    const normalized = raw.toLowerCase();
    if (normalized.includes('clothing_brand') || (normalized.includes('marque') && normalized.includes('vet'))) {
      return 'Marque de vetements';
    }
    if (normalized.includes('accessories') || normalized.includes('accessoire')) {
      return 'Accessoires';
    }
    if (normalized.includes('art') || normalized.includes('tableau')) {
      return 'Art';
    }
    if (normalized.includes('bag') || normalized.includes('sac')) {
      return 'Sacs';
    }
    if (normalized.includes('pastry') || normalized.includes('patis') || normalized.includes('gateau')) {
      return 'Patisserie';
    }
    if (normalized.includes('sport')) {
      return 'Vetements de sport';
    }
    return 'General';
  }

  private mapProductToWritePayload(product: {
    name: string;
    description: string;
    price: number;
    stock: number;
    image: string;
    storeId?: number;
    categoryId?: number;
    dimensionsLabel?: string;
    weight?: number;
  }): ProductWritePayload {
    const storeId = Number(product.storeId || 0);
    const categoryId = Number(product.categoryId || 0);

    return {
      name: String(product.name || '').trim(),
      description: String(product.description || '').trim(),
      price: Number(product.price || 0),
      stock: Number(product.stock || 0),
      imageUrl: String(product.image || '').trim(),
      dimensionsLabel: product.dimensionsLabel,
      weight: product.weight,
      store: storeId > 0 ? { id: storeId } : undefined,
      category: categoryId > 0 ? { id: categoryId } : undefined
    };
  }

  private mapFormValueToWritePayload(formValue: ProductCreationFormValue, context: ProductCreationContext): ProductWritePayload {
    const isDataImage = String(formValue.imageUrl || '').startsWith('data:image/');
    const images = [isDataImage ? '' : formValue.imageUrl, ...parseCsvImages(formValue.imagesCsv)]
      .map((image) => String(image || '').trim())
      .filter((image) => !!image);

    return {
      name: String(formValue.name || '').trim(),
      description: buildProductDescriptionWithMeta({
        description: String(formValue.description || '').trim(),
        category: String(context.categoryName || '').trim() || `Category-${formValue.categoryId}`,
        sku: String(formValue.sku || '').trim(),
        width: Number(formValue.width || 0),
        height: Number(formValue.height || 0),
        depth: Number(formValue.depth || 0),
        dimensionUnit: formValue.dimensionUnit,
        weight: Number(formValue.weight || 0),
        images,
        variants: parseVariantRows(formValue.variantsRaw)
      }),
      price: Number(formValue.price || 0),
      stock: Number(formValue.stock || 0),
      imageUrl: images[0] || '',
      dimensionsLabel: `${formValue.width} x ${formValue.height} x ${formValue.depth} ${formValue.dimensionUnit}`,
      weight: Number(formValue.weight || 0),
      store: { id: Number(context.storeId) },
      category: { id: Number(formValue.categoryId) }
    };
  }
}
