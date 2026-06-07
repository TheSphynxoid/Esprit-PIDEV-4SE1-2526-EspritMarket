import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

import { JwtService } from './jwt.service';
import { ApiConfigService } from './api-config.service';

const REFRESH_PATH = '/api/auth/refresh';
const CHECK_INTERVAL_MS = 5 * 60 * 1000;
const EXPIRY_BUFFER_MS = 30 * 60 * 1000;

@Injectable({ providedIn: 'root' })
export class TokenRefreshService {
  private timerId: ReturnType<typeof setInterval> | null = null;
  private isRefreshing = false;

  constructor() {
    if (isPlatformBrowser(inject(PLATFORM_ID))) {
      this.startPeriodicCheck();
    }
  }

  private startPeriodicCheck(): void {
    if (this.timerId) return;

    this.timerId = setInterval(() => this.checkAndRefresh(), CHECK_INTERVAL_MS);

    window.addEventListener('focus', () => {
      this.checkAndRefresh();
    });
  }

  private checkAndRefresh(): void {
    if (this.isRefreshing) return;

    const token = inject(JwtService).getToken();
    if (!token) return;

    try {
      const parts = token.split('.');
      if (parts.length !== 3) return;
      const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(base64));
      const exp = payload.exp as number | undefined;
      if (!exp) return;

      const remainingMs = exp * 1000 - Date.now();
      if (remainingMs > EXPIRY_BUFFER_MS) return;

      this.doRefresh();
    } catch {
      // ignore decode errors
    }
  }

  private doRefresh(): void {
    const jwtService = inject(JwtService);
    const apiConfig = inject(ApiConfigService);
    const refreshToken = jwtService.getRefreshToken();
    if (!refreshToken || this.isRefreshing) return;

    this.isRefreshing = true;

    fetch(apiConfig.buildUrl(REFRESH_PATH), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    })
      .then(res => {
        if (!res.ok) throw new Error('Refresh failed');
        return res.json();
      })
      .then((data: { token?: string; refreshToken?: string }) => {
        jwtService.setToken(data.token ?? '');
        jwtService.setRefreshToken(data.refreshToken ?? '');
      })
      .catch(() => {
        // will be handled by the interceptor on next 401
      })
      .finally(() => {
        this.isRefreshing = false;
      });
  }

  destroy(): void {
    if (this.timerId) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }
}
