import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';
import { timeout, finalize } from 'rxjs/operators';
import { PartnershipApiService } from '../../../services/api/partnership-api.service';
import {
  InterviewResource,
  InterviewWritePayload,
  ApplicationResource
} from '../../../services/api/models/api-resource.model';
import { RecruiterCacheService } from './recruiter-cache.service';

@Component({
  selector: 'app-recruiter-interviews',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './recruiter-interviews.component.html',
  styleUrls: ['./recruiter-interviews.component.css']
})
export class RecruiterInterviewsComponent implements OnInit, OnDestroy {
  private partnershipApi = inject(PartnershipApiService);
  private cache = inject(RecruiterCacheService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);
  private sub = new Subscription();

  interviews: InterviewResource[] = [];
  applications: ApplicationResource[] = [];
  form!: FormGroup;
  editResultForm!: FormGroup;
  editingInterviewId: number | null = null;
  selectedInterview: InterviewResource | null = null;
  errorMessage = '';
  successMessage = '';
  loading = false;
  isSaving = false;
  suggestedSlots: string[] = [];
  loadingSlots = false;
  showResultModal = false;
  showConfirmation = false;
  showToast = false;

  readonly types = ['PHONE', 'VIDEO', 'IN_PERSON'];
  readonly statuses = ['SCHEDULED', 'COMPLETED', 'CANCELLED'];
  readonly results = ['ACCEPTED', 'REJECTED', 'WAITING_LIST'];
  readonly createStatus = 'SCHEDULED';

