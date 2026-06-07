import { Injectable } from '@angular/core';

const BASIC_AUTH_STORAGE_KEY = 'espritmarket_basic_auth';

export interface BasicAuthCredentials {
  username: string;
  password: string;
}

@Injectable({
  providedIn: 'root'
})
export class BasicAuthService {
  getCredentials(): BasicAuthCredentials | null {
    const raw = localStorage.getItem(BASIC_AUTH_STORAGE_KEY);
    if (!raw) {
      return null;
    }

    try {
      const parsed = JSON.parse(raw) as Partial<BasicAuthCredentials>;
      if (!parsed.username || !parsed.password) {
        return null;
      }
      return {
        username: parsed.username,
        password: parsed.password
      };
    } catch {
      return null;
    }
  }

  setCredentials(credentials: BasicAuthCredentials): void {
    localStorage.setItem(BASIC_AUTH_STORAGE_KEY, JSON.stringify(credentials));
  }

  clearCredentials(): void {
    localStorage.removeItem(BASIC_AUTH_STORAGE_KEY);
  }

  getAuthorizationHeaderValue(): string | null {
    const credentials = this.getCredentials();
    if (!credentials) {
      return null;
    }

    const token = btoa(`${credentials.username}:${credentials.password}`);
    return `Basic ${token}`;
  }
}
