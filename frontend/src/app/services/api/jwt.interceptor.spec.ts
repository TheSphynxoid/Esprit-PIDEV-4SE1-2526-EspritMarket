import { HttpHeaders, HttpRequest, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { ApiConfigService } from './api-config.service';
import { JwtService } from './jwt.service';
import { jwtInterceptor } from './jwt.interceptor';

describe('jwtInterceptor', () => {
  let apiConfigMock: { isBackendUrl: ReturnType<typeof vi.fn> };
  let jwtServiceMock: { getAuthorizationHeaderValue: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    apiConfigMock = {
      isBackendUrl: vi.fn()
    };

    jwtServiceMock = {
      getAuthorizationHeaderValue: vi.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: ApiConfigService, useValue: apiConfigMock },
        { provide: JwtService, useValue: jwtServiceMock }
      ]
    });
  });

  it('should add Authorization header for protected backend requests', () => {
    apiConfigMock.isBackendUrl.mockReturnValue(true);
    jwtServiceMock.getAuthorizationHeaderValue.mockReturnValue('Bearer token');

    const request = new HttpRequest('GET', 'http://localhost:8088/api/orders');
    let forwarded!: HttpRequest<unknown>;

    TestBed.runInInjectionContext(() =>
      jwtInterceptor(request, (nextRequest) => {
        forwarded = nextRequest;
        return of(new HttpResponse({ status: 200 }));
      })
    ).subscribe();

    expect(forwarded.headers.get('Authorization')).toBe('Bearer token');
  });

  it('should skip auth header for login endpoint', () => {
    apiConfigMock.isBackendUrl.mockReturnValue(true);
    jwtServiceMock.getAuthorizationHeaderValue.mockReturnValue('Bearer token');

    const request = new HttpRequest('POST', 'http://localhost:8088/api/auth/login', null);
    let forwarded!: HttpRequest<unknown>;

    TestBed.runInInjectionContext(() =>
      jwtInterceptor(request, (nextRequest) => {
        forwarded = nextRequest;
        return of(new HttpResponse({ status: 200 }));
      })
    ).subscribe();

    expect(forwarded.headers.has('Authorization')).toBeFalsy();
  });

  it('should skip when request already has Authorization header', () => {
    apiConfigMock.isBackendUrl.mockReturnValue(true);

    const request = new HttpRequest('GET', 'http://localhost:8088/api/orders', {
      headers: new HttpHeaders({ Authorization: 'Bearer existing' })
    });
    let forwarded!: HttpRequest<unknown>;

    TestBed.runInInjectionContext(() =>
      jwtInterceptor(request, (nextRequest) => {
        forwarded = nextRequest;
        return of(new HttpResponse({ status: 200 }));
      })
    ).subscribe();

    expect(forwarded.headers.get('Authorization')).toBe('Bearer existing');
    expect(jwtServiceMock.getAuthorizationHeaderValue).not.toHaveBeenCalled();
  });
});
