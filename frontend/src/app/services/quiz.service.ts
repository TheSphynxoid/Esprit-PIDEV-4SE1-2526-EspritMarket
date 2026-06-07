import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ApiConfigService } from './api';

export interface InterviewSlotResponse {
  start: string;
  end: string;
  available: boolean;
}

export interface InterviewDayScheduleResponse {
  date: string;
  dayLabel: string;
  slots: InterviewSlotResponse[];
}

export interface InterviewCalendarResponse {
  weekStart: string;
  weekEnd: string;
  days: InterviewDayScheduleResponse[];
}

export interface InterviewBookingRequest {
  interviewDate: string;
}

export interface QuizSubmitRequest {
  userId: number;
  score: number;
}

export interface QuizResultResponse {
  id: number;
  userId: number;
  userName: string;
  score: number;
  status: 'PENDING' | 'ACCEPTED_QUIZ' | 'REJECTED';
  message: string;
  meetingLink?: string;
  submittedAt: string;
  interviewScheduledAt?: string;
  isPassed: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class QuizService {
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);
  private readonly baseUrl = this.apiConfig.buildUrl('/api/quiz');

  submitQuiz(userId: number, score: number): Observable<QuizResultResponse> {
    const request: QuizSubmitRequest = { userId, score };
    return this.http
      .post<QuizResultResponse>(`${this.baseUrl}/submit`, request)
      .pipe(catchError((error) => this.handleError(error)));
  }

  getQuizResult(userId: number): Observable<QuizResultResponse> {
    return this.http
      .get<QuizResultResponse>(`${this.baseUrl}/${userId}`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  getMostRecentQuizResult(userId: number): Observable<QuizResultResponse> {
    return this.http
      .get<QuizResultResponse>(`${this.baseUrl}/${userId}/recent`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  getAcceptedCandidates(): Observable<QuizResultResponse[]> {
    return this.http
      .get<QuizResultResponse[]>(`${this.baseUrl}/status/accepted`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  getRejectedCandidates(): Observable<QuizResultResponse[]> {
    return this.http
      .get<QuizResultResponse[]>(`${this.baseUrl}/status/rejected`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  getResultsByStatus(status: string): Observable<QuizResultResponse[]> {
    return this.http
      .get<QuizResultResponse[]>(`${this.baseUrl}/status/${status}`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  getInterviewCalendar(): Observable<InterviewCalendarResponse> {
    return this.http
      .get<InterviewCalendarResponse>(`${this.baseUrl}/interview/calendar`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  bookInterviewSlot(request: InterviewBookingRequest): Observable<QuizResultResponse> {
    return this.http
      .post<QuizResultResponse>(`${this.baseUrl}/interview/book`, request)
      .pipe(catchError((error) => this.handleError(error)));
  }

  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'Une erreur est survenue';

    if (error.status === 400) {
      errorMessage = error.error?.message || 'Requete invalide';
    } else if (error.status === 404) {
      errorMessage = 'Ressource non trouvee';
    } else if (error.status === 403) {
      errorMessage = 'Seuls les livreurs (COURIER) peuvent acceder au quiz';
    }

    return throwError(() => errorMessage);
  }
}
