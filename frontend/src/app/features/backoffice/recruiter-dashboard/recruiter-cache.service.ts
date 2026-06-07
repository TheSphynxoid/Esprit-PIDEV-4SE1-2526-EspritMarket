import { Injectable, inject } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { PartnershipApiService } from '../../../services/api/partnership-api.service';
import {
  ApplicationResource,
  InterviewResource,
  JobOfferResource,
  PartnerCompanyResource
} from '../../../services/api/models/api-resource.model';

@Injectable({ providedIn: 'root' })
export class RecruiterCacheService {
  private partnershipApi = inject(PartnershipApiService);

  // ── Data streams (subscribed to by child components) ───────────
  readonly companies$    = new BehaviorSubject<PartnerCompanyResource[]>([]);
  readonly jobOffers$    = new BehaviorSubject<JobOfferResource[]>([]);
  readonly applications$ = new BehaviorSubject<ApplicationResource[]>([]);
  readonly interviews$   = new BehaviorSubject<InterviewResource[]>([]);

  // ── Error streams (subscribed to by child components) ──────────
  readonly companiesError$    = new BehaviorSubject<string>('');
  readonly jobOffersError$    = new BehaviorSubject<string>('');
  readonly applicationsError$ = new BehaviorSubject<string>('');
  readonly interviewsError$   = new BehaviorSubject<string>('');

  /** Fires all four API calls in parallel the moment the dashboard opens. */
  preloadAll(): void {
    this.refreshCompanies();
    this.refreshJobOffers();
    this.refreshApplications();
    this.refreshInterviews();
  }

  // ── Refresh helpers ─────────────────────────────────────────────
  // Each method makes a fresh GET and pushes the result into the
  // matching BehaviorSubject so every subscribed component updates instantly.
  // Loading state is intentionally NOT managed here — each component
  // sets this.loading = true before calling refresh, then the data
  // subscription sets this.loading = false when the response arrives.

  refreshCompanies(): void {
    this.companiesError$.next('');
    this.partnershipApi.companies.list().subscribe({
      next: (data) => this.companies$.next(data),
      error: (err) => this.companiesError$.next('Failed to load companies')
    });
  }

  refreshJobOffers(): void {
    this.jobOffersError$.next('');
    this.partnershipApi.jobOffers.list().subscribe({
      next: (data) => this.jobOffers$.next(data),
      error: (err) => this.jobOffersError$.next('Failed to load job offers')
    });
  }

  refreshApplications(): void {
    this.applicationsError$.next('');
    this.partnershipApi.applications.list().subscribe({
      next: (data) => this.applications$.next(data),
      error: (err) => {
        const msg =
          err.status === 401 ? 'Authentication failed. Please log in again.'
          : err.status === 403 ? 'You do not have permission to view applications.'
          : err.status === 0   ? 'Cannot reach the backend server. Is it running on port 8088?'
          : `Failed to load applications. Error: ${err.status} ${err.statusText}`;
        this.applicationsError$.next(msg);
      }
    });
  }

  refreshInterviews(): void {
    this.interviewsError$.next('');
    this.partnershipApi.interviews.list().subscribe({
      next: (data) => this.interviews$.next(data),
      error: (err) => {
        const msg =
          err.status === 401 ? 'Authentication failed. Please log in again.'
          : err.status === 403 ? 'You do not have permission to view interviews.'
          : err.status === 0   ? 'Cannot reach the backend server. Is it running on port 8088?'
          : `Failed to load interviews. Error: ${err.status} ${err.statusText}`;
        this.interviewsError$.next(msg);
      }
    });
  }
}
