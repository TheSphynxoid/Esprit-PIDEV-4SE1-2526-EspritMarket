import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApiConfigService } from './api-config.service';
import { PageResponse } from './models/api-resource.model';
import {
  ServiceResponse,
  ServiceUpsertRequest,
  BookingResponse,
  BookingRequest,
  BookingStatusUpdateRequest,
  ServiceReviewResponse,
  ServiceReviewRequest,
  PartnerResponse,
  PartnerRequest,
  ProjectResponse,
  ProjectRequest,
  TimeSlotDto,
  WeeklyTemplateResponse,
  WeeklyTemplateRequest,
  WeeklyTemplateBatchRequest,
  ProviderExceptionResponse,
  ProviderExceptionRequest,
  ServiceMandateResponse,
  ServiceMandateRequest,
  ProviderMandateResponse,
  ProviderMandateRequest,
  DeliverableResponse,
  DeliverableCreateRequest,
  DeliverableReviewRequest,
  DeliverableReviewResponse,
  DeliverableAttachmentResponse,
  BookingAttachmentResponse,
  ProjectMilestoneRequest,
  ProjectMilestoneResponse,
  ProjectDependencyRequest,
  ProjectDependencyResponse,
  ProjectTimelineResponse
} from '@esprit-market/api-types';

