import { Injectable, computed, inject, signal } from '@angular/core';
import { BehaviorSubject, Observable, throwError, TimeoutError, of } from 'rxjs';
import { catchError, finalize, map, tap, timeout } from 'rxjs/operators';

import {
  JwtService,
  AuthApiService,
  CommonApiService,
  UserResource,
  Role
} from './api';
import { UserRequest, AuthRequest, AuthResponse } from '@esprit-market/api-types';

export type UserRole =
  | 'visitor'
  | 'seller'
  | 'service_provider'
  | 'deliverer'
  | 'admin_delivery'
  | 'recruiter'
  | 'partner'
  | 'event'
  | 'admin_market';

export interface User {
  id: number;
  email: string;
  name: string;
  role: UserRole;
  avatar?: string;
  joinDate: Date;
}

export interface AuthState {
  isLoggedIn: boolean;
  user: User | null;
  loading: boolean;
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly commonApi = inject(CommonApiService);
  private readonly authApi = inject(AuthApiService);
  private readonly jwtService = inject(JwtService);

  private authState = signal<AuthState>({
    isLoggedIn: false,
    user: null,
    loading: false,
    error: null
  });

  isLoggedIn = computed(() => this.authState().isLoggedIn);
  currentUser = computed(() => this.authState().user);
  isLoading = computed(() => this.authState().loading);
  error = computed(() => this.authState().error);

  private loginAction$ = new BehaviorSubject<{ email: string; password: string; role: UserRole } | null>(null);

  constructor() {
    this.initializeAuth();
  }

  private initializeAuth(): void {
    const savedUser = localStorage.getItem('crispri_user');
    const token = this.jwtService.getToken();

    if (token) {
      if (this.jwtService.isTokenExpired()) {
        this.logout();
        return;
      }

      if (savedUser) {
        try {
          const user = JSON.parse(savedUser);
          
          // Update user ID from JWT token
          const userId = this.jwtService.getUserIdFromToken();
          if (userId && userId !== 0) {
            user.id = userId;
          }
          
          this.authState.set({
            isLoggedIn: true,
            user,
            loading: false,
            error: null
          });
        } catch (e) {
          this.repairSession().subscribe();
        }
      } else {
        this.repairSession().subscribe();
      }
    } else {
      this.logout();
    }
  }

