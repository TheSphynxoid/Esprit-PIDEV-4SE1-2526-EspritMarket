import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { BasicAuthService } from './basic-auth.service';

describe('BasicAuthService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
  });

  it('should be created', () => {
    const service = TestBed.inject(BasicAuthService);
    expect(service).toBeTruthy();
  });
});