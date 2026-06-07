import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { RecruiterJoboffersComponent } from './recruiter-joboffers.component';
import { PartnershipApiService } from '../../../services/api/partnership-api.service';

describe('RecruiterJoboffersComponent', () => {
  let component: RecruiterJoboffersComponent;
  let fixture: ComponentFixture<RecruiterJoboffersComponent>;
  let partnershipApiService: any;

  beforeEach(async () => {
    partnershipApiService = {
      companies: { list: () => of([]) },
      jobOffers: { list: () => of([]), create: () => of({}), update: () => of({}), delete: () => of({}) }
    };

    await TestBed.configureTestingModule({
      imports: [RecruiterJoboffersComponent, ReactiveFormsModule],
      providers: [{ provide: PartnershipApiService, useValue: partnershipApiService }]
    }).compileComponents();

    fixture = TestBed.createComponent(RecruiterJoboffersComponent);
    component = fixture.componentInstance;
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize job offers form', () => {
    fixture.detectChanges();
    expect(component.form).toBeTruthy();
  });

  it('should initialize empty job offers list', () => {
    fixture.detectChanges();
    expect(Array.isArray(component.jobOffers)).toBe(true);
  });

  it('should initialize companies list', () => {
    fixture.detectChanges();
    expect(Array.isArray(component.companies)).toBe(true);
  });
});
