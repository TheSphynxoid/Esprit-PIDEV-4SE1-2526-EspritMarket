import { Injectable } from '@angular/core';

const JWT_TOKEN_KEY = 'espritmarket_jwt_token';
const REFRESH_TOKEN_KEY = 'espritmarket_refresh_token';

@Injectable({
  providedIn: 'root'
})
export class JwtService {
  getToken(): string | null {
    return localStorage.getItem(JWT_TOKEN_KEY);
  }

  setToken(token: string): void {
    localStorage.setItem(JWT_TOKEN_KEY, token);
  }

  clearToken(): void {
    localStorage.removeItem(JWT_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  setRefreshToken(token: string): void {
    localStorage.setItem(REFRESH_TOKEN_KEY, token);
  }

  clearRefreshToken(): void {
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  }

  clearAll(): void {
    this.clearToken();
    this.clearRefreshToken();
  }

  getAuthorizationHeaderValue(): string | null {
    const token = this.getToken();
    if (!token) {
      return null;
    }
    return `Bearer ${token}`;
  }

  isTokenExpired(): boolean {
    const token = this.getToken();
    if (!token) return true;
    try {
      const payload = this.decodeToken(token);
      if (!payload || payload['exp'] == null) return true;
      return Date.now() >= (payload['exp'] as number) * 1000;
    } catch {
      return true;
    }
  }

  getUserIdFromToken(): number | null {
    const token = this.getToken();
    if (!token) {
      return null;
    }

    try {
      const parts = token.split('.');
      if (parts.length !== 3) {
        return null;
      }

      // Decode the payload (second part)
      const payload = JSON.parse(atob(parts[1]));
      
      // Try to get the user ID from common claim names
      const userId = payload.sub || payload.userId || payload.user_id || payload.id;
      
      if (userId && typeof userId === 'number') {
        return userId;
      }
      
      if (userId && typeof userId === 'string') {
        const parsed = Number.parseInt(userId, 10);
        return Number.isFinite(parsed) ? parsed : null;
      }

      return null;
    } catch (error) {
      return null;
    }
  }

  private decodeToken(token: string): Record<string, unknown> | null {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      const base64Url = parts[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(jsonPayload);
    } catch {
      return null;
    }
  }
}