  ngOnInit(): void {
    this.initializeForm();
    this.initializeEditResultForm();
    // Subscribe only to data streams — loading is managed locally
    this.sub.add(
      this.cache.applications$.subscribe(data => {
        this.applications = data;
        this.cdr.detectChanges();
      })
    );
    this.sub.add(
      this.cache.interviews$.subscribe(data => {
        this.interviews = data;
        this.loading = false;
        this.cdr.detectChanges(); // Ensure UI reflects new data
      })
    );
    this.sub.add(
      this.cache.interviewsError$.subscribe(msg => {
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
      interviewDate: ['', [Validators.required, this.futureDateValidator.bind(this)]],
      type: ['PHONE', Validators.required],
      location: ['', Validators.maxLength(255)],
      applicationId: ['', Validators.required]
    });

    this.form.get('type')?.valueChanges.subscribe(() => this.updateLocationValidators());
    this.updateLocationValidators();
  }

  private initializeEditResultForm(): void {
    this.editResultForm = this.fb.group({
      status: ['SCHEDULED', Validators.required],
      result: [null],
      resultNotes: ['', Validators.maxLength(1000)]
    });

    this.editResultForm.get('status')?.valueChanges.subscribe(() => this.updateResultValidators());
    this.updateResultValidators();
  }

  private futureDateValidator(control: any): { [key: string]: any } | null {
    if (!control.value) {
      return null;
    }
    const selectedDate = new Date(control.value);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    if (selectedDate < today) {
      return { pastDate: true };
    }
    return null;
  }

  private updateLocationValidators(): void {
    const locationControl = this.form.get('location');
    const interviewType = this.form.get('type')?.value;
    if (!locationControl) {
      return;
    }

    const validators = [Validators.maxLength(255)];
    if (interviewType === 'VIDEO' || interviewType === 'IN_PERSON') {
      validators.unshift(Validators.required);
    }

    locationControl.setValidators(validators);
    locationControl.updateValueAndValidity({ emitEvent: false });
  }

  private updateResultValidators(): void {
    const status = this.editResultForm.get('status')?.value;
    const resultControl = this.editResultForm.get('result');

    if (!resultControl) {
      return;
    }

    if (status === 'COMPLETED') {
      resultControl.setValidators([Validators.required]);
    } else {
      resultControl.clearValidators();
      resultControl.setValue(null, { emitEvent: false });
    }

    resultControl.updateValueAndValidity({ emitEvent: false });
  }

  /** @deprecated – data is now sourced from RecruiterCacheService via applications$ */
  private loadApplications(): void {
    this.cache.refreshApplications();
  }

  /** @deprecated – data is now sourced from RecruiterCacheService via interviews$ */
  loadInterviews(): void {
    this.loading = true;
    this.partnershipApi.interviews.list().subscribe({
      next: (data) => {
        this.interviews = data;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load interviews';
        this.loading = false;
      }
    });
  }

  fetchSuggestedSlots(): void {
    const appId = this.form.get('applicationId')?.value;
    if (!appId) {
      this.errorMessage = 'Please select an application first to get suggestions.';
      return;
    }

    this.loadingSlots = true;
    this.errorMessage = '';
    this.cdr.detectChanges();
    
    this.partnershipApi.suggestInterviewSlots(Number(appId)).subscribe({
      next: (slots) => {
        this.suggestedSlots = slots;
        this.loadingSlots = false;
        if (slots.length === 0) {
          this.errorMessage = 'No available slots found in the next 30 days.';
        }
        this.cdr.detectChanges(); // Force UI update
      },
      error: (err) => {
        this.loadingSlots = false;
        this.errorMessage = 'Failed to fetch suggestions: ' + (err.error?.message || 'Unknown error');
        this.cdr.detectChanges();
      }
    });
  }

  applySuggestedSlot(slot: string): void {
    // The input type="datetime-local" expects format YYYY-MM-DDTHH:mm
    const formattedSlot = slot.substring(0, 16); 
    this.form.patchValue({ interviewDate: formattedSlot });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorMessage = 'Please fill all required fields correctly';
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';
    const payload: InterviewWritePayload = {
      interviewDate: this.form.get('interviewDate')?.value,
      type: this.form.get('type')?.value,
      location: this.form.get('location')?.value?.trim() || undefined,
      applicationId: Number(this.form.get('applicationId')?.value)
    };

    this.partnershipApi.interviews.create(payload).subscribe({
      next: () => {
        this.successMessage = 'Interview created successfully';
        this.resetForm();
        this.loadInterviews();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage =
          'Failed to create interview: ' + (err.error?.message || 'Unknown error');
        this.cdr.detectChanges();
      }
    });
  }

  startEditResult(interview: InterviewResource): void {
    if (interview.status !== 'SCHEDULED') {
      return;
    }

    this.editingInterviewId = interview.id;
    this.selectedInterview = interview;
    this.showResultModal = true;
    this.showConfirmation = false;
    this.editResultForm.patchValue({
      status: interview.status,
      result: interview.result ?? null,
      resultNotes: interview.resultNotes ?? ''
    });
    this.updateResultValidators();
  }

  cancelEditResult(): void {
    this.editingInterviewId = null;
    this.selectedInterview = null;
    this.showResultModal = false;
    this.showConfirmation = false;
    this.editResultForm.reset({
      status: 'SCHEDULED',
      result: null,
      resultNotes: ''
    });
    this.updateResultValidators();
  }

  confirmResult(): void {
    if (this.editResultForm.invalid || !this.editingInterviewId) {
      this.editResultForm.markAllAsTouched();
      return;
    }
    this.showConfirmation = true;
  }

  goBackToForm(): void {
    this.showConfirmation = false;
  }

  closeModal(): void {
    this.cancelEditResult();
  }

  showSuccessToast(message: string): void {
    this.successMessage = message;
    this.showToast = true;
    setTimeout(() => {
      this.showToast = false;
      this.successMessage = '';
    }, 3000);
  }

  executeResultUpdate(): void {
    this.errorMessage = '';
    this.isSaving = true;

    const selectedStatus = this.editResultForm.get('status')?.value;

    const payload: InterviewWritePayload = {
      status: selectedStatus,
      result: selectedStatus === 'COMPLETED' ? this.editResultForm.get('result')?.value : null,
      resultNotes: this.editResultForm.get('resultNotes')?.value?.trim() || null
    };

    this.partnershipApi.interviews.update(this.editingInterviewId!, payload)
      .pipe(
        timeout(10000),
        finalize(() => this.isSaving = false)
      )
      .subscribe({
        next: () => {
          this.closeModal();
          this.loadInterviews();
          this.showSuccessToast('Interview result saved successfully');
        },
        error: (err) => {
          if (err.name === 'TimeoutError') {
            this.errorMessage = 'Failed to save result: Request timed out after 10 seconds.';
          } else {
            this.errorMessage = 'Failed to save result: ' + (err.error?.message || err.message);
          }
          this.showConfirmation = false;
        }
      });
  }

  resetForm(): void {
    this.form.reset({
      type: 'PHONE',
      location: '',
      applicationId: ''
    });
    this.suggestedSlots = [];
    this.updateLocationValidators();
  }

  getApplicationLabel(applicationId: number | undefined): string {
    if (!applicationId) return 'Unknown';

    const application = this.applications.find((a) => a.id === applicationId);
    if (application) {
      const [firstName, lastName] = this.splitName(application.applicant?.name);
      const jobTitle = application.jobOffer?.title || 'Unknown Job';
      return `${`${firstName} ${lastName}`.trim()} — ${jobTitle}`;
    }

    return `Application #${applicationId}`;
  }

  getInterviewApplicationLabel(interview: InterviewResource): string {
    const student = `${interview.studentFirstName || ''} ${interview.studentLastName || ''}`.trim() || 'Unknown Student';
    const jobTitle = interview.jobTitle || 'Unknown Job';
    return `${student} — ${jobTitle}`;
  }

  isLocationRequired(): boolean {
    const interviewType = this.form.get('type')?.value;
    return interviewType === 'VIDEO' || interviewType === 'IN_PERSON';
  }

  showResultDropdown(): boolean {
    return this.editResultForm.get('status')?.value === 'COMPLETED';
  }

  showCancellationReason(): boolean {
    return this.editResultForm.get('status')?.value === 'CANCELLED';
  }

  canMarkAsDone(interview: InterviewResource): boolean {
    return interview.status === 'SCHEDULED';
  }

  formatType(type: string | undefined): string {
    if (!type) {
      return 'UNKNOWN';
    }
    return type === 'IN_PERSON' ? 'IN PERSON' : type;
  }

  // Helper methods for form validation errors
  getFieldError(fieldName: string): string {
    const field = this.form.get(fieldName);
    if (!field || !field.errors || !field.touched) return '';

    if (field.errors['required']) return `${this.formatLabel(fieldName)} is required`;
    if (field.errors['pastDate'])
      return `${this.formatLabel(fieldName)} cannot be in the past`;
    if (field.errors['maxlength'])
      return `${this.formatLabel(fieldName)} cannot exceed ${field.errors['maxlength'].requiredLength} characters`;
    return '';
  }

  getResultFieldError(fieldName: string): string {
    const field = this.editResultForm.get(fieldName);
    if (!field || !field.errors || !field.touched) return '';

    if (field.errors['required']) return `${this.formatLabel(fieldName)} is required`;
    if (field.errors['maxlength'])
      return `${this.formatLabel(fieldName)} cannot exceed ${field.errors['maxlength'].requiredLength} characters`;
    return '';
  }

  private formatLabel(fieldName: string): string {
    return fieldName.charAt(0).toUpperCase() + fieldName.slice(1).replace(/([A-Z])/g, ' $1');
  }

  private splitName(fullName: string | undefined): [string, string] {
    if (!fullName || !fullName.trim()) {
      return ['Unknown', ''];
    }

    const parts = fullName.trim().split(/\s+/);
    if (parts.length === 1) {
      return [parts[0], ''];
    }

    const firstName = parts[0];
    const lastName = parts.slice(1).join(' ');
    return [firstName, lastName];
  }

  // --- INTERVIEW JOIN LOGIC ---

  isInterviewJoinable(interview: InterviewResource): boolean {
    if (!interview || interview.type !== 'VIDEO') return false;
    
    const now = new Date();
    const interviewDate = new Date(interview.interviewDate);
    
    // Calculate difference in minutes
    const diffMinutes = (interviewDate.getTime() - now.getTime()) / (1000 * 60);
    
    // Joinable if it's within 15 minutes before, or it has already started
    return diffMinutes <= 15;
  }

  getTimeUntilInterviewMessage(interview: InterviewResource): string {
    if (!interview || interview.type !== 'VIDEO') return '';

    const now = new Date();
    const interviewDate = new Date(interview.interviewDate);
    const diffMs = interviewDate.getTime() - now.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    const diffHours = Math.floor((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    const diffMinutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));

    if (diffMs <= 0) {
      return 'Starting now!';
    }

    if (diffDays > 0) {
      return `Opens in ${diffDays}d`;
    }

    if (diffHours > 0) {
      return `Opens in ${diffHours}h`;
    }

    return `Opens in ${diffMinutes}m`;
  }

  joinInterview(interviewId: number | undefined): void {
    if (!interviewId) return;
    // Route to the video room component
    this.router.navigate(['/interview-room', interviewId]);
  }
}


