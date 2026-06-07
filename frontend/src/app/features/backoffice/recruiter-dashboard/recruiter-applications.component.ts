import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { timeout } from 'rxjs/operators';
import { PartnershipApiService } from '../../../services/api/partnership-api.service';
import {
  ApplicationResource,
  ApplicationWritePayload,
  JobOfferResource
} from '../../../services/api/models/api-resource.model';
import { RecruiterCacheService } from './recruiter-cache.service';

@Component({
  selector: 'app-recruiter-applications',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './recruiter-applications.component.html',
  styleUrls: ['./recruiter-applications.component.css']
})
export class RecruiterApplicationsComponent implements OnInit, OnDestroy {
  private partnershipApi = inject(PartnershipApiService);
  private cache = inject(RecruiterCacheService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private sub = new Subscription();

  applications: ApplicationResource[] = [];
  jobOffers: JobOfferResource[] = [];
  form!: FormGroup;
  editingApplicationId: number | null = null;
  errorMessage = '';
  successMessage = '';
  loading = false;
  deletingApplicationId: number | null = null;
  deleteConfirmationId: number | null = null;
  expandedApplicationId: number | null = null;

  readonly statuses = ['PENDING', 'ACCEPTED', 'REJECTED'];

  ngOnInit(): void {
    this.initializeForm();
    // Subscribe only to data streams — loading is managed locally
    this.sub.add(
      this.cache.jobOffers$.subscribe(data => {
        this.jobOffers = data;
        this.cdr.detectChanges();
      })
    );
    this.sub.add(
      this.cache.applications$.subscribe(data => {
        this.applications = data;
        this.loading = false;
        this.cdr.detectChanges();
      })
    );
    this.sub.add(
      this.cache.applicationsError$.subscribe(msg => {
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
      status: ['PENDING', Validators.required]
    });
  }

  /** @deprecated – data is now sourced from RecruiterCacheService */
  private loadJobOffers(): void {
    this.cache.refreshJobOffers();
  }

  /** @deprecated – data is now sourced from RecruiterCacheService */
  private loadApplications(): void {
    this.cache.refreshApplications();
  }

  startEditStatus(application: ApplicationResource): void {
    this.editingApplicationId = application.id;
    this.form.patchValue({
      status: application.status
    });
  }

  cancelEditStatus(): void {
    this.editingApplicationId = null;
    this.form.reset({ status: 'PENDING' });
  }

  updateStatus(): void {
    if (this.form.invalid || !this.editingApplicationId) {
      this.errorMessage = 'Please select a valid status';
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';
    
    const application = this.applications.find(a => a.id === this.editingApplicationId);
    if (!application) return;

    const payload: ApplicationWritePayload = {
      status: this.form.get('status')?.value,
      matchingScore: application.matchingScore,
      applicantId: application.applicant?.id || 0,
      jobOfferId: application.jobOffer?.id || 0
    };

    this.partnershipApi.applications.update(this.editingApplicationId, payload).subscribe({
      next: () => {
        this.successMessage = 'Application status updated successfully';
        this.cancelEditStatus();
        this.loading = true;
        this.cache.refreshApplications();
      },
      error: (err) => {
        this.errorMessage =
          'Failed to update application: ' + (err.error?.message || 'Unknown error');
      }
    });
  }

  getJobOfferTitle(jobOfferId: number | undefined): string {
    if (!jobOfferId) return 'Unknown';
    const jobOffer = this.jobOffers.find((j) => j.id === jobOfferId);
    return jobOffer ? jobOffer.title : 'Unknown';
  }

  getFieldError(fieldName: string): string {
    const field = this.form.get(fieldName);
    if (!field || !field.errors || !field.touched) return '';

    if (field.errors['required']) return `${this.formatLabel(fieldName)} is required`;
    return '';
  }

  private formatLabel(fieldName: string): string {
    return fieldName.charAt(0).toUpperCase() + fieldName.slice(1).replace(/([A-Z])/g, ' $1');
  }

  confirmDelete(applicationId: number): void {
    this.deleteConfirmationId = applicationId;
  }

  cancelDelete(): void {
    this.deleteConfirmationId = null;
  }

  deleteApplication(applicationId: number): void {
    this.deletingApplicationId = applicationId;
    this.errorMessage = '';
    this.successMessage = '';

    this.partnershipApi.applications.delete(applicationId).subscribe({
      next: () => {
        this.successMessage = 'Application deleted successfully';
        this.deleteConfirmationId = null;
        this.deletingApplicationId = null;
        this.loading = true;
        this.cache.refreshApplications();
        
        // Clear success message after 3 seconds
        setTimeout(() => {
          this.successMessage = '';
        }, 3000);
      },
      error: (err) => {
        this.deletingApplicationId = null;
        console.error('Delete failed:', err);
        this.errorMessage = 'Failed to delete application: ' + (err.error?.message || 'Unknown error');
      }
    });
  }

  toggleProfile(applicationId: number): void {
    if (this.expandedApplicationId === applicationId) {
      this.expandedApplicationId = null;
    } else {
      this.expandedApplicationId = applicationId;
    }
  }

  getScoreClass(score: number): string {
    if (score >= 75) return 'score-high';
    if (score >= 50) return 'score-medium';
    return 'score-low';
  }

}
