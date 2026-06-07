import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApiConfigService } from './api-config.service';
import { ApiMessageResponse } from './models/api-resource.model';
import {
  AuthRequest,
  AuthResponse,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  UserRequest
} from '@esprit-market/api-types';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);

  login(request: AuthRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      this.apiConfig.buildUrl('/api/auth/login'),
      request
    );
  }

  register(user: UserRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      this.apiConfig.buildUrl('/api/auth/register'),
      user
    );
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<ApiMessageResponse> {
    return this.http.post<ApiMessageResponse>(
      this.apiConfig.buildUrl('/api/auth/forgot-password'),
      request
    );
  }

  resetPassword(request: ResetPasswordRequest): Observable<ApiMessageResponse> {
    return this.http.post<ApiMessageResponse>(
      this.apiConfig.buildUrl('/api/auth/reset-password'),
      request
    );
  }

  registerWithPath(user: Record<string, unknown>, path: '/api/auth/register' | '/api/auth/signup'): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      this.apiConfig.buildUrl(path),
      user
    );
  }

  getMe(): Observable<AuthResponse> {
    return this.http.get<AuthResponse>(
      this.apiConfig.buildUrl('/api/auth/me')
    );
  }

  refreshToken(refreshToken: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      this.apiConfig.buildUrl('/api/auth/refresh'),
      { refreshToken }
    );
  }

  logout(token: string | null, refreshToken: string | null): Observable<void> {
    return this.http.post<void>(
      this.apiConfig.buildUrl('/api/auth/logout'),
      { token, refreshToken }
    );
  }
}