@Injectable({ providedIn: 'root' })
export class SrvApiService {
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);

  private url(path: string): string {
    return this.apiConfig.buildUrl(`/api/srv${path}`);
  }

  getServices(page = 0, size = 20): Observable<PageResponse<ServiceResponse>> {
    return this.http.get<PageResponse<ServiceResponse>>(this.url('/services'), {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  getServiceById(id: number): Observable<ServiceResponse> {
    return this.http.get<ServiceResponse>(this.url(`/services/${id}`));
  }

  getServicesByProvider(providerId: number, page = 0, size = 20): Observable<PageResponse<ServiceResponse>> {
    return this.http.get<PageResponse<ServiceResponse>>(this.url(`/services/provider/${providerId}`), {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  getServicesByCategory(category: ServiceResponse.CategoryEnum, page = 0, size = 20): Observable<PageResponse<ServiceResponse>> {
    return this.http.get<PageResponse<ServiceResponse>>(this.url(`/services/category/${category}`), {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  getRelatedServices(serviceId: number, page = 0, size = 6): Observable<PageResponse<ServiceResponse>> {
    return this.http.get<PageResponse<ServiceResponse>>(this.url(`/services/${serviceId}/related`), {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  searchServices(keyword: string, page = 0, size = 20): Observable<PageResponse<ServiceResponse>> {
    return this.http.get<PageResponse<ServiceResponse>>(this.url('/services/search'), {
      params: new HttpParams().set('keyword', keyword).set('page', page).set('size', size)
    });
  }

  filterServices(params: {
    category?: ServiceResponse.CategoryEnum;
    minPrice?: number;
    maxPrice?: number;
    location?: string;
    page?: number;
    size?: number;
  }): Observable<PageResponse<ServiceResponse>> {
    let httpParams = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20));
    if (params.category) httpParams = httpParams.set('category', params.category);
    if (params.minPrice !== undefined) httpParams = httpParams.set('minPrice', String(params.minPrice));
    if (params.maxPrice !== undefined) httpParams = httpParams.set('maxPrice', String(params.maxPrice));
    if (params.location) httpParams = httpParams.set('location', params.location);
    return this.http.get<PageResponse<ServiceResponse>>(this.url('/services/filter'), { params: httpParams });
  }

  createService(payload: ServiceUpsertRequest): Observable<ServiceResponse> {
    return this.http.post<ServiceResponse>(this.url('/services'), payload);
  }

  updateService(id: number, payload: ServiceUpsertRequest): Observable<ServiceResponse> {
    return this.http.put<ServiceResponse>(this.url(`/services/${id}`), payload);
  }

  updateServiceProjectParticipation(id: number, allowProjectParticipation: boolean): Observable<ServiceResponse> {
    return this.http.patch<ServiceResponse>(this.url(`/services/${id}/project-participation`), {
      allowProjectParticipation
    });
  }

  updateProviderProjectParticipation(providerId: number, allowProjectParticipation: boolean): Observable<number> {
    return this.http.patch<number>(this.url('/services/provider/project-participation'), {
      providerId,
      allowProjectParticipation
    });
  }

  deleteService(id: number): Observable<void> {
    return this.http.delete<void>(this.url(`/services/${id}`));
  }

  uploadServiceImage(id: number, file: File): Observable<{ imageUrl: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ imageUrl: string }>(this.url(`/services/${id}/image`), formData);
  }

  createBooking(payload: BookingRequest): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(this.url('/bookings'), payload);
  }

  getBookingsByUser(userId: number, page = 0, size = 20): Observable<PageResponse<BookingResponse>> {
    return this.http.get<PageResponse<BookingResponse>>(this.url(`/bookings/user/${userId}`), {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  getBookingsByProvider(providerId: number, page = 0, size = 20): Observable<PageResponse<BookingResponse>> {
    return this.http.get<PageResponse<BookingResponse>>(this.url(`/bookings/provider/${providerId}`), {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  getBookingsByProviderAndStatus(providerId: number, status: BookingResponse.StatusEnum, page = 0, size = 20): Observable<PageResponse<BookingResponse>> {
    return this.http.get<PageResponse<BookingResponse>>(this.url(`/bookings/provider/${providerId}/status/${status}`), {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  getBookingById(id: number): Observable<BookingResponse> {
    return this.http.get<BookingResponse>(this.url(`/bookings/${id}`));
  }

  updateBookingStatus(id: number, payload: BookingStatusUpdateRequest): Observable<BookingResponse> {
    return this.http.patch<BookingResponse>(this.url(`/bookings/${id}/status`), payload);
  }

  cancelBooking(id: number): Observable<BookingResponse> {
    return this.http.patch<BookingResponse>(this.url(`/bookings/${id}/cancel`), {});
  }

  getServiceReviews(serviceId: number, page = 0, size = 20): Observable<PageResponse<ServiceReviewResponse>> {
    return this.http.get<PageResponse<ServiceReviewResponse>>(this.url(`/service-reviews/service/${serviceId}`), {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  getProviderServiceReviews(
    providerId: number,
    status: BookingResponse.StatusEnum,
    page = 0,
    size = 20
  ): Observable<PageResponse<ServiceReviewResponse>> {
    return this.http.get<PageResponse<ServiceReviewResponse>>(this.url(`/service-reviews/provider/${providerId}`), {
      params: new HttpParams()
        .set('status', status)
        .set('page', page)
        .set('size', size)
    });
  }

  createServiceReview(payload: ServiceReviewRequest): Observable<ServiceReviewResponse> {
    return this.http.post<ServiceReviewResponse>(this.url('/service-reviews'), payload);
  }

  deleteServiceReview(id: number): Observable<void> {
    return this.http.delete<void>(this.url(`/service-reviews/${id}`));
  }

  addFavorite(serviceId: number): Observable<void> {
    return this.http.post<void>(this.url(`/favorites/${serviceId}`), {});
  }

  removeFavorite(serviceId: number): Observable<void> {
    return this.http.delete<void>(this.url(`/favorites/${serviceId}`));
  }

  isFavorite(serviceId: number): Observable<boolean> {
    return this.http.get<boolean>(this.url(`/favorites/${serviceId}/check`));
  }

  getFavoriteServices(page = 0, size = 20): Observable<PageResponse<ServiceResponse>> {
    return this.http.get<PageResponse<ServiceResponse>>(this.url('/favorites'), {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  compareServices(serviceIds: number[]): Observable<any[]> {
    return this.http.post<any[]>(this.url('/services/compare'), serviceIds);
  }

  getSurgePricing(): Observable<any> {
    return this.http.get<any>(this.url('/services/surge-pricing'));
  }

  getProjects(page = 0, size = 20): Observable<PageResponse<ProjectResponse>> {
    return this.http.get<PageResponse<ProjectResponse>>(this.url('/projects'), {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  getProjectById(id: number): Observable<ProjectResponse> {
    return this.http.get<ProjectResponse>(this.url(`/projects/${id}`));
  }

  getMyProjects(page = 0, size = 20): Observable<PageResponse<ProjectResponse>> {
    return this.http.get<PageResponse<ProjectResponse>>(this.url('/projects/my'), {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  getOpenPositionProjects(page = 0, size = 20): Observable<PageResponse<ProjectResponse>> {
    return this.http.get<PageResponse<ProjectResponse>>(this.url('/projects/open-positions'), {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  getEligibleServices(page = 0, size = 200): Observable<PageResponse<ServiceResponse>> {
    return this.http.get<PageResponse<ServiceResponse>>(this.url('/projects/eligible-services'), {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  createProject(payload: ProjectRequest): Observable<ProjectResponse> {
    return this.http.post<ProjectResponse>(this.url('/projects'), payload);
  }

  updateProject(id: number, payload: ProjectRequest): Observable<ProjectResponse> {
    return this.http.put<ProjectResponse>(this.url(`/projects/${id}`), payload);
  }

  deleteProject(id: number): Observable<void> {
    return this.http.delete<void>(this.url(`/projects/${id}`));
  }

  updateProjectStatus(id: number, status: string, reason?: string): Observable<ProjectResponse> {
    return this.http.patch<ProjectResponse>(this.url(`/projects/${id}/status`), { status, reason });
  }

  addProjectMember(projectId: number, userId: number): Observable<ProjectResponse> {
    return this.http.post<ProjectResponse>(this.url(`/projects/${projectId}/members/${userId}`), {});
  }

  removeProjectMember(projectId: number, userId: number): Observable<ProjectResponse> {
    return this.http.delete<ProjectResponse>(this.url(`/projects/${projectId}/members/${userId}`));
  }

  addProjectService(projectId: number, serviceId: number): Observable<ProjectResponse> {
    return this.http.post<ProjectResponse>(this.url(`/projects/${projectId}/services/${serviceId}`), {});
  }

  removeProjectService(projectId: number, serviceId: number): Observable<ProjectResponse> {
    return this.http.delete<ProjectResponse>(this.url(`/projects/${projectId}/services/${serviceId}`));
  }

  getProjectBookings(projectId: number, page = 0, size = 20): Observable<PageResponse<BookingResponse>> {
    return this.http.get<PageResponse<BookingResponse>>(this.url(`/projects/${projectId}/bookings`), {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  getProjectMilestones(projectId: number): Observable<ProjectMilestoneResponse[]> {
    return this.http.get<ProjectMilestoneResponse[]>(this.url(`/projects/${projectId}/milestones`));
  }

  createProjectMilestone(projectId: number, payload: ProjectMilestoneRequest): Observable<ProjectMilestoneResponse> {
    return this.http.post<ProjectMilestoneResponse>(this.url(`/projects/${projectId}/milestones`), payload);
  }

  updateProjectMilestone(projectId: number, milestoneId: number, payload: ProjectMilestoneRequest): Observable<ProjectMilestoneResponse> {
    return this.http.put<ProjectMilestoneResponse>(this.url(`/projects/${projectId}/milestones/${milestoneId}`), payload);
  }

  deleteProjectMilestone(projectId: number, milestoneId: number): Observable<void> {
    return this.http.delete<void>(this.url(`/projects/${projectId}/milestones/${milestoneId}`));
  }

  reorderProjectMilestones(projectId: number, orderedMilestoneIds: number[]): Observable<ProjectMilestoneResponse[]> {
    return this.http.put<ProjectMilestoneResponse[]>(this.url(`/projects/${projectId}/milestones/reorder`), {
      orderedMilestoneIds
    });
  }

  linkBookingToMilestone(projectId: number, milestoneId: number, bookingId: number): Observable<ProjectMilestoneResponse> {
    return this.http.post<ProjectMilestoneResponse>(
      this.url(`/projects/${projectId}/milestones/${milestoneId}/bookings/${bookingId}`), {});
  }

  unlinkBookingFromMilestone(projectId: number, milestoneId: number, bookingId: number): Observable<ProjectMilestoneResponse> {
    return this.http.delete<ProjectMilestoneResponse>(
      this.url(`/projects/${projectId}/milestones/${milestoneId}/bookings/${bookingId}`));
  }

  getProjectDependencies(projectId: number): Observable<ProjectDependencyResponse[]> {
    return this.http.get<ProjectDependencyResponse[]>(this.url(`/projects/${projectId}/dependencies`));
  }

  createProjectDependency(projectId: number, payload: ProjectDependencyRequest): Observable<ProjectDependencyResponse> {
    return this.http.post<ProjectDependencyResponse>(this.url(`/projects/${projectId}/dependencies`), payload);
  }

  deleteProjectDependency(projectId: number, dependencyId: number): Observable<void> {
    return this.http.delete<void>(this.url(`/projects/${projectId}/dependencies/${dependencyId}`));
  }

  getProjectTimeline(projectId: number): Observable<ProjectTimelineResponse> {
    return this.http.get<ProjectTimelineResponse>(this.url(`/projects/${projectId}/timeline`));
  }

  executeProjectWorkflow(projectId: number): Observable<ProjectTimelineResponse> {
    return this.http.post<ProjectTimelineResponse>(this.url(`/projects/${projectId}/workflow/execute`), {});
  }

  linkServiceToMilestone(projectId: number, milestoneId: number, serviceId: number): Observable<ProjectMilestoneResponse> {
    return this.http.post<ProjectMilestoneResponse>(
      this.url(`/projects/${projectId}/milestones/${milestoneId}/services/${serviceId}`), {});
  }

  unlinkServiceFromMilestone(projectId: number, milestoneId: number, serviceId: number): Observable<ProjectMilestoneResponse> {
    return this.http.delete<ProjectMilestoneResponse>(
      this.url(`/projects/${projectId}/milestones/${milestoneId}/services/${serviceId}`));
  }

  updateServiceEstimatedHours(projectId: number, milestoneId: number, serviceId: number, hours: number): Observable<void> {
    return this.http.put<void>(
      this.url(`/projects/${projectId}/milestones/${milestoneId}/services/${serviceId}/hours`), null,
      { params: { hours: String(hours) } });
  }

  allocateAndBook(projectId: number): Observable<{
    projectId?: number;
    status?: string;
    createdBookings?: Array<{
      bookingId?: number;
      serviceName?: string;
      providerName?: string;
      date?: string;
      duration?: number;
      milestoneTitle?: string;
    }>;
    skippedMilestones?: string[];
    warnings?: string[];
  }> {
    return this.http.post<any>(
      this.url(`/projects/${projectId}/allocate-and-book`), {});
  }

  generateSchedule(projectId: number): Observable<{
    projectId?: number;
    milestones?: Array<{
      milestoneId?: number;
      milestoneTitle?: string;
      sortOrder?: number;
      suggestedWeekStart?: string;
      suggestedWeekEnd?: string;
      estimatedDurationDays?: number;
      services?: Array<{
        serviceId?: number;
        serviceName?: string;
        providerName?: string;
        suggestedDate?: string;
        estimatedDuration?: number;
        available?: boolean;
      }>;
    }>;
    projectStartDate?: string;
    projectEndDate?: string;
  }> {
    return this.http.get<{
      projectId?: number;
      milestones?: Array<{
        milestoneId?: number;
        milestoneTitle?: string;
        sortOrder?: number;
        suggestedWeekStart?: string;
        suggestedWeekEnd?: string;
        estimatedDurationDays?: number;
        services?: Array<{
          serviceId?: number;
          serviceName?: string;
          providerName?: string;
          suggestedDate?: string;
          estimatedDuration?: number;
          available?: boolean;
        }>;
      }>;
      projectStartDate?: string;
      projectEndDate?: string;
    }>(this.url(`/projects/${projectId}/schedule`));
  }

  executeAutomatedWorkflow(projectId: number): Observable<{
    projectId?: number;
    status?: string;
    createdBookings?: Array<{
      bookingId?: number;
      serviceName?: string;
      providerName?: string;
      date?: string;
      duration?: number;
      milestoneTitle?: string;
    }>;
    skippedMilestones?: string[];
    warnings?: string[];
  }> {
    return this.http.post<{
      projectId?: number;
      status?: string;
      createdBookings?: Array<{
        bookingId?: number;
        serviceName?: string;
        providerName?: string;
        date?: string;
        duration?: number;
        milestoneTitle?: string;
      }>;
      skippedMilestones?: string[];
      warnings?: string[];
    }>(this.url(`/projects/${projectId}/workflow/auto-execute`), {});
  }

  replanProject(projectId: number): Observable<ProjectTimelineResponse> {
    return this.http.post<ProjectTimelineResponse>(this.url(`/projects/${projectId}/workflow/replan`), {});
  }

  getMilestoneTemplates(projectId: number): Observable<{
    milestones?: Array<{ title?: string; details?: string; sortOrder?: number; category?: string }>;
    suggestedDependencies?: Array<{
      predecessorMilestoneId?: number;
      predecessorMilestoneTitle?: string;
      successorMilestoneId?: number;
      successorMilestoneTitle?: string;
      reason?: string;
      confidence?: number;
    }>;
  }> {
    return this.http.get<{
      milestones?: Array<{ title?: string; details?: string; sortOrder?: number; category?: string }>;
      suggestedDependencies?: Array<{
        predecessorMilestoneId?: number;
        predecessorMilestoneTitle?: string;
        successorMilestoneId?: number;
        successorMilestoneTitle?: string;
        reason?: string;
        confidence?: number;
      }>;
    }>(this.url(`/projects/${projectId}/assistant/templates`));
  }

  getDependencySuggestions(projectId: number): Observable<Array<{
    predecessorMilestoneId?: number;
    predecessorMilestoneTitle?: string;
    successorMilestoneId?: number;
    successorMilestoneTitle?: string;
    reason?: string;
    confidence?: number;
  }>> {
    return this.http.get<Array<{
      predecessorMilestoneId?: number;
      predecessorMilestoneTitle?: string;
      successorMilestoneId?: number;
      successorMilestoneTitle?: string;
      reason?: string;
      confidence?: number;
    }>>(this.url(`/projects/${projectId}/assistant/dependency-suggestions`));
  }

  bulkApplyDependencies(projectId: number, requests: Array<{
    predecessorMilestoneId: number;
    successorMilestoneId: number;
  }>): Observable<ProjectDependencyResponse[]> {
    return this.http.post<ProjectDependencyResponse[]>(
      this.url(`/projects/${projectId}/dependencies/bulk`),
      requests
    );
  }

  getRiskAssessment(projectId: number): Observable<{
    projectId?: number;
    alerts?: Array<{
      severity?: string;
      type?: string;
      message?: string;
      milestoneId?: number;
      milestoneTitle?: string;
    }>;
    criticalPath?: Array<{
      milestoneId?: number;
      milestoneTitle?: string;
      sortOrder?: number;
      chainDepth?: number;
      isOnCriticalPath?: boolean;
    }>;
    overallRiskScore?: number;
  }> {
    return this.http.get<{
      projectId?: number;
      alerts?: Array<{
        severity?: string;
        type?: string;
        message?: string;
        milestoneId?: number;
        milestoneTitle?: string;
      }>;
      criticalPath?: Array<{
        milestoneId?: number;
        milestoneTitle?: string;
        sortOrder?: number;
        chainDepth?: number;
        isOnCriticalPath?: boolean;
      }>;
      overallRiskScore?: number;
    }>(this.url(`/projects/${projectId}/assistant/risk-assessment`));
  }

  getScheduleOptimization(projectId: number): Observable<{
    projectId?: number;
    recommendations?: Array<{
      serviceId?: number;
      serviceName?: string;
      bookingId?: number;
      recommendedDate?: string;
      recommendedDuration?: number;
      score?: number;
      reasonCode?: string;
      notes?: string[];
    }>;
    optimizationNotes?: string[];
  }> {
    return this.http.get<{
      projectId?: number;
      recommendations?: Array<{
        serviceId?: number;
        serviceName?: string;
        bookingId?: number;
        recommendedDate?: string;
        recommendedDuration?: number;
        score?: number;
        reasonCode?: string;
        notes?: string[];
      }>;
      optimizationNotes?: string[];
    }>(this.url(`/projects/${projectId}/assistant/schedule-optimization`));
  }

  getProjectSlotSuggestions(
    projectId: number,
    serviceId: number,
    startDate: string,
    endDate?: string,
    mode: 'PROJECT_FIRST' | 'COMPETITIVE' = 'PROJECT_FIRST',
    limit = 10
  ): Observable<{
    serviceId?: number;
    projectId?: number;
    mode?: 'PROJECT_FIRST' | 'COMPETITIVE';
    suggestions?: Array<{
      rank?: number;
      slot?: TimeSlotDto;
      score?: {
        policyProfile?: string;
        availabilityWeight?: number;
        scarcityWeight?: number;
        projectUrgencyWeight?: number;
        projectProgressWeight?: number;
        reliabilityWeight?: number;
        fairnessWeight?: number;
        tieBreakerWeight?: number;
        modeMultiplier?: number;
        finalScore?: number;
        reasonCode?: string;
      };
    }>;
  }> {
    let params = new HttpParams()
      .set('serviceId', serviceId)
      .set('startDate', startDate)
      .set('mode', mode)
      .set('limit', limit);

    if (endDate) params = params.set('endDate', endDate);

    return this.http.get<{
      serviceId?: number;
      projectId?: number;
      mode?: 'PROJECT_FIRST' | 'COMPETITIVE';
      suggestions?: Array<{
        rank?: number;
        slot?: TimeSlotDto;
        score?: {
          policyProfile?: string;
          availabilityWeight?: number;
          scarcityWeight?: number;
          projectUrgencyWeight?: number;
          projectProgressWeight?: number;
          reliabilityWeight?: number;
          fairnessWeight?: number;
          tieBreakerWeight?: number;
          modeMultiplier?: number;
          finalScore?: number;
          reasonCode?: string;
        };
      }>;
    }>(this.url(`/projects/${projectId}/slot-suggestions`), { params });
  }

  getPartners(page = 0, size = 20): Observable<PageResponse<PartnerResponse>> {
    return this.http.get<PageResponse<PartnerResponse>>(this.url('/partners'), {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }

  createPartner(payload: PartnerRequest): Observable<PartnerResponse> {
    return this.http.post<PartnerResponse>(this.url('/partners'), payload);
  }

  getAvailableSlots(serviceId: number, startDate: string, endDate?: string): Observable<TimeSlotDto[]> {
    let params = new HttpParams().set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<TimeSlotDto[]>(this.url(`/availability/${serviceId}/slots`), { params });
  }

  getScoredSlotSuggestions(
    serviceId: number,
    startDate: string,
    endDate?: string,
    projectId?: number,
    mode: 'PROJECT_FIRST' | 'COMPETITIVE' = 'PROJECT_FIRST',
    limit = 10
  ): Observable<{
    serviceId?: number;
    projectId?: number;
    mode?: 'PROJECT_FIRST' | 'COMPETITIVE';
    suggestions?: Array<{
      rank?: number;
      slot?: TimeSlotDto;
      score?: {
        policyProfile?: string;
        availabilityWeight?: number;
        scarcityWeight?: number;
        projectUrgencyWeight?: number;
        projectProgressWeight?: number;
        reliabilityWeight?: number;
        fairnessWeight?: number;
        tieBreakerWeight?: number;
        modeMultiplier?: number;
        finalScore?: number;
        reasonCode?: string;
      };
    }>;
  }> {
    let params = new HttpParams()
      .set('startDate', startDate)
      .set('mode', mode)
      .set('limit', limit);

    if (endDate) params = params.set('endDate', endDate);
    if (projectId != null) params = params.set('projectId', projectId);

    return this.http.get<{
      serviceId?: number;
      projectId?: number;
      mode?: 'PROJECT_FIRST' | 'COMPETITIVE';
      suggestions?: Array<{
        rank?: number;
        slot?: TimeSlotDto;
        score?: {
          policyProfile?: string;
          availabilityWeight?: number;
          scarcityWeight?: number;
          projectUrgencyWeight?: number;
          projectProgressWeight?: number;
          reliabilityWeight?: number;
          fairnessWeight?: number;
          tieBreakerWeight?: number;
          modeMultiplier?: number;
          finalScore?: number;
          reasonCode?: string;
        };
      }>;
    }>(this.url(`/availability/${serviceId}/slots/suggestions`), { params });
  }

  getProjectAllocationAudit(projectId: number): Observable<Array<{
    id?: number;
    serviceId?: number;
    projectId?: number;
    mode?: string;
    slotStart?: string;
    slotEnd?: string;
    finalScore?: number;
    reasonCode?: string;
    policyProfile?: string;
    tieBreakerWeight?: number;
    priorityMarkupApplied?: boolean;
    createdAt?: string;
  }>> {
    return this.http.get<Array<{
      id?: number;
      serviceId?: number;
      projectId?: number;
      mode?: string;
      slotStart?: string;
      slotEnd?: string;
      finalScore?: number;
      reasonCode?: string;
      policyProfile?: string;
      tieBreakerWeight?: number;
      priorityMarkupApplied?: boolean;
      createdAt?: string;
    }>>(this.url(`/availability/allocation-audit/project/${projectId}`));
  }

  isServiceOverbooked(serviceId: number, providerId: number): Observable<boolean> {
    return this.http.get<boolean>(this.url(`/availability/${serviceId}/overbooked`), {
      params: new HttpParams().set('providerId', providerId)
    });
  }

  isProviderOverbooked(providerId: number): Observable<boolean> {
    return this.http.get<boolean>(this.url(`/availability/provider/${providerId}/overbooked`));
  }

  getWeeklyTemplates(providerId: number): Observable<WeeklyTemplateResponse[]> {
    return this.http.get<WeeklyTemplateResponse[]>(this.url('/availability/templates'), {
      params: new HttpParams().set('providerId', providerId)
    });
  }

  getGlobalWeeklyTemplates(providerId: number): Observable<WeeklyTemplateResponse[]> {
    return this.http.get<WeeklyTemplateResponse[]>(this.url('/availability/templates/global'), {
      params: new HttpParams().set('providerId', providerId)
    });
  }

  getWeeklyTemplatesForService(providerId: number, serviceId: number): Observable<WeeklyTemplateResponse[]> {
    return this.http.get<WeeklyTemplateResponse[]>(this.url(`/availability/templates/service/${serviceId}`), {
      params: new HttpParams().set('providerId', providerId)
    });
  }

  createWeeklyTemplate(payload: WeeklyTemplateRequest): Observable<WeeklyTemplateResponse> {
    return this.http.post<WeeklyTemplateResponse>(this.url('/availability/templates'), payload);
  }

  updateWeeklyTemplate(id: number, payload: WeeklyTemplateRequest): Observable<WeeklyTemplateResponse> {
    return this.http.put<WeeklyTemplateResponse>(this.url(`/availability/templates/${id}`), payload);
  }

  deleteWeeklyTemplate(id: number): Observable<void> {
    return this.http.delete<void>(this.url(`/availability/templates/${id}`));
  }

  saveWeeklyTemplatesBatch(payload: WeeklyTemplateBatchRequest): Observable<WeeklyTemplateResponse[]> {
    return this.http.put<WeeklyTemplateResponse[]>(this.url('/availability/templates/batch'), payload);
  }

  getProviderExceptions(providerId: number): Observable<ProviderExceptionResponse[]> {
    return this.http.get<ProviderExceptionResponse[]>(this.url('/availability/exceptions'), {
      params: new HttpParams().set('providerId', providerId)
    });
  }

  getProviderExceptionsByDateRange(providerId: number, startDate: string, endDate: string): Observable<ProviderExceptionResponse[]> {
    return this.http.get<ProviderExceptionResponse[]>(this.url('/availability/exceptions/date-range'), {
      params: new HttpParams().set('providerId', providerId).set('startDate', startDate).set('endDate', endDate)
    });
  }

  createProviderException(payload: ProviderExceptionRequest): Observable<ProviderExceptionResponse> {
    return this.http.post<ProviderExceptionResponse>(this.url('/availability/exceptions'), payload);
  }

  updateProviderException(id: number, payload: ProviderExceptionRequest): Observable<ProviderExceptionResponse> {
    return this.http.put<ProviderExceptionResponse>(this.url(`/availability/exceptions/${id}`), payload);
  }

  deleteProviderException(id: number): Observable<void> {
    return this.http.delete<void>(this.url(`/availability/exceptions/${id}`));
  }

  getServiceMandates(providerId: number): Observable<ServiceMandateResponse[]> {
    return this.http.get<ServiceMandateResponse[]>(this.url('/mandates/service'), {
      params: new HttpParams().set('providerId', providerId)
    });
  }

  createServiceMandate(payload: ServiceMandateRequest): Observable<ServiceMandateResponse> {
    return this.http.post<ServiceMandateResponse>(this.url('/mandates/service'), payload);
  }

  deleteServiceMandate(id: number): Observable<void> {
    return this.http.delete<void>(this.url(`/mandates/service/${id}`));
  }

  getProviderMandate(providerId: number): Observable<ProviderMandateResponse | null> {
    return this.http.get<ProviderMandateResponse | null>(this.url('/mandates/provider'), {
      params: new HttpParams().set('providerId', providerId)
    });
  }

  createProviderMandate(payload: ProviderMandateRequest): Observable<ProviderMandateResponse> {
    return this.http.post<ProviderMandateResponse>(this.url('/mandates/provider'), payload);
  }

  deleteProviderMandate(id: number): Observable<void> {
    return this.http.delete<void>(this.url(`/mandates/provider/${id}`));
  }

  requestReschedule(bookingId: number, payload: { proposedDate: string; proposedDuration: number; reason?: string; message?: string }): Observable<any> {
    return this.http.post(this.url(`/bookings/${bookingId}/reschedule`), payload);
  }

  getActiveReschedule(bookingId: number): Observable<any> {
    return this.http.get(this.url(`/bookings/${bookingId}/reschedule`));
  }

  getRescheduleHistory(bookingId: number): Observable<any[]> {
    return this.http.get<any[]>(this.url(`/bookings/${bookingId}/reschedule/history`));
  }

  acceptReschedule(requestId: number, responseMessage?: string): Observable<any> {
    return this.http.patch(this.url(`/bookings/reschedule/${requestId}/accept`), { responseMessage });
  }

  rejectReschedule(requestId: number, responseMessage?: string): Observable<any> {
    return this.http.patch(this.url(`/bookings/reschedule/${requestId}/reject`), { responseMessage });
  }

  cancelReschedule(requestId: number): Observable<any> {
    return this.http.patch(this.url(`/bookings/reschedule/${requestId}/cancel`), {});
  }

  createDeliverable(bookingId: number, title: string, description: string, files?: File[]): Observable<DeliverableResponse> {
    const formData = new FormData();
    const metadataBlob = new Blob([JSON.stringify({ title, description })], { type: 'application/json' });
    formData.append('metadata', metadataBlob);
    if (files) {
      files.forEach(f => formData.append('files', f));
    }
    return this.http.post<DeliverableResponse>(this.url(`/bookings/${bookingId}/deliverables`), formData);
  }

  getDeliverablesByBooking(bookingId: number): Observable<DeliverableResponse[]> {
    return this.http.get<DeliverableResponse[]>(this.url(`/bookings/${bookingId}/deliverables`));
  }

  getDeliverableById(id: number): Observable<DeliverableResponse> {
    return this.http.get<DeliverableResponse>(this.url(`/deliverables/${id}`));
  }

  submitDeliverable(id: number): Observable<DeliverableResponse> {
    return this.http.patch<DeliverableResponse>(this.url(`/deliverables/${id}/submit`), {});
  }

  reviewDeliverable(id: number, decision: string, comment?: string): Observable<DeliverableResponse> {
    return this.http.post<DeliverableResponse>(this.url(`/deliverables/${id}/review`), { decision, comment });
  }

  addDeliverableAttachment(deliverableId: number, file: File): Observable<DeliverableAttachmentResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<DeliverableAttachmentResponse>(this.url(`/deliverables/${deliverableId}/attachments`), formData);
  }

  deleteDeliverableAttachment(deliverableId: number, attachmentId: number): Observable<void> {
    return this.http.delete<void>(this.url(`/deliverables/${deliverableId}/attachments/${attachmentId}`));
  }

  getDeliverableHistory(deliverableId: number): Observable<DeliverableReviewResponse[]> {
    return this.http.get<DeliverableReviewResponse[]>(this.url(`/deliverables/${deliverableId}/history`));
  }

  getDeliverableVersions(deliverableId: number): Observable<Array<{
    id?: number;
    deliverableId?: number;
    versionNumber?: number;
    status?: string;
    submittedAt?: string;
    reviewedAt?: string;
    createdAt?: string;
    attachments?: Array<{
      fileUrl?: string;
      fileName?: string;
      fileSize?: number;
      fileType?: string;
      uploadedAt?: string;
    }>;
  }>> {
    return this.http.get<Array<{
      id?: number;
      deliverableId?: number;
      versionNumber?: number;
      status?: string;
      submittedAt?: string;
      reviewedAt?: string;
      createdAt?: string;
      attachments?: Array<{
        fileUrl?: string;
        fileName?: string;
        fileSize?: number;
        fileType?: string;
        uploadedAt?: string;
      }>;
    }>>(this.url(`/deliverables/${deliverableId}/versions`));
  }

  getBookingAttachments(bookingId: number): Observable<BookingAttachmentResponse[]> {
    return this.http.get<BookingAttachmentResponse[]>(this.url(`/bookings/${bookingId}/attachments`));
  }

  uploadBookingAttachment(bookingId: number, file: File): Observable<BookingAttachmentResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<BookingAttachmentResponse>(this.url(`/bookings/${bookingId}/attachments`), formData);
  }

  deleteBookingAttachment(bookingId: number, attachmentId: number): Observable<void> {
    return this.http.delete<void>(this.url(`/bookings/${bookingId}/attachments/${attachmentId}`));
  }

  getBookingMessages(bookingId: number): Observable<Array<{
    id?: number;
    bookingId?: number;
    senderId?: number;
    senderName?: string;
    message?: string;
    createdAt?: string;
  }>> {
    return this.http.get<Array<{
      id?: number;
      bookingId?: number;
      senderId?: number;
      senderName?: string;
      message?: string;
      createdAt?: string;
    }>>(this.url(`/bookings/${bookingId}/messages`));
  }

  getBookingMessagesBatch(bookingId: number, beforeId?: number, limit = 30): Observable<{
    items: Array<{
      id?: number;
      bookingId?: number;
      senderId?: number;
      senderName?: string;
      message?: string;
      createdAt?: string;
    }>;
    nextCursor?: number;
    hasMore: boolean;
  }> {
    let params = new HttpParams().set('limit', limit);
    if (beforeId != null) {
      params = params.set('beforeId', beforeId);
    }
    return this.http.get<{
      items: Array<{
        id?: number;
        bookingId?: number;
        senderId?: number;
        senderName?: string;
        message?: string;
        createdAt?: string;
      }>;
      nextCursor?: number;
      hasMore: boolean;
    }>(this.url(`/bookings/${bookingId}/messages/batch`), { params });
  }

  sendBookingMessage(bookingId: number, message: string): Observable<{
    id?: number;
    bookingId?: number;
    senderId?: number;
    senderName?: string;
    message?: string;
    createdAt?: string;
  }> {
    return this.http.post<{
      id?: number;
      bookingId?: number;
      senderId?: number;
      senderName?: string;
      message?: string;
      createdAt?: string;
    }>(this.url(`/bookings/${bookingId}/messages`), { message });
  }

  getBookingMessagesNewer(bookingId: number, afterId: number, limit = 30): Observable<Array<{
    id?: number;
    bookingId?: number;
    senderId?: number;
    senderName?: string;
    message?: string;
    createdAt?: string;
  }>> {
    return this.http.get<Array<{
      id?: number;
      bookingId?: number;
      senderId?: number;
      senderName?: string;
      message?: string;
      createdAt?: string;
    }>>(this.url(`/bookings/${bookingId}/messages/newer`), { params: { afterId, limit } });
  }

  getBookingMlPrediction(bookingId: number): Observable<{
    completionProbability?: number;
    riskLevel?: string;
    confidence?: string;
    keyFactors?: string[];
    recommendation?: string;
  }> {
    return this.http.get(this.url(`/bookings/${bookingId}/ml-prediction`));
  }

  getProjectDelayPrediction(projectId: number): Observable<{
    onTimeProbability?: number;
    delayRiskLevel?: string;
    estimatedDelayDays?: number;
    keyFactors?: string[];
    recommendation?: string;
  }> {
    return this.http.get(this.url(`/projects/${projectId}/ml-delay-prediction`));
  }

  getServiceRiskAnalysis(projectId: number): Observable<{
    projectId?: number;
    services?: Array<{
      serviceId?: number;
      serviceName?: string;
      category?: string;
      providerName?: string;
      providerId?: number;
      milestoneTitle?: string;
      milestoneId?: number;
      riskLevel?: number;
      completionProbability?: number;
      confidence?: string;
      recommendation?: string;
      keyFactors?: string[];
    }>;
  }> {
    return this.http.get(this.url(`/projects/${projectId}/service-risk-analysis`));
  }

  getProviderStanding(providerId: number): Observable<{
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
  }> {
    return this.http.get(this.url(`/services/provider/${providerId}/standing`));
  }
}
