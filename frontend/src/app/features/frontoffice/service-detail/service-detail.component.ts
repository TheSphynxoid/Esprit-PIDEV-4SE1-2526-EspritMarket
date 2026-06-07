import { Component, computed, inject, signal, ViewChild, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import { SrvApiService, AuthService } from '../../../services';
import { ServiceResponse as ServiceResource, ServiceReviewResponse as ServiceReviewResource, ServicePackageResponse } from '@esprit-market/api-types';
import { AvailabilitySlotPickerComponent } from './availability-slot-picker.component';
import { resolveHttpError } from '../../../shared/utils';

@Component({
  selector: 'app-service-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, HeaderComponent, FooterComponent, AvailabilitySlotPickerComponent],
  templateUrl: './service-detail.component.html',
  styleUrls: []
})
export class ServiceDetailComponent implements OnInit {
  private static readonly MAX_BOOKING_DURATION_HOURS = 8;

  private readonly route = inject(ActivatedRoute);
  readonly router = inject(Router);
  private readonly srvApi = inject(SrvApiService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);

  @ViewChild(AvailabilitySlotPickerComponent) slotPicker!: AvailabilitySlotPickerComponent;

  readonly service = signal<ServiceResource | null>(null);
  readonly reviews = signal<ServiceReviewResource[]>([]);
  readonly isFavorite = signal(false);
  readonly isLoading = signal(true);
  readonly bookingSubmitted = signal(false);
  readonly bookingError = signal('');
  readonly showBookingForm = signal(false);
  readonly estimatedTotal = signal<number | null>(null);
  readonly priorityMarkupEstimate = signal<number | null>(null);

  readonly selectedSlotStart = signal<string | null>(null);
  readonly selectedSlotEnd = signal<string | null>(null);
  readonly slotDurationMinutes = signal(60);
  readonly selectedDuration = signal(1);
  readonly dateChecking = signal(false);
  readonly dateAvailable = signal<boolean | null>(null);
  readonly dateMessage = signal('');
  readonly selectedPackageId = signal<number | null>(null);
  readonly selectedPkg = signal<ServicePackageResponse | null>(null);

  readonly providerStanding = signal<{
    providerId?: number;
    providerName?: string;
    totalBookings?: number;
    completedBookings?: number;
    cancelledBookings?: number;
    disputedBookings?: number;
    activeBookings?: number;
    completionRate?: number;
    cancellationRate?: number;
    disputeRate?: number;
    totalReviews?: number;
    averageRating?: number;
    activeServices?: number;
    mlReliabilityScore?: number;
    mlRiskLevel?: string;
    mlConfidence?: string;
    mlKeyFactors?: string[];
    mlRecommendation?: string;
    xp?: number;
    level?: string;
    levelNumber?: number;
  } | null>(null);
  readonly standingLoading = signal(false);
  readonly compatibility = signal<any>(null);
  readonly sentimentResults = signal<Record<number, any>>({});
  readonly categoryDemand = signal<any>(null);

  readonly durationStep = computed(() => this.slotDurationMinutes() / 60);
  readonly durationMin = computed(() => this.slotDurationMinutes() / 60);
  readonly durationOptions = computed(() => {
    const step = this.durationStep();
    const opts: number[] = [];
    for (let i = step; i <= ServiceDetailComponent.MAX_BOOKING_DURATION_HOURS; i += step) {
      opts.push(Math.round(i * 100) / 100);
    }
    return opts;
  });

  bookingForm!: FormGroup;
  private formSub: any;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.bookingForm = this.fb.group({
      date: [''],
      duration: [1, [Validators.required, Validators.min(0.5), Validators.max(ServiceDetailComponent.MAX_BOOKING_DURATION_HOURS)]],
      notes: [''],
      highPriority: [false]
    });
    this.formSub = this.bookingForm.get('duration')!.valueChanges.subscribe((val) => {
      this.selectedDuration.set(Number(val) || 1);
      this.updateEstimate();
    });
    this.bookingForm.get('highPriority')!.valueChanges.subscribe(() => {
      this.updateEstimate();
    });
    this.bookingForm.get('date')!.valueChanges.subscribe((val) => {
      if (val && this.service()?.pricingType === 'FIXED') {
        this.checkFixedDateAvailability(val);
      } else {
        this.dateAvailable.set(null);
        this.dateMessage.set('');
      }
    });
    this.loadService(id);
  }

  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  today(): string {
    return new Date().toISOString().split('T')[0];
  }

  get canSubmit(): boolean {
    const svc = this.service();
    if (svc?.pricingType === 'PACKAGED') {
      return this.bookingForm.valid && this.selectedPackageId() !== null;
    }
    if (svc?.pricingType === 'FIXED') {
      return this.bookingForm.valid && this.dateAvailable() === true;
    }
    return this.bookingForm.valid && this.selectedSlotStart() !== null;
  }

  selectPackage(pkg: ServicePackageResponse): void {
    this.selectedPackageId.set(pkg.id ?? null);
    this.selectedPkg.set(pkg);
    this.bookingError.set('');
  }

  packagePriorityMarkup(): number {
    const pkg = this.selectedPkg();
    if (!pkg?.price) return 0;
    return Math.round(pkg.price * 0.12 * 100) / 100;
  }

  get submitButtonText(): string {
    const svc = this.service();
    if (!svc) return 'Submit';
    if (svc.pricingType === 'PACKAGED') {
      if (!this.selectedPkg()) return 'Select a package';
      const hp = this.bookingForm.get('highPriority')?.value;
      const pkgPrice = this.selectedPkg()?.price ?? 0;
      const total = pkgPrice + (hp ? this.packagePriorityMarkup() : 0);
      return 'Continue (' + total + ' TND)';
    }
    if (svc.pricingType === 'FIXED') return 'Confirm Booking';
    return this.selectedSlotStart() ? 'Confirm Booking' : 'Select a time slot above';
  }

  toggleBookingForm(): void {
    if (!this.isLoggedIn) {
      this.router.navigate(['/login']);
      return;
    }
    this.showBookingForm.update(v => !v);
    this.bookingSubmitted.set(false);
    this.bookingError.set('');
  }

  onSlotSelected(event: { start: string; end: string; slotDurationMinutes: number }): void {
    this.selectedSlotStart.set(event.start);
    this.selectedSlotEnd.set(event.end);
    this.slotDurationMinutes.set(event.slotDurationMinutes);
    const minDuration = event.slotDurationMinutes / 60;
    this.bookingForm.get('duration')?.setValue(minDuration);
    this.bookingError.set('');
  }

  submitBooking(): void {
    if (!this.canSubmit || !this.service()) return;

    const userId = this.authService.currentUser()?.id;
    if (!userId) {
      this.router.navigate(['/login']);
      return;
    }

    const formValue = this.bookingForm.value;
    const svc = this.service()!;
    const slotStart = this.selectedSlotStart();

    if (svc.pricingType === 'PACKAGED') {
      if (!this.selectedPackageId()) return;
      const now = new Date();
      now.setDate(now.getDate() + 2); // Ensure it's definitely in the future
      const bookingDate = now.toISOString().slice(0, 19);
      this.srvApi.createBooking({
        date: bookingDate,
        duration: 1,
        notes: formValue.notes || undefined,
        serviceId: svc.id!,
        highPriority: !!formValue.highPriority,
        packageId: this.selectedPackageId()!
      }).subscribe({
        next: () => {
          this.bookingSubmitted.set(true);
          this.bookingError.set('');
          this.selectedPackageId.set(null);
          this.selectedPkg.set(null);
          this.bookingForm.reset({ date: '', duration: 1, notes: '' });
        },
        error: (err) => {
          this.bookingError.set(resolveHttpError(err, 'Booking failed. Please try again.'));
        }
      });
      return;
    }

    if (svc.pricingType === 'FIXED' && !formValue.date) return;
    if (svc.pricingType === 'HOURLY' && !slotStart) return;

    let bookingDate = svc.pricingType === 'FIXED' ? formValue.date : slotStart;
    if (svc.pricingType === 'FIXED') {
      if (!bookingDate) return;
      // Fixed pricing defaults to a full 24h duration internally, but 1 day in the UI
      if (bookingDate.length === 10) {
         // Create a proper datetime in the future
         const d = new Date(bookingDate);
         d.setHours(23, 59, 59, 0);
         bookingDate = d.toISOString().slice(0, 19);
      }
    }
    const duration = svc.pricingType === 'FIXED' ? 24 : Number(formValue.duration);

    this.srvApi.createBooking({
      date: bookingDate,
      duration: duration,
      notes: formValue.notes || undefined,
      serviceId: svc.id!,
      highPriority: !!formValue.highPriority
    }).subscribe({
      next: () => {
        this.bookingSubmitted.set(true);
        this.bookingError.set('');
        this.selectedSlotStart.set(null);
        this.selectedSlotEnd.set(null);
        this.selectedPackageId.set(null);
        this.selectedPkg.set(null);
        this.bookingForm.reset({ date: '', duration: 1, notes: '' });
        this.dateAvailable.set(null);
        this.dateMessage.set('');
        this.slotPicker?.reloadSlots();
      },
      error: (err) => {
        this.bookingError.set(resolveHttpError(err, 'Booking failed. Please try again.'));
        this.slotPicker?.reloadSlots();
      }
    });
  }

  private checkFixedDateAvailability(dateStr: string): void {
    const svc = this.service();
    if (!svc?.id) return;
    this.dateChecking.set(true);
    this.dateAvailable.set(null);
    this.dateMessage.set('');
    this.bookingError.set('');
    this.srvApi.getAvailableSlots(svc.id, dateStr, dateStr).subscribe({
      next: (slots) => {
        this.dateChecking.set(false);
        if (slots && slots.length > 0) {
          this.dateAvailable.set(true);
          this.dateMessage.set('');
        } else {
          this.dateAvailable.set(false);
          this.dateMessage.set('The provider is not available on this date. Please choose another day.');
        }
      },
      error: () => {
        this.dateChecking.set(false);
        this.dateAvailable.set(false);
        this.dateMessage.set('Unable to check availability. Please try another date.');
      }
    });
  }

  toggleFavorite(): void {
    const svc = this.service();
    if (!svc) return;
    if (!this.isLoggedIn) {
      this.router.navigate(['/login']);
      return;
    }

    if (this.isFavorite()) {
      this.srvApi.removeFavorite(svc.id!).subscribe({
        next: () => { this.isFavorite.set(false); }
      });
    } else {
      this.srvApi.addFavorite(svc.id!).subscribe({
        next: () => { this.isFavorite.set(true); }
      });
    }
  }

  private loadService(id: number): void {
    this.isLoading.set(true);
    this.srvApi.getServiceById(id).subscribe({
      next: (service) => {
        this.service.set(service);
        this.isLoading.set(false);
        this.updateEstimate();
        this.loadReviews(id);
        this.checkFavorite(id);
        if (service.providerId) {
          this.loadProviderStanding(service.providerId);
          this.loadCompatibility(service.providerId);
        }
        this.loadCategoryDemand();
      },
      error: () => {
        this.isLoading.set(false);
        this.service.set(null);
      }
    });
  }

  private loadReviews(serviceId: number): void {
    this.srvApi.getServiceReviews(serviceId, 0, 50).subscribe({
      next: (page) => {
        this.reviews.set(page.content);
        // Map built-in sentiment fields to the existing sentimentResults signal
        const map: Record<number, any> = {};
        page.content.forEach((r: any) => {
          if (r.sentiment) {
            map[r.id] = { sentiment: r.sentiment, confidence: r.sentimentConfidence };
          }
        });
        this.sentimentResults.set(map);
      },
      error: () => {
        this.reviews.set([]);
      }
    });
  }

  private checkFavorite(serviceId: number): void {
    if (!this.isLoggedIn) return;
    this.srvApi.isFavorite(serviceId).subscribe({
      next: (fav) => { this.isFavorite.set(fav); },
      error: () => { this.isFavorite.set(false); }
    });
  }

  private loadProviderStanding(providerId: number): void {
    this.standingLoading.set(true);
    this.srvApi.getProviderStanding(providerId).subscribe({
      next: (standing) => {
        this.providerStanding.set(standing);
        this.standingLoading.set(false);
      },
      error: () => {
        this.standingLoading.set(false);
      }
    });
  }

  private loadCompatibility(providerId: number): void {
    if (!this.isLoggedIn) return;
    const clientId = this.authService.currentUser()?.id;
    if (!clientId) return;
    this.http.get(`/api/srv/services/provider/${providerId}/compatibility/${clientId}`).subscribe({
      next: (data: any) => this.compatibility.set(data),
      error: () => this.compatibility.set(null)
    });
  }

  private updateEstimate(): void {
    const svc = this.service();
    const duration = this.bookingForm?.get('duration')?.value;
    if (!svc || !duration || duration <= 0) {
      this.estimatedTotal.set(null);
      this.priorityMarkupEstimate.set(null);
      return;
    }
    const total = svc.price! * Number(duration);
    this.estimatedTotal.set(Math.round(total * 100) / 100);

    const isHighPriority = this.bookingForm?.get('highPriority')?.value;
    if (isHighPriority) {
      this.priorityMarkupEstimate.set(Math.round(total * 0.12 * 100) / 100);
    } else {
      this.priorityMarkupEstimate.set(null);
    }
  }

  private loadCategoryDemand(): void {
    this.srvApi.getSurgePricing().subscribe({
      next: (data) => {
        const svc = this.service();
        if (svc?.category && data?.categories) {
          const match = data.categories.find((c: any) => c.category === svc.category);
          if (match) this.categoryDemand.set(match);
        }
      },
      error: () => {}
    });
  }

  demandBadgeClass(badge: string): string {
    switch (badge) {
      case 'CRITICAL': return 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400';
      case 'HIGH': return 'bg-orange-100 dark:bg-orange-900/30 text-orange-700 dark:text-orange-400';
      case 'MODERATE': return 'bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400';
      default: return 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400';
    }
  }
}
