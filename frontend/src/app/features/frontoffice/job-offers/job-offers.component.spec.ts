import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { of } from 'rxjs';
import { JobOffersComponent } from './job-offers.component';
import { PartnershipApiService } from '../../../services/api/partnership-api.service';
import { AuthService } from '../../../services/auth.service';
import { ActivatedRoute } from '@angular/router';

describe('JobOffersComponent', () => {
  let component: JobOffersComponent;
  let fixture: ComponentFixture<JobOffersComponent>;
  let partnershipApiService: any;
  let authService: any;

  beforeEach(async () => {
    partnershipApiService = {
      jobOffers: { list: () => of([]) },
      applications: { create: () => of({}) },
      searchJobOffers: () => of([]),
      applyWithProfile: () => of({})
    };

    authService = {
      currentUser: () => ({ id: 1, name: 'Test User' }),
      isLoggedIn: () => true
    };

    await TestBed.configureTestingModule({
      imports: [JobOffersComponent, FormsModule, CommonModule],
      providers: [
        { provide: PartnershipApiService, useValue: partnershipApiService },
        { provide: AuthService, useValue: authService },
        { provide: ActivatedRoute, useValue: {} }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(JobOffersComponent);
    component = fixture.componentInstance;
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize empty job offers list', () => {
    fixture.detectChanges();
    expect(Array.isArray(component.jobOffers)).toBe(true);
  });

  it('should initialize empty motivations object', () => {
    fixture.detectChanges();
    expect(typeof component.motivations).toBe('object');
  });
});
