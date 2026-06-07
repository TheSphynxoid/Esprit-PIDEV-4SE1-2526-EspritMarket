import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { jwtInterceptor, authErrorInterceptor } from './services/api';
import { TokenRefreshService } from './services/api/token-refresh.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection(),
    provideHttpClient(withInterceptors([jwtInterceptor, authErrorInterceptor])),
    provideRouter(routes),
    TokenRefreshService
  ]
};
