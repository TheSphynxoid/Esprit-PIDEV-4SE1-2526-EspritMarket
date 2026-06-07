import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError, from, BehaviorSubject, filter, take } from 'rxjs';

import { JwtService } from './jwt.service';
import { ApiConfigService } from './api-config.service';

const REFRESH_PATH = '/api/auth/refresh';
const LOGIN_PATH = '/api/auth/login';

let isRefreshing = false;
let refreshSubject = new BehaviorSubject<string | null>(null);

interface RefreshResponse {
  token?: string;
  refreshToken?: string;
}

export const authErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const router = inject(Router);
  const jwtService = inject(JwtService);
  const apiConfig = inject(ApiConfigService);

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || !apiConfig.isBackendUrl(request.url)) {
        return throwError(() => error);
      }

      let pathname: string;
      try {
        pathname = new URL(request.url).pathname;
      } catch {
        pathname = request.url;
      }

      if (pathname === REFRESH_PATH || pathname === LOGIN_PATH) {
        return throwError(() => error);
      }

      if (isRefreshing) {
        return refreshSubject.pipe(
          filter(token => token !== null),
          take(1),
          switchMap(() => next(request))
        );
      }

      const refreshToken = jwtService.getRefreshToken();
      if (!refreshToken) {
        forceLogout(jwtService, router);
        return throwError(() => error);
      }

      isRefreshing = true;
      refreshSubject.next(null);

      return from(
        fetch(apiConfig.buildUrl(REFRESH_PATH), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken })
        })
      ).pipe(
        switchMap((response) => {
          if (!response.ok) {
            throw new Error('Refresh failed');
          }
          return from(response.json() as Promise<RefreshResponse>);
        }),
        switchMap((data) => {
          jwtService.setToken(data.token ?? '');
          jwtService.setRefreshToken(data.refreshToken ?? '');
          isRefreshing = false;
          refreshSubject.next(data.token ?? '');
          return next(request.clone({
            setHeaders: { Authorization: `Bearer ${data.token}` }
          }));
        }),
        catchError(() => {
          isRefreshing = false;
          refreshSubject.next(null);
          forceLogout(jwtService, router);
          return throwError(() => error);
        })
      );
    })
  );
};

function forceLogout(jwtService: JwtService, router: Router): void {
  jwtService.clearAll();
  localStorage.removeItem('crispri_user');
  router.navigate(['/login']);
}
