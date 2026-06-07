import { HttpHeaders, HttpRequest, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { ApiConfigService } from './api-config.service';
import { BasicAuthService } from './basic-auth.service';
import { basicAuthInterceptor } from './basic-auth.interceptor';

describe('basicAuthInterceptor', () => {
  let apiConfigMock: { isBackendUrl: ReturnType<typeof vi.fn> };
  let basicAuthServiceMock: { getAuthorizationHeaderValue: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    apiConfigMock = {
      isBackendUrl: vi.fn()
    };

    basicAuthServiceMock = {
      getAuthorizationHeaderValue: vi.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: ApiConfigService, useValue: apiConfigMock },
        { provide: BasicAuthService, useValue: basicAuthServiceMock }
      ]
    });
  });

  it('should add Authorization header for backend requests', () => {
    apiConfigMock.isBackendUrl.mockReturnValue(true);
    basicAuthServiceMock.getAuthorizationHeaderValue.mockReturnValue('Basic abc');

    const request = new HttpRequest('GET', 'http://localhost:8088/api/products');
    let forwarded!: HttpRequest<unknown>;

    TestBed.runInInjectionContext(() =>
      basicAuthInterceptor(request, (nextRequest) => {
        forwarded = nextRequest;
        return of(new HttpResponse({ status: 200 }));
      })
    ).subscribe();

    expect(forwarded.headers.get('Authorization')).toBe('Basic abc');
  });

  it('should keep existing Authorization header untouched', () => {
    apiConfigMock.isBackendUrl.mockReturnValue(true);

    const request = new HttpRequest('GET', 'http://localhost:8088/api/products', {
      headers: new HttpHeaders({ Authorization: 'Bearer existing' })
    });
    let forwarded!: HttpRequest<unknown>;

    TestBed.runInInjectionContext(() =>
      basicAuthInterceptor(request, (nextRequest) => {
        forwarded = nextRequest;
        return of(new HttpResponse({ status: 200 }));
      })
    ).subscribe();

    expect(forwarded.headers.get('Authorization')).toBe('Bearer existing');
    expect(basicAuthServiceMock.getAuthorizationHeaderValue).not.toHaveBeenCalled();
  });

  it('should bypass public registration endpoint', () => {
    apiConfigMock.isBackendUrl.mockReturnValue(true);
    basicAuthServiceMock.getAuthorizationHeaderValue.mockReturnValue('Basic abc');

    const request = new HttpRequest('POST', 'http://localhost:8088/api/common/users', null);
    let forwarded!: HttpRequest<unknown>;

    TestBed.runInInjectionContext(() =>
      basicAuthInterceptor(request, (nextRequest) => {
        forwarded = nextRequest;
        return of(new HttpResponse({ status: 200 }));
      })
    ).subscribe();

    expect(forwarded.headers.has('Authorization')).toBeFalsy();
  });
});
