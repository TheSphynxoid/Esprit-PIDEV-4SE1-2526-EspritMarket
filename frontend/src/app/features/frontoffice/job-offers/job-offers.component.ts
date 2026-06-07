import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import { PartnershipApiService } from '../../../services/api/partnership-api.service';
import { ProfilService, Profil } from '../../../services/api/profil.service';
import { SrvApiService } from '../../../services/api/srv-api.service';
import { AuthService } from '../../../services/auth.service';
import { timeout, debounceTime, distinctUntilChanged, switchMap, takeUntil } from 'rxjs/operators';
import { Subject, BehaviorSubject } from 'rxjs';

@Component({
  selector: 'app-job-offers',
  standalone: true,
  imports: [CommonModule, HeaderComponent, FooterComponent, FormsModule],
  templateUrl: './job-offers.component.html'
})
export class JobOffersComponent implements OnInit, OnDestroy {
  private readonly partnershipApi = inject(PartnershipApiService);
  private readonly profilService = inject(ProfilService);
  private readonly srvApi = inject(SrvApiService);
  private readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly ngZone = inject(NgZone);
  private destroy$ = new Subject<void>();

  jobOffers: Array<{
    id: number;
    title: string;
    type: string;
    company: string;
    description: string;
    experienceLevel: string;
    location: string;
    salary: string | null;
    skills: string[];
  }> = [];
  
  motivations: { [jobId: number]: string } = {};
  loadingOffers = false;
  applyingJobId: number | null = null;
  errorMessages: { [jobId: number]: string } = {};
  successMessages: { [jobId: number]: string } = {};
  
  // Profile Step State
  applicationStep: { [jobId: number]: number } = {};
  profilForms: { [jobId: number]: Profil } = {};
  loadingProfile: { [jobId: number]: boolean } = {};
  
  loadingError: string | null = null;
  showSuccessDialog = false;
  successDialogJobTitle = '';
  submitError = '';

  // Search and Filter variables
  searchKeyword = '';
  filterType = '';
  filterLocation = '';
  filterExperienceLevel = '';

  // Pagination variables
  currentPage = 0;
  pageSize = 6;
  totalElements = 0;
  totalPages = 0;
  pages: number[] = [];

  private searchSubject = new Subject<void>();
  private readonly maxCvSizeBytes = 5 * 1024 * 1024;

  ngOnInit(): void {
    // Setup reactive search
    this.searchSubject.pipe(
      debounceTime(300), // Wait 300ms after last keystroke
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.loadJobOffers();
    });

    this.loadJobOffers();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadJobOffers(): void {
    this.loadingOffers = true;
    this.loadingError = null;
    
    this.partnershipApi.searchJobOffers(
      this.searchKeyword,
      this.filterType,
      this.filterLocation,
      this.filterExperienceLevel,
      this.currentPage,
      this.pageSize
    ).pipe(
      timeout(10000),
    ).subscribe({
      next: (page) => {
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.pages = Array.from({ length: this.totalPages }, (_, i) => i);
        
        if (!page.content || page.content.length === 0) {
          if (!this.searchKeyword && !this.filterType && !this.filterLocation && !this.filterExperienceLevel) {
            this.loadMockJobOffers();
          } else {
            this.jobOffers = [];
            this.loadingOffers = false;
          }
          return;
        }
        
        this.jobOffers = page.content.map((offer) => ({
          id: offer.id,
          title: offer.title,
          type: offer.type,
          company: offer.company?.name || 'ESPRIT Market',
          description: offer.description,
          experienceLevel: offer.experienceLevel || 'BEGINNER',
          location: offer.location || 'Not specified',
          salary: null,
          skills: typeof offer.requiredSkills === 'string'
            ? offer.requiredSkills.split(',').map(s => s.trim()).filter(s => s.length > 0)
            : (offer.requiredSkills || [])
        }));
        this.loadingOffers = false;
        this.cdr.detectChanges(); // Force UI update
      },
      error: (err) => {
        console.error('Failed to load job offers:', err);
        if (!this.searchKeyword && !this.filterType && !this.filterLocation && !this.filterExperienceLevel) {
          this.loadMockJobOffers();
        } else {
          this.loadingError = 'An error occurred while loading job offers.';
          this.loadingOffers = false;
        }
        this.cdr.detectChanges(); // Force UI update
      }
    });
  }

  onSearch(): void {
    this.currentPage = 0;
    this.searchSubject.next();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadJobOffers();
  }

  resetFilters(): void {
    this.searchKeyword = '';
    this.filterType = '';
    this.filterLocation = '';
    this.filterExperienceLevel = '';
    this.currentPage = 0;
    this.loadJobOffers();
  }

