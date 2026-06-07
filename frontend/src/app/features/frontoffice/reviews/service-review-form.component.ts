import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import { StarRatingComponent } from '../../../shared/components';
import { SrvApiService, AuthService } from '../../../services';
import { ServiceReviewResponse as ServiceReviewResource, BookingResponse as BookingResource } from '@esprit-market/api-types';
import { resolveHttpError } from '../../../shared/utils';

@Component({
  selector: 'app-service-review-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, HeaderComponent, FooterComponent, StarRatingComponent],
  templateUrl: './service-review-form.component.html',
  styleUrls: []
})
export class ServiceReviewFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  readonly router = inject(Router);
  private readonly srvApi = inject(SrvApiService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly bookingId = signal<number | null>(null);
  readonly booking = signal<BookingResource | null>(null);
  readonly isSubmitting = signal(false);
  readonly submitted = signal(false);
  readonly error = signal('');

  form!: FormGroup;

  ngOnInit(): void {
    const id = this.route.snapshot.queryParamMap.get('bookingId');
    this.bookingId.set(id ? Number(id) : null);
    this.form = this.fb.group({
      rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
      comment: ['']
    });
    if (this.bookingId()) {
      this.loadBooking();
    }
  }

  submit(): void {
    if (!this.form.valid || !this.bookingId()) return;

    this.isSubmitting.set(true);
    const formValue = this.form.value;
    this.srvApi.createServiceReview({
      rating: Number(formValue.rating),
      comment: formValue.comment || undefined,
      bookingId: this.bookingId()!
    }).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.submitted.set(true);
        this.error.set('');
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.error.set(resolveHttpError(err, 'Could not submit your review. Please try again.'));
      }
    });
  }

  private loadBooking(): void {
    const id = this.bookingId();
    if (!id) return;
    this.srvApi.getBookingById(id).subscribe({
      next: (booking) => { this.booking.set(booking); },
      error: () => { this.booking.set(null); }
    });
  }
}
