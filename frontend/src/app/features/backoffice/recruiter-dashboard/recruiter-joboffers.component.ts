import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';
import { PartnershipApiService } from '../../../services/api/partnership-api.service';
import {
  JobOfferResource,
  JobOfferWritePayload,
  PartnerCompanyResource
} from '../../../services/api/models/api-resource.model';
import { RecruiterCacheService } from './recruiter-cache.service';

@Component({
  selector: 'app-recruiter-joboffers',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './recruiter-joboffers.component.html',
  styleUrls: ['./recruiter-joboffers.component.css']
})
export class RecruiterJoboffersComponent implements OnInit, OnDestroy {
  private partnershipApi = inject(PartnershipApiService);
  private cache = inject(RecruiterCacheService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private sub = new Subscription();

  jobOffers: JobOfferResource[] = [];
  companies: PartnerCompanyResource[] = [];
  form!: FormGroup;
  isEditMode = false;
  selectedJobOfferId: number | null = null;
  errorMessage = '';
  successMessage = '';
  loading = false;

  readonly types = ['INTERNSHIP', 'PROJECT', 'SERVICE'];
  readonly statuses = ['OPEN', 'CLOSED'];
  readonly experienceLevels: Array<'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'> = [
    'BEGINNER',
    'INTERMEDIATE',
    'ADVANCED'
  ];
  newSkillInput = '';

  ngOnInit(): void {
    this.initializeForm();
    // Subscribe only to data streams — loading is managed locally
    this.sub.add(
      this.cache.companies$.subscribe(data => {
        this.companies = data;
        this.cdr.detectChanges();
      })
    );
    this.sub.add(
      this.cache.jobOffers$.subscribe(data => {
        this.jobOffers = data;
        this.loading = false;
        this.cdr.detectChanges();
      })
    );
    this.sub.add(
      this.cache.jobOffersError$.subscribe(msg => {
        if (msg) {
          this.errorMessage = msg;
          this.loading = false;
          this.cdr.detectChanges();
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  private initializeForm(): void {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(150)]],
      description: ['', Validators.maxLength(2000)],
      type: ['INTERNSHIP', Validators.required],
      status: ['OPEN', Validators.required],
      location: ['', [Validators.required, Validators.maxLength(255)]],
      experienceLevel: ['BEGINNER', Validators.required],
      requiredSkills: [[] as string[]],
      companyId: ['', Validators.required]
    });
  }

  /** @deprecated – data is now sourced from RecruiterCacheService */
  private loadCompanies(): void {
    this.cache.refreshCompanies();
  }

  /** @deprecated – data is now sourced from RecruiterCacheService */
  private loadJobOffers(): void {
    this.cache.refreshJobOffers();
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.errorMessage = 'Please fill all required fields correctly';
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';
    const payload: any = {
      title: this.form.value.title,
      description: this.form.value.description,
      type: this.form.value.type,
      status: this.form.value.status,
      location: this.form.value.location,
      experienceLevel: this.form.value.experienceLevel,
      requiredSkills: this.requiredSkills.join(', '),
      companyId: Number(this.form.value.companyId)
    };

    if (this.isEditMode && this.selectedJobOfferId) {
      this.partnershipApi.jobOffers.update(this.selectedJobOfferId, payload).subscribe({
        next: () => {
          this.successMessage = 'Job offer updated successfully';
          this.resetForm();
          this.loading = true;
          this.cache.refreshJobOffers();
        },
        error: (err) => {
          this.errorMessage =
            'Failed to update job offer: ' + (err.error?.message || 'Unknown error');
        }
      });
    } else {
      this.partnershipApi.jobOffers.create(payload).subscribe({
        next: () => {
          this.successMessage = 'Job offer created successfully';
          this.resetForm();
          this.loading = true;
          this.cache.refreshJobOffers();
        },
        error: (err) => {
          this.errorMessage = 'Failed to create job offer: ' + (err.error?.message || 'Unknown error');
        }
      });
    }
  }

  editJobOffer(jobOffer: JobOfferResource): void {
    this.isEditMode = true;
    this.selectedJobOfferId = jobOffer.id;
    const skills = typeof jobOffer.requiredSkills === 'string'
      ? jobOffer.requiredSkills.split(',').map(s => s.trim())
      : (jobOffer.requiredSkills || []);
    this.form.patchValue({
      title: jobOffer.title,
      description: jobOffer.description,
      type: jobOffer.type,
      status: jobOffer.status,
      location: jobOffer.location || '',
      experienceLevel: jobOffer.experienceLevel || 'BEGINNER',
      requiredSkills: skills,
      companyId: jobOffer.company?.id || ''
    });
    this.newSkillInput = '';
  }

  deleteJobOffer(id: number): void {
    if (confirm('Are you sure you want to delete this job offer?')) {
      this.partnershipApi.jobOffers.delete(id).subscribe({
        next: () => {
          this.successMessage = 'Job offer deleted successfully';
          this.loading = true;
          this.cache.refreshJobOffers();
        },
        error: (err) => {
          this.errorMessage = 'Failed to delete job offer: ' + (err.error?.message || 'Unknown error');
        }
      });
    }
  }

  resetForm(): void {
    this.form.reset({
      type: 'INTERNSHIP',
      status: 'OPEN',
      location: '',
      experienceLevel: 'BEGINNER',
      requiredSkills: [],
      companyId: ''
    });
    this.newSkillInput = '';
    this.isEditMode = false;
    this.selectedJobOfferId = null;
  }

  get requiredSkills(): string[] {
    return this.form.get('requiredSkills')?.value || [];
  }

  onSkillInputChange(event: Event): void {
    this.newSkillInput = (event.target as HTMLInputElement).value;
  }

  onSkillInputKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.addSkill();
    }
  }

  addSkill(): void {
    const skill = this.newSkillInput.trim();
    if (!skill) {
      return;
    }

    const exists = this.requiredSkills.some((existingSkill) =>
      existingSkill.toLowerCase() === skill.toLowerCase()
    );
    if (exists) {
      this.newSkillInput = '';
      return;
    }

    this.form.patchValue({ requiredSkills: [...this.requiredSkills, skill] });
    this.newSkillInput = '';
  }

  removeSkill(skill: string): void {
    this.form.patchValue({
      requiredSkills: this.requiredSkills.filter((existingSkill) => existingSkill !== skill)
    });
  }

  getCompanyName(companyId: number | undefined): string {
    if (!companyId) return 'Unknown';
    const company = this.companies.find((c) => c.id === companyId);
    return company ? company.name : 'Unknown';
  }

  // Helper methods for form validation errors
  getFieldError(fieldName: string): string {
    const field = this.form.get(fieldName);
    if (!field || !field.errors || !field.touched) return '';

    if (field.errors['required']) return `${this.formatLabel(fieldName)} is required`;
    if (field.errors['minlength'])
      return `${this.formatLabel(fieldName)} must be at least ${field.errors['minlength'].requiredLength} characters`;
    if (field.errors['maxlength'])
      return `${this.formatLabel(fieldName)} cannot exceed ${field.errors['maxlength'].requiredLength} characters`;
    return '';
  }

  private formatLabel(fieldName: string): string {
    return fieldName.charAt(0).toUpperCase() + fieldName.slice(1).replace(/([A-Z])/g, ' $1');
  }
}

