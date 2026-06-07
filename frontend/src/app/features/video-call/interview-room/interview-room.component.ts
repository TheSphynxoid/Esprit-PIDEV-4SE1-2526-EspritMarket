import { Component, OnInit, OnDestroy, inject, ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { PartnershipApiService } from '../../../services/api/partnership-api.service';
import { AuthService } from '../../../services/auth.service';
import { InterviewResource } from '../../../services/api/models/api-resource.model';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-interview-room',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './interview-room.component.html',
  styleUrls: ['./interview-room.component.css']
})
export class InterviewRoomComponent implements OnInit, OnDestroy {
  @ViewChild('jitsiContainer', { static: true }) jitsiContainer!: ElementRef;

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private partnershipApi = inject(PartnershipApiService);
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private sanitizer = inject(DomSanitizer);

  interviewId!: number;
  interview: InterviewResource | null = null;
  loading = true;
  errorMessage = '';

  isRecruiter = false;
  jitsiApi: any = null;

  // Mark Result Form (for recruiters)
  resultForm!: FormGroup;
  savingResult = false;
  showSuccessToast = false;
  
  jitsiRoomUrl: SafeResourceUrl | null = null;

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      this.errorMessage = 'Invalid Interview ID';
      this.loading = false;
      return;
    }

    this.interviewId = parseInt(idParam, 10);
    this.isRecruiter = this.authService.hasRole('recruiter')();

    if (this.isRecruiter) {
      this.initResultForm();
    }

    this.loadInterviewDetails();
  }

  ngOnDestroy(): void {
    // Iframe is destroyed naturally by Angular
  }

  private initResultForm(): void {
    this.resultForm = this.fb.group({
      result: ['', Validators.required],
      resultNotes: ['', [Validators.maxLength(1000)]]
    });
  }

  private loadInterviewDetails(): void {
    this.partnershipApi.interviews.getById(this.interviewId).subscribe({
      next: (data) => {
        this.interview = data;
        
        if (this.interview.type !== 'VIDEO') {
          this.errorMessage = 'This interview is not a video interview.';
          this.loading = false;
          this.cdr.detectChanges();
          return;
        }

        if (this.interview.status === 'COMPLETED' || this.interview.status === 'CANCELLED') {
          this.errorMessage = `This interview has already been ${this.interview.status.toLowerCase()}.`;
          this.loading = false;
          this.cdr.detectChanges();
          return;
        }

        this.loading = false;
        
        // Generate secure Jitsi iframe URL
        const roomName = `EspritMarket_Interview_${this.interviewId}_ROOM`;
        const url = `https://meet.jit.si/${roomName}`;
        this.jitsiRoomUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);

        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error fetching interview:', err);
        this.errorMessage = 'Failed to load interview details. You may not have permission to view this or it does not exist.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  leaveRoom(): void {
    // Redirect based on role
    if (this.isRecruiter) {
      this.router.navigate(['/dashboard/recruiter/interviews']);
    } else {
      this.router.navigate(['/my-applications']);
    }
  }

  // --- RECRUITER ACTIONS ---

  saveInterviewResult(): void {
    if (this.resultForm.invalid || !this.interview) return;

    this.savingResult = true;
    const result = this.resultForm.get('result')?.value;
    const notes = this.resultForm.get('resultNotes')?.value;

    const payload: any = {
      status: 'COMPLETED',
      result: result,
      resultNotes: notes
    };

    this.partnershipApi.interviews.update(this.interviewId, payload).subscribe({
      next: () => {
        this.savingResult = false;
        this.showSuccessToast = true;
        this.interview!.status = 'COMPLETED'; // Optimistic update
        this.cdr.detectChanges();
        
        setTimeout(() => {
          this.leaveRoom();
        }, 2000);
      },
      error: (err) => {
        this.savingResult = false;
        alert('Failed to save result: ' + (err.error?.message || err.message));
        this.cdr.detectChanges();
      }
    });
  }
}
