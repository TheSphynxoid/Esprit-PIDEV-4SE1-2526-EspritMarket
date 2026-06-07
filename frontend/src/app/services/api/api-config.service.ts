import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ApiConfigService {
  readonly baseUrl = environment.apiBaseUrl.replace(/\/$/, '');

  buildUrl(path: string): string {
    const normalizedPath = path.startsWith('/') ? path : `/${path}`;
    return `${this.baseUrl}${normalizedPath}`;
  }

  buildAssetUrl(path: string): string {
    const normalizedPath = path.startsWith('/') ? path : `/${path}`;
    const backendOrigin = this.getBackendOrigin();
    return backendOrigin ? `${backendOrigin}${normalizedPath}` : normalizedPath;
  }

  isBackendUrl(url: string): boolean {
    if (url.startsWith('/api/')) {
      return true;
    }

    if (this.baseUrl && url.startsWith(this.baseUrl)) {
      return true;
    }

    try {
      const parsed = new URL(url, 'http://example.invalid');
      return parsed.pathname.startsWith('/api/');
    } catch {
      return false;
    }
  }

  private getBackendOrigin(): string {
    if (!this.baseUrl) {
      return '';
    }

    try {
      return new URL(this.baseUrl).origin;
    } catch {
      return this.baseUrl;
    }
  }
}