  private loadMockJobOffers(): void {
    // Mock data for demonstration
    this.jobOffers = [
      {
        id: 1,
        title: 'Senior Angular Developer',
        type: 'INTERNSHIP',
        company: 'ESPRIT Market',
        description: 'We are looking for an experienced Angular developer to join our team and work on cutting-edge projects.',
        experienceLevel: 'BEGINNER',
        location: 'Tunisie',
        salary: '2000',
        skills: ['Angular', 'TypeScript', 'RxJS']
      },
      {
        id: 2,
        title: 'Full Stack Developer',
        type: 'PROJECT',
        company: 'ESPRIT Market',
        description: 'Join our development team to build scalable web applications using modern technologies.',
        experienceLevel: 'INTERMEDIATE',
        location: 'Tunisie',
        salary: '2500',
        skills: ['Node.js', 'Angular', 'MongoDB']
      },
      {
        id: 3,
        title: 'UI/UX Designer',
        type: 'SERVICE',
        company: 'ESPRIT Market',
        description: 'Create beautiful and intuitive user interfaces for our digital products.',
        experienceLevel: 'ADVANCED',
        location: 'Tunisie',
        salary: null,
        skills: ['Figma', 'Design Thinking', 'User Research']
      }
    ];
    console.log('Using mock job offers:', this.jobOffers);
    this.loadingOffers = false;
  }

  nextStep(projectId: number): void {
    const motivation = this.motivations[projectId] || '';
    delete this.errorMessages[projectId];

    if (!motivation || motivation.trim().length < 10) {
      this.errorMessages[projectId] = 'Motivation must be at least 10 characters';
      return;
    }

    const currentUser = this.authService.currentUser();
    if (!currentUser || !currentUser.id) {
      this.errorMessages[projectId] = 'Please log in to apply for jobs';
      return;
    }

    this.loadingProfile[projectId] = true;
    
    // Fetch user profile to prefill
    this.profilService.getProfil(currentUser.id).subscribe({
      next: (profil) => {
        this.profilForms[projectId] = { ...profil };
        // Ensure arrays exist
        if (!this.profilForms[projectId].skills) this.profilForms[projectId].skills = [];
        if (!this.profilForms[projectId].languages) this.profilForms[projectId].languages = [];
        
        this.applicationStep[projectId] = 2;
        this.loadingProfile[projectId] = false;
        this.cdr.detectChanges();
      },
      error: () => {
        // If not found, create empty profile
        this.profilForms[projectId] = {
          skills: [],
          experienceLevel: 'BEGINNER',
          fieldOfStudy: '',
          yearsOfExperience: '',
          languages: []
        };
        this.applicationStep[projectId] = 2;
        this.loadingProfile[projectId] = false;
        this.cdr.detectChanges();
      }
    });
  }

  prevStep(projectId: number): void {
    this.applicationStep[projectId] = 1;
  }

  applyForJob(projectId: number): void {
    const motivation = this.motivations[projectId] || '';
    const profilData = this.profilForms[projectId];
    
    delete this.errorMessages[projectId];
    delete this.successMessages[projectId];

    const currentUser = this.authService.currentUser();
    if (!currentUser || !currentUser.id) {
      this.errorMessages[projectId] = 'Please log in to apply for jobs';
      return;
    }

    this.applyingJobId = projectId;

    // 1. Save or Update the profile first
    this.profilService.saveOrUpdateProfil(currentUser.id, profilData).pipe(
      switchMap(() => {
        // 2. Submit application with the profile data
        const applicationData = {
          applicantId: currentUser.id,
          jobOfferId: projectId,
          motivation: motivation.trim(),
          skills: profilData.skills,
          experienceLevel: profilData.experienceLevel,
          fieldOfStudy: profilData.fieldOfStudy,
          yearsOfExperience: profilData.yearsOfExperience,
          languages: profilData.languages,
          status: 'PENDING',
          matchingScore: 0
        };
        return this.partnershipApi.applyWithProfile(applicationData);
      }),
      timeout(15000)
    ).subscribe({
      next: (response) => {
        this.ngZone.run(() => {
          const jobOffer = this.jobOffers.find(j => j.id === projectId);
          this.successDialogJobTitle = jobOffer?.title || 'Job Offer';
          this.showSuccessDialog = true;

          this.motivations[projectId] = '';
          this.applicationStep[projectId] = 1;
          this.applyingJobId = null;
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.applyingJobId = null;
        const errorMsg = err.error?.message || err.message || `Error: ${err.status || 'Unknown'}`;
        this.submitError = 'Failed to submit application: ' + errorMsg;
        this.errorMessages[projectId] = this.submitError;
        this.cdr.markForCheck();
      }
    });
  }

  addSkill(jobId: number, input: HTMLInputElement): void {
    const value = input.value.trim();
    if (value && !this.profilForms[jobId].skills.includes(value)) {
      this.profilForms[jobId].skills.push(value);
    }
    input.value = '';
  }

  removeSkill(jobId: number, index: number): void {
    this.profilForms[jobId].skills.splice(index, 1);
  }

  addLanguage(jobId: number, input: HTMLInputElement): void {
    const value = input.value.trim();
    if (value && !this.profilForms[jobId].languages.includes(value)) {
      this.profilForms[jobId].languages.push(value);
    }
    input.value = '';
  }

  removeLanguage(jobId: number, index: number): void {
    this.profilForms[jobId].languages.splice(index, 1);
  }

  getMotivationClass(jobId: number): { [key: string]: boolean } {
    const length = this.getMotivationLength(jobId);
    return {
      'text-yellow-600': length > 0 && length < 10,
      'text-green-600': length >= 10
    };
  }

  getMotivationLength(jobId: number): number {
    return (this.motivations[jobId] || '').length;
  }

  // Retry loading job offers
  retryLoadingOffers(): void {
    this.loadJobOffers();
  }

  closeSuccessDialog(): void {
    this.showSuccessDialog = false;
    // Reload the page
    window.location.reload();
  }
}
