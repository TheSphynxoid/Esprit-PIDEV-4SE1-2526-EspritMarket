import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';

import { ApiConfigService } from './api/api-config.service';
import { JwtService } from './api/jwt.service';
import { StoreResource, StoreWritePayload } from './api/models/api-resource.model';

export interface StoreCreatePayload extends StoreWritePayload {
  categories?: string[];
}

@Injectable({ providedIn: 'root' })
export class StoreService {
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);
  private readonly jwtService = inject(JwtService);

  createStore(ownerUserId: number, payload: StoreCreatePayload): Observable<HttpResponse<StoreResource>> {
    const ownerScopedEndpoint = this.apiConfig.buildUrl(`/api/marketplace/stores/create/${ownerUserId}`);
    console.log('🔵 StoreService.createStore: POST', ownerScopedEndpoint, payload);

    return this.http.post<StoreResource>(ownerScopedEndpoint, payload, {
      headers: this.buildAuthHeaders(),
      observe: 'response'
    }).pipe(
      catchError((error) => {
        // Some backend deployments expose /api/stores/my (or /api/marketplace/stores) instead of owner-scoped create.
        if (error?.status === 404 || error?.status === 405) {
          console.warn('⚠️ StoreService.createStore: primary endpoint unavailable, trying fallback endpoints');
          return this.createWithFallbackEndpoints(payload);
        }

        return throwError(() => error);
      })
    );
  }

  getMyStore(): Observable<StoreResource> {
    if (!this.jwtService.getToken()) {
      return throwError(() => new Error('Authentication required'));
    }

    return this.http.get<StoreResource>(this.apiConfig.buildUrl('/api/stores/my'));
  }

  private createWithFallbackEndpoints(payload: StoreCreatePayload): Observable<HttpResponse<StoreResource>> {
    const myStoreEndpoint = this.apiConfig.buildUrl('/api/stores/my');
    console.log('🔁 StoreService.createStore: fallback POST', myStoreEndpoint);

    return this.http.post<StoreResource>(myStoreEndpoint, payload, {
      headers: this.buildAuthHeaders(),
      observe: 'response'
    }).pipe(
      catchError((error) => {
        if (error?.status === 404 || error?.status === 405) {
          const marketplaceStoresEndpoint = this.apiConfig.buildUrl('/api/marketplace/stores');
          console.log('🔁 StoreService.createStore: fallback POST', marketplaceStoresEndpoint);
          return this.http.post<StoreResource>(marketplaceStoresEndpoint, payload, {
            headers: this.buildAuthHeaders(),
            observe: 'response'
          });
        }

        return throwError(() => error);
      })
    );
  }

  private buildAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('espritmarket_jwt_token');
    if (!token) {
      return new HttpHeaders();
    }

    return new HttpHeaders({
      Authorization: `Bearer ${token}`
    });
  }
}
