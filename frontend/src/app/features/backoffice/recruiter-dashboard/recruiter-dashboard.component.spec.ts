import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { RecruiterDashboardComponent } from './recruiter-dashboard.component';

describe('RecruiterDashboardComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecruiterDashboardComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
      .overrideComponent(RecruiterDashboardComponent, {
        set: {
          template: ''
        }
      })
      .compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(RecruiterDashboardComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});