import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { vi } from 'vitest';

import { AuthService } from '../services/auth.service';
import { JwtService } from '../services/api';
import { authGuard, roleGuard } from './auth.guard';

describe('auth.guard', () => {
  let authServiceMock: {
    isLoggedIn: ReturnType<typeof vi.fn>;
    currentUser: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
  };
  let jwtServiceMock: {
    isTokenExpired: ReturnType<typeof vi.fn>;
  };
  let routerMock: Pick<Router, 'navigate'>;

  beforeEach(() => {
    authServiceMock = {
      isLoggedIn: vi.fn(),
      currentUser: vi.fn(),
      logout: vi.fn()
    };

    jwtServiceMock = {
      isTokenExpired: vi.fn().mockReturnValue(false)
    };

    routerMock = {
      navigate: vi.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: JwtService, useValue: jwtServiceMock },
        { provide: Router, useValue: routerMock }
      ]
    });
  });

  it('authGuard should allow navigation for logged-in users', () => {
    authServiceMock.isLoggedIn.mockReturnValue(true);

    const result = TestBed.runInInjectionContext(() => authGuard({} as never, { url: '/dashboard' } as never));

    expect(result).toBeTruthy();
    expect(routerMock.navigate).not.toHaveBeenCalled();
  });

  it('authGuard should redirect anonymous users to login with returnUrl', () => {
    authServiceMock.isLoggedIn.mockReturnValue(false);

    const result = TestBed.runInInjectionContext(() => authGuard({} as never, { url: '/secure' } as never));

    expect(result).toBeFalsy();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/secure' }
    });
  });

  it('roleGuard should allow users with expected roles', () => {
    authServiceMock.isLoggedIn.mockReturnValue(true);
    authServiceMock.currentUser.mockReturnValue({ role: 'seller' });

    const result = TestBed.runInInjectionContext(() =>
      roleGuard({ data: { roles: ['seller', 'partner'] } } as never, { url: '/dashboard' } as never)
    );

    expect(result).toBeTruthy();
    expect(routerMock.navigate).not.toHaveBeenCalled();
  });

  it('roleGuard should redirect unauthenticated users to login', () => {
    authServiceMock.isLoggedIn.mockReturnValue(false);

    const result = TestBed.runInInjectionContext(() =>
      roleGuard({ data: { roles: ['admin_delivery'] } } as never, { url: '/dashboard' } as never)
    );

    expect(result).toBeFalsy();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('roleGuard should redirect users without role access to their fallback page', () => {
    authServiceMock.isLoggedIn.mockReturnValue(true);
    authServiceMock.currentUser.mockReturnValue({ role: 'seller' });

    const result = TestBed.runInInjectionContext(() =>
      roleGuard({ data: { roles: ['admin_delivery'] } } as never, { url: '/dashboard' } as never)
    );

    expect(result).toBeFalsy();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/dashboard/vendor']);
  });
});
