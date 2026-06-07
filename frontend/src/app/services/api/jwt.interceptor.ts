import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { ApiConfigService } from './api-config.service';
import { JwtService } from './jwt.service';

const PUBLIC_GET_PREFIXES = [
  '/api/srv/services',
  '/api/srv/service-reviews'
];

const PUBLIC_EXACT_PATHS = [
  '/api/auth/login',
  '/api/auth/register'
];

export const jwtInterceptor: HttpInterceptorFn = (request, next) => {
  const apiConfig = inject(ApiConfigService);
  const jwtService = inject(JwtService);

  if (!apiConfig.isBackendUrl(request.url) || request.headers.has('Authorization')) {
    return next(request);
  }

  let pathname: string;
  try {
    pathname = new URL(request.url).pathname;
  } catch {
    pathname = request.url;
  }

  if (PUBLIC_EXACT_PATHS.some(path => pathname === path)) {
    return next(request);
  }

  const isPublicGet = request.method === 'GET' &&
    PUBLIC_GET_PREFIXES.some(prefix => pathname === prefix || pathname.startsWith(prefix + '/'));

  if (isPublicGet) {
    return next(request);
  }

  const authHeaderValue = jwtService.getAuthorizationHeaderValue();
  if (!authHeaderValue) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        Authorization: authHeaderValue
      }
    })
  );
};
