import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';
import { PartnershipApiService } from '../../../services/api/partnership-api.service';
import { PartnerCompanyResource, PartnerCompanyWritePayload } from '../../../services/api/models/api-resource.model';
import { RecruiterCacheService } from './recruiter-cache.service';

@Component({
  selector: 'app-recruiter-companies',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './recruiter-companies.component.html',
  styleUrls: ['./recruiter-companies.component.css']
})
export class RecruiterCompaniesComponent implements OnInit, OnDestroy {
  private partnershipApi = inject(PartnershipApiService);
  private cache = inject(RecruiterCacheService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private sub = new Subscription();

  companies: PartnerCompanyResource[] = [];
  form!: FormGroup;
  isEditMode = false;
  selectedCompanyId: number | null = null;
  errorMessage = '';
  successMessage = '';
  loading = false;

  readonly statuses = ['PENDING', 'APPROVED', 'REJECTED'];

  ngOnInit(): void {
    this.initializeForm();
    // Subscribe only to the data stream — loading is managed locally
    this.sub.add(
      this.cache.companies$.subscribe(data => {
        this.companies = data;
        this.loading = false;
        this.cdr.detectChanges();
      })
    );
    this.sub.add(
      this.cache.companiesError$.subscribe(msg => {
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
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      sector: [''],
      contactEmail: ['', [Validators.required, Validators.email]],
      partnershipStatus: ['PENDING', Validators.required]
    });
  }

  /** @deprecated – data is now sourced from RecruiterCacheService */
  private loadCompanies(): void {
    this.cache.refreshCompanies();
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.errorMessage = 'Please fill all required fields correctly';
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';
    const payload: PartnerCompanyWritePayload = this.form.value;

    if (this.isEditMode && this.selectedCompanyId) {
      this.partnershipApi.companies.update(this.selectedCompanyId, payload).subscribe({
        next: () => {
          this.successMessage = 'Company updated successfully';
          this.resetForm();
          this.loading = true;
          this.cache.refreshCompanies();
        },
        error: (err) => {
          this.errorMessage = 'Failed to update company: ' + (err.error?.message || 'Unknown error');
        }
      });
    } else {
      this.partnershipApi.companies.create(payload).subscribe({
        next: () => {
          this.successMessage = 'Company created successfully';
          this.resetForm();
          this.loading = true;
          this.cache.refreshCompanies();
        },
        error: (err) => {
          this.errorMessage = 'Failed to create company: ' + (err.error?.message || 'Unknown error');
        }
      });
    }
  }

  editCompany(company: PartnerCompanyResource): void {
    this.isEditMode = true;
    this.selectedCompanyId = company.id;
    this.form.patchValue({
      name: company.name,
      sector: company.sector,
      contactEmail: company.contactEmail,
      partnershipStatus: company.partnershipStatus
    });
  }

  deleteCompany(id: number): void {
    if (confirm('Are you sure you want to delete this company?')) {
      this.partnershipApi.companies.delete(id).subscribe({
        next: () => {
          this.successMessage = 'Company deleted successfully';
          this.loading = true;
          this.cache.refreshCompanies();
        },
        error: (err) => {
          this.errorMessage = 'Failed to delete company: ' + (err.error?.message || 'Unknown error');
        }
      });
    }
  }

  resetForm(): void {
    this.form.reset({ partnershipStatus: 'PENDING' });
    this.isEditMode = false;
    this.selectedCompanyId = null;
  }

  // Helper methods for form validation errors
  getFieldError(fieldName: string): string {
    const field = this.form.get(fieldName);
    if (!field || !field.errors || !field.touched) return '';

    if (field.errors['required']) return `${this.formatLabel(fieldName)} is required`;
    if (field.errors['minlength']) return `${this.formatLabel(fieldName)} must be at least ${field.errors['minlength'].requiredLength} characters`;
    if (field.errors['maxlength']) return `${this.formatLabel(fieldName)} cannot exceed ${field.errors['maxlength'].requiredLength} characters`;
    if (field.errors['email']) return 'Valid email is required';
    return '';
  }

  private formatLabel(fieldName: string): string {
    return fieldName.charAt(0).toUpperCase() + fieldName.slice(1).replace(/([A-Z])/g, ' $1');
  }
}
