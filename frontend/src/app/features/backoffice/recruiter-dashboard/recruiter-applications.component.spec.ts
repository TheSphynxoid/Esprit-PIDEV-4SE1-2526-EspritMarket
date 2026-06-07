import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { RecruiterApplicationsComponent } from './recruiter-applications.component';
import { PartnershipApiService } from '../../../services/api/partnership-api.service';

describe('RecruiterApplicationsComponent', () => {
  let component: RecruiterApplicationsComponent;
  let fixture: ComponentFixture<RecruiterApplicationsComponent>;
  let partnershipApiService: any;

  beforeEach(async () => {
    partnershipApiService = {
      applications: { list: () => of([]), update: () => of({}), delete: () => of({}) },
      jobOffers: { list: () => of([]) }
    };

    await TestBed.configureTestingModule({
      imports: [RecruiterApplicationsComponent, ReactiveFormsModule],
      providers: [{ provide: PartnershipApiService, useValue: partnershipApiService }]
    }).compileComponents();

    fixture = TestBed.createComponent(RecruiterApplicationsComponent);
    component = fixture.componentInstance;
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize applications list', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should initialize with empty form', () => {
    fixture.detectChanges();
    expect(component.form).toBeTruthy();
  });
});
