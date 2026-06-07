import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import { SkeletonComponent, StarRatingComponent } from '../../../shared/components';
import { SrvApiService, AuthService } from '../../../services';
import { ApiConfigService } from '../../../services/api';
import { BookingResponse } from '@esprit-market/api-types';
import { bookingStatusPillClass, formatStatusLabel, resolveHttpError } from '../../../shared/utils';

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, HeaderComponent, FooterComponent, SkeletonComponent, StarRatingComponent],
  templateUrl: './my-bookings.component.html',
  styleUrls: []
})
export class MyBookingsComponent implements OnInit {
  private readonly srvApi = inject(SrvApiService);
  private readonly authService = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);
  readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly bookings = signal<BookingResponse[]>([]);
  readonly allBookings = signal<BookingResponse[]>([]);
  readonly isLoading = signal(true);
  readonly reviewingBookingId = signal<number | null>(null);
  readonly reviewSubmitted = signal(false);
  readonly reviewError = signal('');
  readonly actionError = signal('');

  readonly bookingPredictions = signal<Map<number, {
    completionProbability?: number;
    riskLevel?: string;
    confidence?: string;
    keyFactors?: string[];
    recommendation?: string;
  }>>(new Map());

  readonly statusFilter = signal<string>('ALL');
  readonly currentPage = signal(0);
  readonly pageSize = 10;

  reviewForm!: FormGroup;

  BookingStatus = BookingResponse.StatusEnum;

  readonly statusOptions = ['ALL', 'PENDING', 'APPROVED', 'CONFIRMED', 'IN_PROGRESS', 'PENDING_REVIEW', 'COMPLETED', 'CANCELLED', 'REJECTED', 'DISPUTED'] as const;

  get filteredBookings(): BookingResponse[] {
    const filter = this.statusFilter();
    if (filter === 'ALL') return this.allBookings();
    return this.allBookings().filter(b => b.status === filter);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredBookings.length / this.pageSize));
  }

  get paginatedBookings(): BookingResponse[] {
    const start = this.currentPage() * this.pageSize;
    return this.filteredBookings.slice(start, start + this.pageSize);
  }

  get pageRange(): number[] {
    const total = this.totalPages;
    const current = this.currentPage();
    const start = Math.max(0, current - 2);
    const end = Math.min(total, current + 3);
    return Array.from({ length: end - start }, (_, i) => start + i);
  }

  ngOnInit(): void {
    this.reviewForm = this.fb.group({
      rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
      comment: ['']
    });
    this.loadBookings();
  }

  onStatusFilterChange(status: string): void {
    this.statusFilter.set(status);
    this.currentPage.set(0);
  }

  goToPage(page: number): void {
    this.currentPage.set(page);
  }

  canCancel(status: BookingResponse.StatusEnum | undefined): boolean {
    return status === BookingResponse.StatusEnum.Pending || status === BookingResponse.StatusEnum.Approved;
  }

  canReview(status: BookingResponse.StatusEnum | undefined): boolean {
    return status === BookingResponse.StatusEnum.Completed ||
           status === BookingResponse.StatusEnum.PendingReview;
  }

  cancelBooking(booking: BookingResponse): void {
    if (!confirm('Cancel this booking?')) return;
    this.srvApi.cancelBooking(booking.id!).subscribe({
      next: () => {
        this.bookings.update(list =>
          list.map((b) =>
            b.id === booking.id ? { ...b, status: BookingResponse.StatusEnum.Cancelled } : b
          )
        );
      }
    });
  }

  startReview(bookingId: number | undefined): void {
    if (bookingId == null) return;
    this.reviewingBookingId.set(bookingId);
    this.reviewSubmitted.set(false);
    this.reviewError.set('');
    this.reviewForm.reset({ rating: 5, comment: '' });
  }

  reviewDeliverable(booking: BookingResponse): void {
    if (booking.id == null) return;
    this.router.navigate(['/bookings', booking.id]);
  }

  downloadDeliverable(booking: BookingResponse): void {
    if (booking.id == null) return;
    this.actionError.set('');

    this.srvApi.getDeliverablesByBooking(booking.id).subscribe({
      next: (deliverables) => {
        const latest = deliverables[0];
        if (!latest?.id) {
          this.actionError.set('No deliverable has been submitted for this booking yet.');
          return;
        }

        this.srvApi.getDeliverableById(latest.id).subscribe({
          next: (detail) => {
            const attachment = detail.attachments?.[0];
            if (!attachment?.fileUrl) {
              this.actionError.set('No files are attached to this deliverable.');
              return;
            }
            this.downloadFile(attachment.fileUrl, attachment.fileName || 'deliverable-file');
          },
          error: (err) => {
            this.actionError.set(resolveHttpError(err, 'Could not load deliverable details.'));
          }
        });
      },
      error: (err) => {
        this.actionError.set(resolveHttpError(err, 'Could not load deliverables.'));
      }
    });
  }

  private downloadFile(fileUrl: string, fileName: string): void {
    this.http.get(this.apiConfig.buildAssetUrl(fileUrl), { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const objectUrl = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = objectUrl;
        anchor.download = fileName;
        document.body.appendChild(anchor);
        anchor.click();
        document.body.removeChild(anchor);
        URL.revokeObjectURL(objectUrl);
      },
      error: (err) => {
        this.actionError.set(resolveHttpError(err, 'Could not download the file.'));
      }
    });
  }

  cancelReview(): void {
    this.reviewingBookingId.set(null);
  }

  submitReview(): void {
    if (!this.reviewForm.valid || !this.reviewingBookingId()) return;

    const formValue = this.reviewForm.value;
    this.srvApi.createServiceReview({
      rating: Number(formValue.rating),
      comment: formValue.comment || undefined,
      bookingId: this.reviewingBookingId()!
    }).subscribe({
      next: () => {
        this.reviewSubmitted.set(true);
        this.reviewError.set('');
        this.reviewingBookingId.set(null);
      },
      error: (err) => {
        this.reviewError.set(resolveHttpError(err, 'Could not submit your review. Please try again.'));
      }
    });
  }

  private loadBookingPredictions(items: BookingResponse[]): void {
    const activeStatuses: string[] = ['PENDING', 'PENDING_EVALUATION', 'TENTATIVE', 'APPROVED', 'CONFIRMED', 'IN_PROGRESS'];
    const active = items.filter(b => activeStatuses.includes(b.status ?? ''));
    active.forEach(b => {
      if (b.id && !this.bookingPredictions().has(b.id)) {
        this.srvApi.getBookingMlPrediction(b.id).subscribe({
          next: (pred) => {
            const map = new Map(this.bookingPredictions());
            map.set(b.id!, pred);
            this.bookingPredictions.set(map);
          },
          error: () => {}
        });
      }
    });
  }

  getStatusClass(status: BookingResponse.StatusEnum | undefined): string {
    return bookingStatusPillClass(status);
  }

  private loadBookings(): void {
    const userId = this.authService.currentUser()?.id;
    if (!userId) {
      this.router.navigate(['/login']);
      return;
    }

    this.srvApi.getBookingsByUser(userId, 0, 100).subscribe({
      next: (page) => {
        this.allBookings.set(page.content);
        this.bookings.set(page.content);
        this.isLoading.set(false);
        this.loadBookingPredictions(page.content);
      },
      error: () => {
        this.allBookings.set([]);
        this.bookings.set([]);
        this.isLoading.set(false);
      }
    });
  }
}