  login(email: string, password: string): Observable<User> {
    if (!email || !password) {
      return throwError(() => 'Email and password required');
    }

    this.authState.update(state => ({ ...state, loading: true, error: null }));

    const authRequest: AuthRequest = { email, password };

    return this.authApi.login(authRequest).pipe(
      map((authResponse) => {
        this.jwtService.setToken(authResponse.token ?? '');
        this.jwtService.setRefreshToken(authResponse.refreshToken ?? '');

        // Get user ID from AuthResponse first, fallback to JWT decoding
        let userId = authResponse.id ?? authResponse.userId ?? this.jwtService.getUserIdFromToken() ?? 0;

        const user: User = {
          id: userId,
          email: authResponse.email ?? '',
          name: authResponse.name ?? '',
          role: this.mapBackendRoleStringToFrontendRole(authResponse.role ?? ''),
          avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(authResponse.name ?? '')}&background=C8102E&color=fff`,
          joinDate: new Date()
        };

        return user;
      }),
      tap((user) => {
        localStorage.setItem('crispri_user', JSON.stringify(user));
        this.authState.set({
          isLoggedIn: true,
          user,
          loading: false,
          error: null
        });
        this.loginAction$.next({ email, password, role: user.role });
      }),
      catchError((error) => {
        const message = this.extractErrorMessage(error, 'Login error');
        this.jwtService.clearAll();
        this.authState.update(state => ({
          ...state,
          isLoggedIn: false,
          user: null,
          loading: false,
          error: message
        }));
        return throwError(() => message);
      }),
      finalize(() => {
        this.authState.update(state => ({ ...state, loading: false }));
      })
    );
  }

  register(email: string, password: string, name: string, role: UserRole): Observable<User> {
    this.authState.update(state => ({ ...state, loading: true, error: null }));

    const [firstName = '', ...lastNameParts] = name.trim().split(/\s+/);
    const lastName = lastNameParts.join(' ');
    const backendRole = this.mapFrontendRoleToBackendRole(role);

    const registrationAttempts: Array<{
      path: '/api/auth/register' | '/api/auth/signup';
      payload: Record<string, unknown>;
    }> = [
      {
        path: '/api/auth/register',
        payload: {
          name,
          email,
          password,
          role: backendRole
        }
      }
    ];

    if (role === 'admin_market') {
      registrationAttempts.push({
        path: '/api/auth/register',
        payload: {
          name,
          fullName: name,
          firstName,
          lastName,
          email,
          password,
          role: 'ADMIN'
        }
      });
    }

    console.log('Registration payload:', {
      ...registrationAttempts[0].payload,
      password: '***'
    });

    const executeAttempt = (index: number): Observable<AuthResponse> => {
      const attempt = registrationAttempts[index];
      return this.authApi.registerWithPath(attempt.payload, attempt.path).pipe(
        catchError((error) => {
          const canRetry = index < registrationAttempts.length - 1;
          const isLikelyContractMismatch = error?.status === 500 || error?.status === 400 || error?.status === 404;

          if (canRetry && isLikelyContractMismatch) {
            console.warn(`Registration attempt ${index + 1} failed. Retrying with fallback strategy.`);
            return executeAttempt(index + 1);
          }

          if (role === 'admin_market' && (error?.status === 500 || error?.status === 403 || error?.status === 400)) {
            return throwError(() => 'Admin Market registration failed on backend. Please verify backend role permissions/configuration and try again.');
          }

          // Format error message for client-side display
          let errorMessage: string;
          if (error?.error?.message) {
            errorMessage = error.error.message;
          } else if (error?.message) {
            errorMessage = error.message;
          } else if (error?.status === 0) {
            errorMessage = 'Cannot connect to server. Please check if the backend is running and reachable.';
          } else if (error?.status) {
            errorMessage = `Server error (${error.status}): ${error?.statusText || 'Unknown error'}`;
          } else {
            errorMessage = 'Registration failed. Please try again.';
          }

          return throwError(() => errorMessage);
        })
      );
    };

    return executeAttempt(0).pipe(
      timeout(30000), // 30 second timeout
      map((authResponse) => {
        console.log('Registration response:', authResponse);
        this.jwtService.setToken(authResponse.token ?? '');
        this.jwtService.setRefreshToken(authResponse.refreshToken ?? '');

        // Get user ID from AuthResponse first, fallback to JWT decoding
        let userId = authResponse.id ?? authResponse.userId ?? this.jwtService.getUserIdFromToken() ?? 0;

        const user: User = {
          id: userId,
          email: authResponse.email ?? '',
          name: authResponse.name ?? '',
          role: this.mapBackendRoleStringToFrontendRole(authResponse.role ?? ''),
          avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(authResponse.name ?? '')}&background=C8102E&color=fff`,
          joinDate: new Date()
        };

        return user;
      }),
      tap((user) => {
        localStorage.setItem('crispri_user', JSON.stringify(user));
        this.authState.set({
          isLoggedIn: true,
          user,
          loading: false,
          error: null
        });
      }),
      catchError((error) => {
        console.error('Registration error:', error);

        let message: string;
        if (error instanceof TimeoutError) {
          message = 'Request timed out. Please check your connection and try again.';
        } else if (error?.error?.message) {
          message = error.error.message;
        } else if (error?.message) {
          message = error.message;
        } else if (error?.status === 0) {
          message = 'Cannot connect to server. Please check if the backend is running and reachable.';
        } else if (error?.status) {
          message = `Server error (${error.status}): ${error?.statusText || 'Unknown error'}`;
        } else {
          message = 'Registration failed. Please try again.';
        }

        this.jwtService.clearAll();
        this.authState.update(state => ({
          ...state,
          loading: false,
          error: message
        }));
        return throwError(() => message);
      }),
      finalize(() => {
        this.authState.update(state => ({ ...state, loading: false }));
      })
    );
  }

  forgotPassword(email: string): Observable<string> {
    if (!email) {
      return throwError(() => 'Email required');
    }

    this.authState.update(state => ({ ...state, loading: true, error: null }));

    return this.authApi.forgotPassword({ email }).pipe(
      map((response) => response?.message ?? 'Code sent by email.'),
      catchError((error) => {
        const message = this.extractErrorMessage(error, 'Unable to send reset code.');
        this.authState.update(state => ({ ...state, error: message }));
        return throwError(() => message);
      }),
      finalize(() => {
        this.authState.update(state => ({ ...state, loading: false }));
      })
    );
  }

  resetPassword(email: string, code: string, newPassword: string): Observable<string> {
    if (!email || !code || !newPassword) {
      return throwError(() => 'Email, code and new password are required');
    }

    this.authState.update(state => ({ ...state, loading: true, error: null }));

    return this.authApi.resetPassword({ email, code, newPassword }).pipe(
      map((response) => response?.message ?? 'Password has been reset successfully.'),
      catchError((error) => {
        const message = this.extractErrorMessage(error, 'Unable to reset password.');
        this.authState.update(state => ({ ...state, error: message }));
        return throwError(() => message);
      }),
      finalize(() => {
        this.authState.update(state => ({ ...state, loading: false }));
      })
    );
  }

  logout(): void {
    const token = this.jwtService.getToken();
    const refreshToken = this.jwtService.getRefreshToken();

    if (token) {
      this.authApi.logout(token, refreshToken).subscribe({
        error: () => {}
      });
    }

    localStorage.removeItem('crispri_user');
    localStorage.removeItem('create_store_allow_step2');
    this.jwtService.clearAll();
    this.authState.set({
      isLoggedIn: false,
      user: null,
      loading: false,
      error: null
    });
  }

  repairSession(): Observable<boolean> {
    if (!this.jwtService.getToken()) {
      return of(false);
    }

    return this.authApi.getMe().pipe(
      map((authResponse) => {
        const user: User = {
          id: authResponse.id ?? 0,
          email: authResponse.email ?? '',
          name: authResponse.name ?? '',
          role: this.mapBackendRoleStringToFrontendRole(authResponse.role ?? ''),
          avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(authResponse.name ?? '')}&background=C8102E&color=fff`,
          joinDate: new Date()
        };
        localStorage.setItem('crispri_user', JSON.stringify(user));
        this.authState.update(state => ({ ...state, user, isLoggedIn: true }));
        return true;
      }),
      catchError((err) => {
        console.error('Failed to auto-repair session:', err);
        if (err?.status === 401) {
          this.logout();
        }
        return of(false);
      })
    );
  }

  refreshUser(userId: number): Observable<User> {
    return this.commonApi.users.getById(userId).pipe(
      map(resource => this.toAuthUser(resource)),
      tap(user => {
        const currentUser = this.authState().user;
        if (currentUser) {
          const updatedUser = { ...currentUser, role: user.role, name: user.name };
          localStorage.setItem('crispri_user', JSON.stringify(updatedUser));
          this.authState.update(state => ({ ...state, user: updatedUser }));
        }
      })
    );
  }

  updateCurrentUserRole(role: UserRole): void {
    const currentUser = this.authState().user;
    if (!currentUser) {
      return;
    }

    const updatedUser = { ...currentUser, role };
    localStorage.setItem('crispri_user', JSON.stringify(updatedUser));
    this.authState.update(state => ({ ...state, user: updatedUser, isLoggedIn: true }));
  }

  updateUser(updates: Partial<User>): void {
    const current = this.authState().user;
    if (current) {
      const updated = { ...current, ...updates };
      localStorage.setItem('crispri_user', JSON.stringify(updated));
      this.authState.update(state => ({
        ...state,
        user: updated
      }));
    }
  }

  hasRole(role: UserRole | UserRole[]): () => boolean {
    return () => {
      const user = this.currentUser();
      if (!user) return false;
      const roles = Array.isArray(role) ? role : [role];
      return roles.includes(user.role);
    };
  }

  private toAuthUser(resource: UserResource, fallbackRole?: UserRole): User {
    const resolvedRole = fallbackRole ?? this.mapBackendRoleToFrontendRole(resource.role ?? Role.USER);

    return {
      id: resource.id ?? 0,
      email: resource.email ?? '',
      name: resource.name ?? '',
      role: resolvedRole,
      avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(resource.name ?? '')}&background=C8102E&color=fff`,
      joinDate: new Date()
    };
  }

  private mapBackendRoleToFrontendRole(role: Role): UserRole {
    switch (role) {
      case Role.SELLER:
        return 'seller';
      case Role.SERVICE_PROVIDER:
        return 'service_provider';
      case Role.COURIER:
        return 'deliverer';
      case Role.ADMIN_DELIVERY:
        return 'admin_delivery';
      case Role.ORGANIZER:
        return 'recruiter';
      case Role.PARTNER:
        return 'partner';
      case Role.EVENT:
        return 'event';
      case Role.ADMIN_MARKET:
        return 'admin_market';
      case Role.RECRUITER:
        return 'recruiter';
      case Role.ADMIN:
        return 'admin_market';
      case Role.USER:
      default:
        return 'visitor';
    }
  }

  private mapBackendRoleStringToFrontendRole(roleString: string): UserRole {
    // Map the string role from JWT response to UserRole
    switch (roleString.toUpperCase()) {
      case 'SELLER':
        return 'seller';
      case 'SERVICE_PROVIDER':
        return 'service_provider';
      case 'COURIER':
        return 'deliverer';
      case 'ADMIN_DELIVERY':
        return 'admin_delivery';
      case 'ORGANIZER':
        return 'recruiter';
      case 'PARTNER':
        return 'partner';
      case 'EVENT':
        return 'event';
      case 'RECRUITER':
        return 'recruiter';
      case 'ADMIN_MARKET':
        return 'admin_market';
      case 'ADMIN':
        return 'admin_market';
      case 'USER':
      default:
        return 'visitor';
    }
  }

  private mapFrontendRoleToBackendRole(role: UserRole): Role {
    switch (role) {
      case 'seller':
        return Role.SELLER;
      case 'service_provider':
        return Role.SERVICE_PROVIDER;
      case 'deliverer':
        return Role.COURIER;
      case 'admin_delivery':
        return Role.ADMIN_DELIVERY;
      case 'recruiter':
        return Role.ORGANIZER;
      case 'partner':
        return Role.PARTNER;
      case 'event':
        return Role.EVENT;
      case 'admin_market':
        return Role.ADMIN_MARKET;
      case 'visitor':
      default:
        return Role.USER;
    }
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const typedError = error as { error?: { message?: string }; message?: string };
    return typedError?.error?.message ?? typedError?.message ?? fallback;
  }
}
