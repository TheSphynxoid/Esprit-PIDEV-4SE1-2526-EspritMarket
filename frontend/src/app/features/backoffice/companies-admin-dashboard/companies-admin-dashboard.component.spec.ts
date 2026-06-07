import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { CompaniesAdminDashboardComponent } from './companies-admin-dashboard.component';

describe('CompaniesAdminDashboardComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CompaniesAdminDashboardComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
      .overrideComponent(CompaniesAdminDashboardComponent, {
        set: {
          template: ''
        }
      })
      .compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(CompaniesAdminDashboardComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});