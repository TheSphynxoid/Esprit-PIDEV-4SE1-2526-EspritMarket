import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import { SrvApiService, AuthService } from '../../../services';
import {
  DeliverableResponse,
  DeliverableReviewResponse,
  DeliverableAttachmentResponse
} from '@esprit-market/api-types';
import { deliverableStatusClass, resolveHttpError } from '../../../shared/utils';

@Component({
  selector: 'app-deliverable-review',
  standalone: true,
  imports: [CommonModule, FormsModule, HeaderComponent, FooterComponent],
  templateUrl: './deliverable-review.component.html',
  styleUrls: []
})
export class DeliverableReviewComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  readonly router = inject(Router);
  private readonly srvApi = inject(SrvApiService);
  private readonly authService = inject(AuthService);

  readonly deliverableId = signal<number | null>(null);
  readonly deliverable = signal<DeliverableResponse | null>(null);
  readonly history = signal<DeliverableReviewResponse[]>([]);
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly submitted = signal(false);
  readonly error = signal('');
  readonly legacySunsetDate = '2026-06-30';
  readonly decision = signal<string>('');
  readonly comment = signal('');

  DeliverableStatus = DeliverableResponse.StatusEnum;

  ngOnInit(): void {
    var id = this.route.snapshot.paramMap.get('id');
    this.deliverableId.set(id ? Number(id) : null);
    if (this.deliverableId()) {
      this.loadDeliverable();
      this.loadHistory();
    }
  }

  getStatusClass(status: string | undefined): string {
    return deliverableStatusClass(status);
  }

  getDecisionClass(decision: string | undefined): string {
    return deliverableStatusClass(decision);
  }

  submitReview(): void {
    if (!this.decision() || !this.deliverableId()) return;

    this.isSubmitting.set(true);
    this.error.set('');

    this.srvApi.reviewDeliverable(this.deliverableId()!, this.decision(), this.comment() || undefined).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.submitted.set(true);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.error.set(resolveHttpError(err, 'Could not submit the review. Please try again.'));
      }
    });
  }

  private loadDeliverable(): void {
    var id = this.deliverableId();
    if (!id) return;
    this.srvApi.getDeliverableById(id).subscribe({
      next: (del) => {
        this.deliverable.set(del);
        this.isLoading.set(false);
      },
      error: () => {
        this.deliverable.set(null);
        this.isLoading.set(false);
      }
    });
  }

  private loadHistory(): void {
    var id = this.deliverableId();
    if (!id) return;
    this.srvApi.getDeliverableHistory(id).subscribe({
      next: (reviews) => { this.history.set(reviews); },
      error: () => { this.history.set([]); }
    });
  }

  openWorkspace(): void {
    const bookingId = this.deliverable()?.bookingId;
    if (!bookingId) return;
    this.router.navigate(['/bookings', bookingId], { queryParams: { source: 'legacy-deliverable-page' } });
  }
}
