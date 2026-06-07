import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import { SrvApiService } from '../../../services';
import { DeliverableResponse } from '@esprit-market/api-types';
import { resolveHttpError } from '../../../shared/utils';

@Component({
  selector: 'app-deliverable-submit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, HeaderComponent, FooterComponent],
  templateUrl: './deliverable-submit.component.html',
  styleUrls: []
})
export class DeliverableSubmitComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  readonly router = inject(Router);
  private readonly srvApi = inject(SrvApiService);
  private readonly fb = inject(FormBuilder);

  readonly bookingId = signal<number | null>(null);
  readonly isLoading = signal(false);
  readonly isSubmitting = signal(false);
  readonly submitted = signal(false);
  readonly error = signal('');
  readonly selectedFiles = signal<File[]>([]);
  readonly uploadProgress = signal(0);
  readonly legacySunsetDate = '2026-06-30';

  form!: FormGroup;

  ngOnInit(): void {
    var id = this.route.snapshot.queryParamMap.get('bookingId');
    this.bookingId.set(id ? Number(id) : null);
    this.form = this.fb.group({
      title: ['', [Validators.required]],
      description: ['']
    });
  }

  onFilesSelected(event: Event): void {
    var input = event.target as HTMLInputElement;
    if (input.files) {
      var files = Array.from(input.files);
      this.selectedFiles.set(files);
    }
  }

  removeFile(index: number): void {
    var files = this.selectedFiles().filter((_, i) => i !== index);
    this.selectedFiles.set(files);
  }

  submit(): void {
    if (!this.form.valid || !this.bookingId()) return;

    this.isSubmitting.set(true);
    this.error.set('');
    this.uploadProgress.set(0);

    var formValue = this.form.value;
    var files = this.selectedFiles();

    this.uploadProgress.set(30);

    this.srvApi.createDeliverable(
      this.bookingId()!,
      formValue.title,
      formValue.description || '',
      files.length > 0 ? files : undefined
    ).subscribe({
      next: (deliverable: DeliverableResponse) => {
        this.uploadProgress.set(70);
        if (deliverable.id) {
          this.srvApi.submitDeliverable(deliverable.id).subscribe({
            next: () => {
              this.uploadProgress.set(100);
              this.isSubmitting.set(false);
              this.submitted.set(true);
            },
            error: (err) => {
              this.isSubmitting.set(false);
              this.error.set(resolveHttpError(err, 'Could not submit the deliverable. Please try again.'));
            }
          });
        } else {
          this.isSubmitting.set(false);
          this.submitted.set(true);
        }
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.error.set(resolveHttpError(err, 'Could not create the deliverable. Please try again.'));
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/bookings']);
  }

  openWorkspace(): void {
    if (!this.bookingId()) return;
    this.router.navigate(['/bookings', this.bookingId()], { queryParams: { source: 'legacy-deliverable-page' } });
  }
}
