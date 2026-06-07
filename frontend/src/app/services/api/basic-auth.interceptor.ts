import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { ApiConfigService } from './api-config.service';
import { BasicAuthService } from './basic-auth.service';

const PUBLIC_USER_REGISTRATION_PATH = '/api/common/users';

function isPublicRequest(pathname: string, method: string): boolean {
  return pathname === PUBLIC_USER_REGISTRATION_PATH && method.toUpperCase() === 'POST';
}

export const basicAuthInterceptor: HttpInterceptorFn = (request, next) => {
  const apiConfig = inject(ApiConfigService);
  const basicAuthService = inject(BasicAuthService);

  if (!apiConfig.isBackendUrl(request.url) || request.headers.has('Authorization')) {
    return next(request);
  }

  let pathname: string;
  try {
    pathname = new URL(request.url).pathname;
  } catch {
    pathname = request.url;
  }
  if (isPublicRequest(pathname, request.method)) {
    return next(request);
  }

  const authHeaderValue = basicAuthService.getAuthorizationHeaderValue();
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
