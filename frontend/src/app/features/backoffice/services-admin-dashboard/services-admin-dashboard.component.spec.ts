import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { ServicesAdminDashboardComponent } from './services-admin-dashboard.component';

describe('ServicesAdminDashboardComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ServicesAdminDashboardComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
      .overrideComponent(ServicesAdminDashboardComponent, {
        set: {
          template: ''
        }
      })
      .compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(ServicesAdminDashboardComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});