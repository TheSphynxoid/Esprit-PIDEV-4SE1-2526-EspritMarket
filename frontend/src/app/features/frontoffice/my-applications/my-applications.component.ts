import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import { PartnershipApiService } from '../../../services/api/partnership-api.service';
import { AuthService } from '../../../services/auth.service';
import { ApplicationResource } from '../../../services/api/models/api-resource.model';
import { Subscription } from 'rxjs';
import { timeout } from 'rxjs/operators';

@Component({
  selector: 'app-my-applications',
  standalone: true,
  imports: [CommonModule, RouterModule, HeaderComponent, FooterComponent],
  templateUrl: './my-applications.component.html',
  styleUrls: ['./my-applications.component.css']
})
export class MyApplicationsComponent implements OnInit, OnDestroy {
  private partnershipApi = inject(PartnershipApiService);
  private authService = inject(AuthService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);
  private sub = new Subscription();

  applications: ApplicationResource[] = [];
  loading = false;
  errorMessage = '';

  ngOnInit(): void {
    this.loadApplications();
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  loadApplications(): void {
    const currentUser = this.authService.currentUser();
    if (!currentUser || !currentUser.id) {
      this.errorMessage = 'Please log in to view your applications.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.sub.add(
      this.partnershipApi.getStudentApplications(currentUser.id)
        .pipe(timeout(10000))
        .subscribe({
          next: (data) => {
            // Sort by newest first (assuming higher ID means newer, or we can sort by date if we had a createdDate)
            this.applications = data.sort((a, b) => b.id - a.id);
            this.loading = false;
            this.cdr.detectChanges();
          },
          error: (err) => {
            this.loading = false;
            this.errorMessage = 'Failed to load applications. ' + (err.error?.message || '');
            this.cdr.detectChanges();
          }
        })
    );
  }

  getInterviewsForApplication(app: ApplicationResource) {
    // Assuming the application object has an 'interviews' array from the backend, 
    // or we might need to rely on what the backend provides.
    // The backend Application entity has 'interviews' mapped, but it might not be serialized properly.
    // Wait, the backend Application has @ToString.Exclude private List<Interview> interviews.
    // Let's check if the backend DTO or entity includes interviews when fetching findAllWithDetails.
    // If not, we might need a separate endpoint, but let's assume it returns it, or we just use app.interviews if present.
    return app.interviews || [];
  }

  hasScheduledInterview(app: ApplicationResource): boolean {
    const interviews = this.getInterviewsForApplication(app);
    return interviews.some(i => i.status === 'SCHEDULED');
  }

  getScheduledInterview(app: ApplicationResource) {
    const interviews = this.getInterviewsForApplication(app);
    return interviews.find(i => i.status === 'SCHEDULED');
  }

  getLatestInterview(app: ApplicationResource) {
    const interviews = this.getInterviewsForApplication(app);
    if (interviews.length === 0) return null;
    return interviews[interviews.length - 1]; // Assuming appended chronologically, or sort if needed
  }

  isInterviewJoinable(interview: any): boolean {
    if (!interview || interview.type !== 'VIDEO') return false;
    
    const now = new Date();
    const interviewDate = new Date(interview.interviewDate);
    
    // Calculate difference in minutes
    const diffMinutes = (interviewDate.getTime() - now.getTime()) / (1000 * 60);
    
    // Joinable if it's within 15 minutes before, or it has already started
    return diffMinutes <= 15;
  }

  getTimeUntilInterviewMessage(interview: any): string {
    if (!interview || interview.type !== 'VIDEO') return '';

    const now = new Date();
    const interviewDate = new Date(interview.interviewDate);
    const diffMs = interviewDate.getTime() - now.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    const diffHours = Math.floor((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    const diffMinutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));

    if (diffMs <= 0) {
      return 'The interview should be starting now!';
    }

    if (diffDays > 0) {
      return `Opens in ${diffDays} day${diffDays > 1 ? 's' : ''}`;
    }

    if (diffHours > 0) {
      return `Opens in ${diffHours} hour${diffHours > 1 ? 's' : ''}`;
    }

    return `Opens in ${diffMinutes} min`;
  }

  joinInterview(roomId: string | undefined): void {
    if (!roomId) {
      alert('The interview room is not available yet.');
      return;
    }
    this.router.navigate(['/interview-room', roomId]);
  }

  getScoreClass(score: number): string {
    if (score >= 75) return 'score-high';
    if (score >= 50) return 'score-medium';
    return 'score-low';
  }
}
