import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import { SrvApiService, AuthService } from '../../../services';
import {
  DeliverableResponse,
  DeliverableReviewResponse
} from '@esprit-market/api-types';
import { deliverableStatusClass, resolveHttpError } from '../../../shared/utils';

@Component({
  selector: 'app-deliverable-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, HeaderComponent, FooterComponent],
  templateUrl: './deliverable-detail.component.html',
  styleUrls: []
})
export class DeliverableDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  readonly router = inject(Router);
  private readonly srvApi = inject(SrvApiService);
  private readonly authService = inject(AuthService);

  readonly deliverableId = signal<number | null>(null);
  readonly deliverable = signal<DeliverableResponse | null>(null);
  readonly history = signal<DeliverableReviewResponse[]>([]);
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly error = signal('');
  readonly legacySunsetDate = '2026-06-30';

  DeliverableStatus = DeliverableResponse.StatusEnum;

  ngOnInit(): void {
    var id = this.route.snapshot.paramMap.get('id');
    this.deliverableId.set(id ? Number(id) : null);
    if (this.deliverableId()) {
      this.loadDeliverable();
      this.loadHistory();
    }
  }

  get isProvider(): boolean {
    var user = this.authService.currentUser();
    var del = this.deliverable();
    return !!user && !!del && user.id === del.providerId;
  }

  get isClient(): boolean {
    var user = this.authService.currentUser();
    return !!user && !this.isProvider;
  }

  canSubmit(): boolean {
    var del = this.deliverable();
    return this.isProvider && !!del && (
      del.status === DeliverableResponse.StatusEnum.Draft ||
      del.status === DeliverableResponse.StatusEnum.RevisionRequested
    );
  }

  canReview(): boolean {
    var del = this.deliverable();
    return this.isClient && !!del && del.status === DeliverableResponse.StatusEnum.Submitted;
  }

  getStatusClass(status: string | undefined): string {
    return deliverableStatusClass(status);
  }

  getDecisionClass(decision: string | undefined): string {
    return deliverableStatusClass(decision);
  }

  submitDeliverable(): void {
    var id = this.deliverableId();
    if (!id) return;
    this.isSubmitting.set(true);
    this.srvApi.submitDeliverable(id).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.loadDeliverable();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.error.set(resolveHttpError(err, 'Could not submit the deliverable. Please try again.'));
      }
    });
  }

  onAddAttachment(event: Event): void {
    var input = event.target as HTMLInputElement;
    var id = this.deliverableId();
    if (!input.files || !input.files[0] || !id) return;
    var file = input.files[0];
    this.srvApi.addDeliverableAttachment(id, file).subscribe({
      next: () => { this.loadDeliverable(); },
      error: (err) => { this.error.set(resolveHttpError(err, 'Could not add the file. Please try again.')); }
    });
  }

  deleteAttachment(attachmentId: number | undefined): void {
    var id = this.deliverableId();
    if (!attachmentId || !id) return;
    this.srvApi.deleteDeliverableAttachment(id, attachmentId).subscribe({
      next: () => { this.loadDeliverable(); },
      error: (err) => { this.error.set(resolveHttpError(err, 'Could not remove the file. Please try again.')); }
    });
  }

  goBack(): void {
    this.router.navigate(['/bookings']);
  }

  openWorkspace(): void {
    const bookingId = this.deliverable()?.bookingId;
    if (!bookingId) return;
    this.router.navigate(['/bookings', bookingId], { queryParams: { source: 'legacy-deliverable-page' } });
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
}
