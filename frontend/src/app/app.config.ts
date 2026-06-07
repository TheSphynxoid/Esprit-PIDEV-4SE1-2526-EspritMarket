import { ApplicationConfig, APP_INITIALIZER, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors, HttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { environment } from '../environments/environment';

import { routes } from './app.routes';
import { jwtInterceptor, authErrorInterceptor } from './services/api';
import { TokenRefreshService } from './services/api/token-refresh.service';

function initializeGoogleMaps(http: HttpClient) {
  return () => firstValueFrom(http.get<{ configured: boolean, baseUrl: string, apiKey: string }>(`${environment.apiBaseUrl}/api/delivery/maps/config`))
    .then(config => {
      if (config.configured && config.apiKey) {
        return new Promise<void>((resolve, reject) => {
          const script = document.createElement('script');
          script.src = `https://maps.googleapis.com/maps/api/js?key=${config.apiKey}&libraries=places,geometry`;
          script.async = true;
          script.defer = true;
          script.onload = () => resolve();
          script.onerror = () => reject();
          document.head.appendChild(script);
        });
      }
      return Promise.resolve();
    })
    .catch(err => {
      console.warn('Failed to load Google Maps configuration', err);
      return Promise.resolve();
    });
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection(),
    provideHttpClient(withInterceptors([jwtInterceptor, authErrorInterceptor])),
    provideRouter(routes),
    TokenRefreshService,
    {
      provide: APP_INITIALIZER,
      useFactory: initializeGoogleMaps,
      deps: [HttpClient],
      multi: true
    }
  ]
};
