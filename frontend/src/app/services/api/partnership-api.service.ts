import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { CrudApiService } from './crud-api.service';
import { ApiConfigService } from './api-config.service';
import {
  PartnerCompanyResource,
  PartnerCompanyWritePayload,
  JobOfferResource,
  JobOfferWritePayload,
  ApplicationResource,
  ApplicationWritePayload,
  InterviewResource,
  InterviewWritePayload,
  JobOfferPerformanceDTO
} from './models/api-resource.model';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

@Injectable({ providedIn: 'root' })
export class PartnershipApiService {
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);

  readonly companies = new CrudApiService<PartnerCompanyResource, PartnerCompanyWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/partnership/companies')
  );

  readonly jobOffers = new CrudApiService<JobOfferResource, JobOfferWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/partnership/joboffers')
  );

  readonly applications = new CrudApiService<ApplicationResource, ApplicationWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/partnership/applications')
  );

  getApplicationsForInterviewForm(): Observable<ApplicationResource[]> {
    return this.http.get<ApplicationResource[]>(
      this.apiConfig.buildUrl('/api/partnership/applications')
    );
  }

  applyWithProfile(applicationData: any): Observable<ApplicationResource> {
    return this.http.post<ApplicationResource>(
      this.apiConfig.buildUrl('/api/partnership/applications/apply'),
      applicationData
    );
  }

  // ── Inactivity Detection ───────────────────────────────

  getFlaggedApplications(): Observable<ApplicationResource[]> {
    return this.http.get<ApplicationResource[]>(
      this.apiConfig.buildUrl('/api/partnership/applications/inactive')
    );
  }

  refreshCandidateActivity(applicationId: number): Observable<void> {
    return this.http.patch<void>(
      this.apiConfig.buildUrl(`/api/partnership/applications/${applicationId}/refresh-activity`),
      {}
    );
  }

  readonly interviews = new CrudApiService<InterviewResource, InterviewWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/partnership/interviews')
  );

  suggestInterviewSlots(applicationId: number): Observable<string[]> {
    return this.http.get<string[]>(
      this.apiConfig.buildUrl(`/api/partnership/interviews/suggest-slots`),
      { params: new HttpParams().set('applicationId', applicationId.toString()) }
    );
  }

  // TASK 2: Complex JPQL with Joins - Frontend Service Method
  // Fetches job offer performance report with aggregated metrics
  getJobOfferPerformanceReport(): Observable<JobOfferPerformanceDTO[]> {
    return this.http.get<JobOfferPerformanceDTO[]>(
      this.apiConfig.buildUrl('/api/partnership/joboffers/performance-report')
    );
  }

  // TASK 3: Keywords function - Advanced Job Search
  // Search job offers by title keyword, company sector, and status
  // Demonstrates multi-table JPA keyword queries on frontend consumption
  searchJobOffers(
    keyword: string = '',
    type: string = '',
    location: string = '',
    experienceLevel: string = '',
    page: number = 0,
    size: number = 10,
    sort: string = 'id,desc'
  ): Observable<Page<JobOfferResource>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (keyword && keyword.trim() !== '') {
      params = params.set('keyword', keyword);
    }
    if (type && type.trim() !== '') {
      params = params.set('type', type);
    }
    if (location && location.trim() !== '') {
      params = params.set('location', location);
    }
    if (experienceLevel && experienceLevel !== '') {
      params = params.set('experienceLevel', experienceLevel);
    }

    return this.http.get<Page<JobOfferResource>>(
      this.apiConfig.buildUrl('/api/partnership/joboffers/search'),
      { params }
    );
  }

  // Search jobs by type - returns only from approved partner companies
  getJobsByType(type: string): Observable<JobOfferResource[]> {
    const params = new HttpParams().set('type', type);
    return this.http.get<JobOfferResource[]>(
      this.apiConfig.buildUrl('/api/partnership/joboffers/by-type'),
      { params }
    );
  }

  // Fetch applications for a specific student
  getStudentApplications(studentId: number): Observable<ApplicationResource[]> {
    return this.http.get<ApplicationResource[]>(
      this.apiConfig.buildUrl(`/api/partnership/applications/student/${studentId}`)
    );
  }
}
