import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { RecruiterInterviewsComponent } from './recruiter-interviews.component';
import { PartnershipApiService } from '../../../services/api/partnership-api.service';

describe('RecruiterInterviewsComponent', () => {
  let component: RecruiterInterviewsComponent;
  let fixture: ComponentFixture<RecruiterInterviewsComponent>;
  let partnershipApiService: any;

  beforeEach(async () => {
    partnershipApiService = {
      getApplicationsForInterviewForm: () => of([]),
      interviews: { list: () => of([]), create: () => of({}), update: () => of({}) }
    };

    await TestBed.configureTestingModule({
      imports: [RecruiterInterviewsComponent, ReactiveFormsModule],
      providers: [{ provide: PartnershipApiService, useValue: partnershipApiService }]
    }).compileComponents();

    fixture = TestBed.createComponent(RecruiterInterviewsComponent);
    component = fixture.componentInstance;
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize interview forms', () => {
    fixture.detectChanges();
    expect(component.form).toBeTruthy();
    expect(component.editResultForm).toBeTruthy();
  });

  it('should initialize interviews list', () => {
    fixture.detectChanges();
    expect(Array.isArray(component.interviews)).toBe(true);
  });
});
