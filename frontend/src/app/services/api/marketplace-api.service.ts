import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { CrudApiService } from './crud-api.service';
import { ApiConfigService } from './api-config.service';
import {
  CategoryResource,
  CategoryWritePayload,
  OrderLineResource,
  OrderLineWritePayload,
  OrderResource,
  OrderWritePayload,
  ProductResource,
  ProductPromotionPayload,
  ProductWritePayload,
  ReviewResource,
  ReviewWritePayload,
  StoreStatsResource,
  StoreResource,
  StoreWritePayload,
  VisualSearchRequest,
  VisualSearchResponseResource,
  SemanticSearchRequest,
  SemanticSearchResponseResource
} from './models/api-resource.model';
import { Observable, catchError, map, throwError } from 'rxjs';
import { JwtService } from './jwt.service';

@Injectable({ providedIn: 'root' })
export class MarketplaceApiService {
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);
  private readonly jwtService = inject(JwtService);

  /**
   * Public method to post a product with FormData (for file upload)
   */
  postProductWithFormData(formData: FormData): Observable<ProductResource> {
    return this.http.post<ProductResource>(this.apiConfig.buildUrl('/api/marketplace/products/upload'), formData);
  }

  readonly categories = new CrudApiService<CategoryResource, CategoryWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/marketplace/categories')
  );

  readonly orders = new CrudApiService<OrderResource, OrderWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/marketplace/orders')
  );

  readonly orderLines = new CrudApiService<OrderLineResource, OrderLineWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/marketplace/order-lines')
  );

  readonly products = new CrudApiService<ProductResource, ProductWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/marketplace/products')
  );

  readonly reviews = new CrudApiService<ReviewResource, ReviewWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/marketplace/reviews')
  );

  readonly stores = new CrudApiService<StoreResource, StoreWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/marketplace/stores')
  );

  getStoreStats(storeId: number): Observable<StoreStatsResource> {
    return this.http.get<StoreStatsResource>(
      this.apiConfig.buildUrl(`/api/marketplace/stores/${storeId}/stats`)
    );
  }

  getMyStore(): Observable<StoreResource> {
    if (!this.jwtService.getToken()) {
      return throwError(() => new Error('Authentication required'));
    }

    return this.http.get<StoreResource>(this.apiConfig.buildUrl('/api/stores/my')).pipe(
      catchError((error) => {
        if (error?.status === 404 || error?.status === 405) {
          return this.http.get<StoreResource>(this.apiConfig.buildUrl('/api/marketplace/stores/my'));
        }
        return throwError(() => error);
      })
    );
  }

  getMyStores(): Observable<StoreResource[]> {
    const primaryUrl = this.apiConfig.buildUrl('/api/marketplace/stores/my/all');
    return this.http.get<StoreResource[]>(primaryUrl).pipe(
      catchError((error) => {
        if (error?.status === 404 || error?.status === 405) {
          const fallbackUrl = this.apiConfig.buildUrl('/api/marketplace/stores/my');
          return this.http.get<StoreResource | StoreResource[]>(fallbackUrl).pipe(
            map((result: StoreResource | StoreResource[] | null) =>
              Array.isArray(result) ? result : result ? [result] : []
            ),
            catchError((innerError) => {
              if (innerError?.status === 404 || innerError?.status === 405) {
                return this.http.get<StoreResource[]>(this.apiConfig.buildUrl('/api/stores/my/list'));
              }
              return throwError(() => innerError);
            })
          );
        }
        return throwError(() => error);
      })
    );
  }

  createMyStore(payload: Partial<StoreWritePayload>): Observable<StoreResource> {
    return this.http.post<StoreResource>(this.apiConfig.buildUrl('/api/stores/my'), payload);
  }

  getPublicStore(storeId: number): Observable<StoreResource> {
    return this.http.get<StoreResource>(this.apiConfig.buildUrl(`/api/stores/${storeId}`));
  }

  listMarketProducts(): Observable<ProductResource[]> {
    return this.http.get<ProductResource[]>(this.apiConfig.buildUrl('/api/market/products'));
  }

  listProductsByStoreId(storeId: number): Observable<ProductResource[]> {
    return this.http.get<ProductResource[]>(
      this.apiConfig.buildUrl(`/api/marketplace/products?storeId=${storeId}`)
    );
  }

  createOwnerProduct(payload: ProductWritePayload): Observable<ProductResource> {
    return this.http.post<ProductResource>(this.apiConfig.buildUrl('/api/products/my-store'), payload);
  }

  createProductMultipart(formData: FormData): Observable<ProductResource> {
    return this.http.post<ProductResource>(
      this.apiConfig.buildUrl('/api/marketplace/products/upload'),
      formData
    );
  }

  updateOwnerProduct(productId: number, payload: ProductWritePayload): Observable<ProductResource> {
    return this.http.put<ProductResource>(this.apiConfig.buildUrl(`/api/products/${productId}/owner-update`), payload);
  }

  updateProductDirect(productId: number, payload: unknown): Observable<ProductResource> {
    return this.http.put<ProductResource>(this.apiConfig.buildUrl(`/api/marketplace/products/${productId}`), payload);
  }

  patchProductDirect(productId: number, payload: unknown): Observable<ProductResource> {
    return this.http.patch<ProductResource>(this.apiConfig.buildUrl(`/api/marketplace/products/${productId}`), payload);
  }

  deleteOwnerProduct(productId: number): Observable<void> {
    return this.http.delete<void>(this.apiConfig.buildUrl(`/api/products/${productId}/owner-delete`));
  }

  applyProductPromotion(productId: number, payload: ProductPromotionPayload): Observable<ProductResource> {
    return this.http.patch<ProductResource>(this.apiConfig.buildUrl(`/api/products/${productId}/promotion`), payload);
  }

  removeProductPromotion(productId: number): Observable<ProductResource> {
    return this.http.delete<ProductResource>(this.apiConfig.buildUrl(`/api/products/${productId}/promotion`));
  }

  visualSearch(request: VisualSearchRequest): Observable<VisualSearchResponseResource> {
    return this.http.post<VisualSearchResponseResource>(this.apiConfig.buildUrl('/api/marketplace/visual-search'), request);
  }

  semanticSearch(request: SemanticSearchRequest): Observable<SemanticSearchResponseResource> {
    return this.http.post<SemanticSearchResponseResource>(this.apiConfig.buildUrl('/api/marketplace/semantic-search'), request);
  }
}
