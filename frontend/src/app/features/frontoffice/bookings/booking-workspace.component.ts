import { Component, inject, signal, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import { SrvApiService, AuthService, BookingChatRealtimeService, BookingRealtimeMessage } from '../../../services';
import { ApiConfigService } from '../../../services/api';
import {
  BookingResponse,
  DeliverableResponse,
  DeliverableReviewResponse,
  BookingAttachmentResponse
} from '@esprit-market/api-types';

import { bookingStatusPillClass, deliverableStatusClass, resolveHttpError } from '../../../shared/utils';

import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-booking-workspace',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink, HeaderComponent, FooterComponent],
  templateUrl: './booking-workspace.component.html',
  styleUrls: []
})
export class BookingWorkspaceComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  readonly router = inject(Router);
  private readonly srvApi = inject(SrvApiService);
  private readonly authService = inject(AuthService);
  private readonly bookingChatRealtime = inject(BookingChatRealtimeService);
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);
  private readonly fb = inject(FormBuilder);
  private readonly loadingPreviewKeys = new Set<string>();
  private readonly failedPreviewKeys = new Set<string>();
  private readonly messageIds = new Set<number>();

  readonly bookingId = signal<number | null>(null);
  readonly booking = signal<BookingResponse | null>(null);
  readonly isLoading = signal(true);
  readonly error = signal('');
  readonly relatedServices = signal<Array<any>>([]);
  hideRelatedServices = () => this.relatedServices.set([]);
  readonly isProvider = signal(false);
  readonly isBackofficeContext = signal(false);
  readonly currentUserId = signal<number | null>(null);

  readonly sharedFiles = signal<BookingAttachmentResponse[]>([]);
  readonly deliverables = signal<DeliverableResponse[]>([]);
  readonly history = signal<DeliverableReviewResponse[]>([]);
  readonly bookingPrediction = signal<{
    completionProbability?: number;
    riskLevel?: string;
    confidence?: string;
    keyFactors?: string[];
    recommendation?: string;
  } | null>(null);
  readonly versions = signal<Array<{
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
  }>>([]);
  readonly messages = signal<Array<{
    id?: number;
    bookingId?: number;
    senderId?: number;
    senderName?: string;
    message?: string;
    createdAt?: string;
  }>>([]);

  readonly isSubmittingDeliverable = signal(false);
  readonly isSubmittingReview = signal(false);
  readonly isUploadingFile = signal(false);
  readonly selectedFiles = signal<File[]>([]);
  readonly existingDeliverableFiles = signal<File[]>([]);
  readonly reviewDecision = signal('');
  readonly reviewComment = signal('');
  readonly chatMessage = signal('');
  readonly isSendingMessage = signal(false);
  readonly isChatOpen = signal(false);
  readonly isLoadingOlderMessages = signal(false);
  readonly hasOlderMessages = signal(true);
  readonly deliverableCreated = signal(false);
  readonly previewUrls = signal<Record<string, string>>({});

  readonly showReschedule = signal(false);
  readonly rescheduleLoading = signal(false);
  readonly activeReschedule = signal<any>(null);
  readonly rescheduleHistory = signal<any[]>([]);
  readonly rescheduleForm = this.fb.group({
    proposedDate: ['', Validators.required],
    proposedDuration: [1, [Validators.required, Validators.min(0.5)]],
    reason: ['SCHEDULING_CONFLICT'],
    message: ['']
  });
  private chatPollTimer?: ReturnType<typeof setInterval>;
  private chatUnsubscribe?: () => void;
  private readonly handleWindowFocus = () => this.loadMessages();
  private static readonly CHAT_BATCH_SIZE = 30;

  deliverableForm!: FormGroup;
  BookingStatus = BookingResponse.StatusEnum;
  DeliverableStatus = DeliverableResponse.StatusEnum;

  ngOnInit(): void {
    var id = this.route.snapshot.paramMap.get('id');
    this.bookingId.set(id ? Number(id) : null);
    const source = this.route.snapshot.queryParamMap.get('source');
    this.isBackofficeContext.set(source === 'provider-dashboard');

    this.deliverableForm = this.fb.group({
      title: ['', [Validators.required]],
      description: ['']
    });

    if (this.bookingId()) {
      this.loadWorkspace();
      this.connectRealtimeChat();
      this.loadRescheduleData();
      window.addEventListener('focus', this.handleWindowFocus);
    }
  }

  ngOnDestroy(): void {
    Object.values(this.previewUrls()).forEach((url) => URL.revokeObjectURL(url));
    if (this.chatPollTimer) {
      clearInterval(this.chatPollTimer);
    }
    if (this.chatUnsubscribe) {
      this.chatUnsubscribe();
    }
    window.removeEventListener('focus', this.handleWindowFocus);
  }

  get canCreateDeliverable(): boolean {
    const status = this.booking()?.status;
    return this.isProvider() &&
      (status === BookingResponse.StatusEnum.InProgress || status === BookingResponse.StatusEnum.Confirmed) &&
      this.deliverables().length === 0;
  }

  get canSubmitDeliverable(): boolean {
    if (!this.isProvider() || this.deliverables().length === 0) return false;
    var del = this.deliverables()[0];
    return del.status === DeliverableResponse.StatusEnum.Draft ||
           del.status === DeliverableResponse.StatusEnum.RevisionRequested;
  }

  get canReviewDeliverable(): boolean {
    if (this.isProvider() || this.deliverables().length === 0) return false;
    var del = this.deliverables()[0];
    return del.status === DeliverableResponse.StatusEnum.Submitted;
  }

  get canRequestReschedule(): boolean {
    const b = this.booking();
    if (!b?.id || this.activeReschedule()) return false;
    return b.status === BookingResponse.StatusEnum.Pending ||
           b.status === BookingResponse.StatusEnum.Approved ||
           b.status === BookingResponse.StatusEnum.Tentative ||
           b.status === BookingResponse.StatusEnum.Confirmed;
  }

  get canActOnReschedule(): boolean {
    return !!this.activeReschedule() && this.activeReschedule().status === 'PENDING';
  }

  toggleReschedule(): void {
    this.showReschedule.update(v => !v);
    if (this.showReschedule()) {
      this.loadRescheduleData();
    }
  }

  submitReschedule(): void {
    const bookingId = this.bookingId();
    if (!bookingId || !this.rescheduleForm.valid) return;
    this.rescheduleLoading.set(true);
    this.error.set('');
    const form = this.rescheduleForm.value;
    this.srvApi.requestReschedule(bookingId, {
      proposedDate: form.proposedDate || '',
      proposedDuration: Number(form.proposedDuration),
      reason: form.reason || undefined,
      message: form.message || undefined
    }).subscribe({
      next: () => {
        this.rescheduleLoading.set(false);
        this.showReschedule.set(false);
        this.rescheduleForm.reset({ proposedDate: '', proposedDuration: 1, reason: 'SCHEDULING_CONFLICT', message: '' });
        this.loadRescheduleData();
      },
      error: (err) => {
        this.rescheduleLoading.set(false);
        this.error.set(resolveHttpError(err, 'Could not request a reschedule. Please try again.'));
      }
    });
  }

  acceptReschedule(): void {
    const reqId = this.activeReschedule()?.id;
    if (!reqId) return;
    this.rescheduleLoading.set(true);
    const responseMessage = prompt('Optional response message:') || undefined;
    this.srvApi.acceptReschedule(reqId, responseMessage).subscribe({
      next: () => {
        this.rescheduleLoading.set(false);
        this.loadRescheduleData();
        this.loadWorkspace();
      },
      error: (err) => {
        this.rescheduleLoading.set(false);
        this.error.set(resolveHttpError(err, 'Could not accept the reschedule. Please try again.'));
      }
    });
  }

  rejectReschedule(): void {
    const reqId = this.activeReschedule()?.id;
    if (!reqId || !confirm('Reject this reschedule request?')) return;
    this.rescheduleLoading.set(true);
    const responseMessage = prompt('Optional response message:') || undefined;
    this.srvApi.rejectReschedule(reqId, responseMessage).subscribe({
      next: () => {
        this.rescheduleLoading.set(false);
        this.loadRescheduleData();
      },
      error: (err) => {
        this.rescheduleLoading.set(false);
        this.error.set(resolveHttpError(err, 'Could not reject the reschedule. Please try again.'));
      }
    });
  }

  cancelReschedule(): void {
    const reqId = this.activeReschedule()?.id;
    if (!reqId || !confirm('Cancel this reschedule request?')) return;
    this.rescheduleLoading.set(true);
    this.srvApi.cancelReschedule(reqId).subscribe({
      next: () => {
        this.rescheduleLoading.set(false);
        this.loadRescheduleData();
      },
      error: (err) => {
        this.rescheduleLoading.set(false);
        this.error.set(resolveHttpError(err, 'Could not cancel the reschedule. Please try again.'));
      }
    });
  }

  private loadRescheduleData(): void {
    const bookingId = this.bookingId();
    if (!bookingId) return;
    this.srvApi.getActiveReschedule(bookingId).subscribe({
      next: (req) => this.activeReschedule.set(req || null),
      error: () => this.activeReschedule.set(null)
    });
    this.srvApi.getRescheduleHistory(bookingId).subscribe({
      next: (history) => this.rescheduleHistory.set(history || []),
      error: () => this.rescheduleHistory.set([])
    });
  }

  onDeliverableFilesSelected(event: Event): void {
    var input = event.target as HTMLInputElement;
    if (input.files) {
      this.selectedFiles.set(Array.from(input.files));
    }
  }

  removeDeliverableFile(index: number): void {
    this.selectedFiles.set(this.selectedFiles().filter((_, i) => i !== index));
  }

  createAndSubmitDeliverable(): void {
    if (!this.deliverableForm.valid || !this.bookingId()) return;
    this.isSubmittingDeliverable.set(true);
    this.error.set('');

    var formValue = this.deliverableForm.value;
    var files = this.selectedFiles();

    this.srvApi.createDeliverable(
      this.bookingId()!,
      formValue.title,
      formValue.description || '',
      files.length > 0 ? files : undefined
    ).subscribe({
      next: (deliverable: DeliverableResponse) => {
        if (deliverable.id) {
          this.srvApi.submitDeliverable(deliverable.id).subscribe({
            next: () => {
              this.isSubmittingDeliverable.set(false);
              this.deliverableCreated.set(true);
              this.loadWorkspace();
            },
            error: (err) => {
              this.isSubmittingDeliverable.set(false);
              this.error.set(resolveHttpError(err, 'Could not submit the deliverable. Please try again.'));
            }
          });
        } else {
          this.isSubmittingDeliverable.set(false);
          this.deliverableCreated.set(true);
          this.loadWorkspace();
        }
      },
      error: (err) => {
        this.isSubmittingDeliverable.set(false);
        this.error.set(resolveHttpError(err, 'Could not create a deliverable. Please try again.'));
      }
    });
  }

  submitExistingDeliverable(): void {
    var del = this.deliverables()[0];
    if (!del?.id) return;
    this.isSubmittingDeliverable.set(true);
    const files = this.existingDeliverableFiles();

    const submitNow = () => {
      this.srvApi.submitDeliverable(del.id!).subscribe({
        next: () => {
          this.isSubmittingDeliverable.set(false);
          this.existingDeliverableFiles.set([]);
          this.loadWorkspace();
        },
        error: (err: any) => {
          this.isSubmittingDeliverable.set(false);
          this.error.set(resolveHttpError(err, 'Could not submit. Please try again.'));
        }
      });
    };

    if (files.length > 0) {
      forkJoin(files.map(file => this.srvApi.addDeliverableAttachment(del.id!, file))).subscribe({
        next: () => submitNow(),
        error: (err: any) => {
          this.isSubmittingDeliverable.set(false);
          this.error.set(resolveHttpError(err, 'Could not upload the revised files. Please try again.'));
        }
      });
      return;
    }

    submitNow();
  }

  onExistingDeliverableFilesSelected(event: Event): void {
    var input = event.target as HTMLInputElement;
    if (input.files) {
      this.existingDeliverableFiles.set(Array.from(input.files));
    }
  }

  removeExistingDeliverableFile(index: number): void {
    this.existingDeliverableFiles.set(this.existingDeliverableFiles().filter((_, i) => i !== index));
  }

  reviewDeliverable(): void {
    var del = this.deliverables()[0];
    if (!del?.id || !this.reviewDecision()) return;
    this.isSubmittingReview.set(true);
    this.srvApi.reviewDeliverable(del.id, this.reviewDecision(), this.reviewComment() || undefined).subscribe({
      next: () => {
        this.isSubmittingReview.set(false);
        this.reviewDecision.set('');
        this.reviewComment.set('');
        this.loadWorkspace();
      },
      error: (err) => {
        this.isSubmittingReview.set(false);
        this.error.set(resolveHttpError(err, 'Could not submit the review. Please try again.'));
      }
    });
  }

  sendMessage(): void {
    const bookingId = this.bookingId();
    const message = this.chatMessage().trim();
    if (!bookingId || !message) return;

    this.isSendingMessage.set(true);
    this.srvApi.sendBookingMessage(bookingId, message).subscribe({
      next: (sent) => {
        if (sent?.id && !this.messageIds.has(sent.id)) {
          this.messageIds.add(sent.id);
          this.messages.update((existing) => [...existing, sent]);
        }
        this.chatMessage.set('');
        this.isSendingMessage.set(false);
      },
      error: (err) => {
        this.isSendingMessage.set(false);
        this.error.set(resolveHttpError(err, 'Could not send the message. Please try again.'));
      }
    });
  }

  toggleChat(): void {
    const open = !this.isChatOpen();
    this.isChatOpen.set(open);
    if (open) {
      this.loadLatestMessagesBatch();
    }
  }

  loadOlderMessages(): void {
    if (this.isLoadingOlderMessages() || !this.hasOlderMessages()) {
      return;
    }

    const existing = this.messages();
    if (existing.length === 0) {
      this.loadLatestMessagesBatch();
      return;
    }

    const firstWithId = existing.find((m) => m.id != null);
    if (!firstWithId?.id) {
      this.hasOlderMessages.set(false);
      return;
    }

    this.isLoadingOlderMessages.set(true);
    this.loadMessagesBatch(firstWithId.id, true);
  }

  onSharedFileSelected(event: Event): void {
    var input = event.target as HTMLInputElement;
    if (!input.files || !input.files[0] || !this.bookingId()) return;
    this.isUploadingFile.set(true);
    this.srvApi.uploadBookingAttachment(this.bookingId()!, input.files[0]).subscribe({
      next: () => {
        this.isUploadingFile.set(false);
        this.loadSharedFiles();
      },
      error: (err) => {
        this.isUploadingFile.set(false);
        this.error.set(resolveHttpError(err, 'Could not upload the file. Please try again.'));
      }
    });
  }

  deleteSharedFile(attachmentId: number | undefined): void {
    if (!attachmentId || !this.bookingId()) return;
    this.srvApi.deleteBookingAttachment(this.bookingId()!, attachmentId).subscribe({
      next: () => this.loadSharedFiles(),
      error: (err) => this.error.set(resolveHttpError(err, 'Could not delete. Please try again.'))
    });
  }

  goBack(): void {
    if (this.isBackofficeContext()) {
      this.router.navigate(['/dashboard/service-provider']);
      return;
    }
    this.router.navigate(['/bookings']);
  }

  getStatusClass(status: BookingResponse.StatusEnum | undefined): string {
    return bookingStatusPillClass(status);
  }

  getDeliverableStatusClass(status: string | undefined): string {
    return deliverableStatusClass(status);
  }

  isImageFile(fileType: string | undefined, fileName: string | undefined, fileUrl: string | undefined): boolean {
    if (fileType && fileType.toLowerCase().startsWith('image/')) {
      return true;
    }

    const value = `${fileName ?? ''} ${fileUrl ?? ''}`.toLowerCase();
    return /\.(jpg|jpeg|png|gif|webp|bmp|svg)(\?|$)/.test(value);
  }

  getPreviewUrl(fileUrl: string | undefined, fileType: string | undefined, fileName: string | undefined): string | undefined {
    if (!fileUrl || !this.isImageFile(fileType, fileName, fileUrl)) {
      return undefined;
    }

    const cached = this.previewUrls()[fileUrl];
    if (cached) {
      return cached;
    }

    if (this.loadingPreviewKeys.has(fileUrl)) {
      return undefined;
    }

    if (this.failedPreviewKeys.has(fileUrl)) {
      return undefined;
    }

    this.loadingPreviewKeys.add(fileUrl);
    this.http.get(this.apiConfig.buildAssetUrl(fileUrl), { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const objectUrl = URL.createObjectURL(blob);
        this.previewUrls.update((map) => ({ ...map, [fileUrl]: objectUrl }));
        this.loadingPreviewKeys.delete(fileUrl);
      },
      error: () => {
        this.failedPreviewKeys.add(fileUrl);
        this.loadingPreviewKeys.delete(fileUrl);
      }
    });

    return undefined;
  }

  hasFailedPreview(fileUrl: string | undefined): boolean {
    return !!fileUrl && this.failedPreviewKeys.has(fileUrl);
  }

  downloadFile(fileUrl: string | undefined, fileName: string | undefined): void {
    if (!fileUrl) return;
    this.http.get(this.apiConfig.buildAssetUrl(fileUrl), { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const objectUrl = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = objectUrl;
        anchor.download = fileName || 'download';
        document.body.appendChild(anchor);
        anchor.click();
        document.body.removeChild(anchor);
        URL.revokeObjectURL(objectUrl);
      },
      error: () => {
        this.error.set('This file is no longer available on the server. Please upload it again.');
      }
    });
  }

  downloadLatestDeliverable(): void {
    const del = this.deliverables()[0];
    if (!del?.attachments || del.attachments.length === 0) {
      this.error.set('No deliverable attachment found to download.');
      return;
    }

    const attachment = del.attachments[0];
    this.downloadFile(attachment.fileUrl, attachment.fileName || 'deliverable-file');
  }

  private loadWorkspace(): void {
    var id = this.bookingId();
    if (!id) return;

    this.srvApi.getBookingById(id).subscribe({
      next: (b) => {
        this.booking.set(b);
        var user = this.authService.currentUser();
        this.isProvider.set(!!user && !!b && user.id === b.providerId);
        this.currentUserId.set(user?.id ?? null);
        this.isLoading.set(false);
        this.loadBookingPrediction(b);
        this.loadRelatedServices(b);
      },
      error: () => {
        this.booking.set(null);
        this.isLoading.set(false);
      }
    });

    this.loadSharedFiles();

    this.srvApi.getDeliverablesByBooking(id).subscribe({
      next: (dels) => {
        if (dels.length > 0 && dels[0].id) {
          const deliverableId = dels[0].id;

          this.srvApi.getDeliverableById(deliverableId).subscribe({
            next: (detail) => this.deliverables.set([detail]),
            error: () => this.deliverables.set(dels)
          });

          this.srvApi.getDeliverableHistory(deliverableId).subscribe({
            next: (h) => this.history.set(h),
            error: () => this.history.set([])
          });

          this.srvApi.getDeliverableVersions(deliverableId).subscribe({
            next: (versions) => this.versions.set(versions),
            error: () => this.versions.set([])
          });
        } else {
          this.deliverables.set([]);
          this.history.set([]);
          this.versions.set([]);
        }
      },
      error: () => {
        this.deliverables.set([]);
        this.history.set([]);
        this.versions.set([]);
      }
    });
  }

  private loadSharedFiles(): void {
    var id = this.bookingId();
    if (!id) return;
    this.srvApi.getBookingAttachments(id).subscribe({
      next: (files) => this.sharedFiles.set(files),
      error: () => this.sharedFiles.set([])
    });
  }

  private loadMessages(): void {
    const existing = this.messages();
    if (existing.length === 0) {
      this.loadLatestMessagesBatch();
      return;
    }

    const lastWithId = [...existing].reverse().find((m) => m.id != null);
    if (!lastWithId?.id) {
      this.loadLatestMessagesBatch();
      return;
    }

    this.srvApi.getBookingMessagesNewer(this.bookingId()!, lastWithId.id, BookingWorkspaceComponent.CHAT_BATCH_SIZE).subscribe({
      next: (items) => {
        if (!items?.length) {
          return;
        }

        const incoming = items.filter((m) => m.id == null || !this.messageIds.has(m.id));

        incoming.forEach((m) => {
          if (m.id != null) {
            this.messageIds.add(m.id);
          }
        });

        if (incoming.length > 0) {
          this.messages.update((current) => [...current, ...incoming]);
        }
      },
      error: () => {}
    });
  }

  private loadLatestMessagesBatch(): void {
    this.messageIds.clear();
    if (this.messages().length > 0) {
      this.messages.set([]);
    }
    this.hasOlderMessages.set(true);
    this.loadMessagesBatch(undefined, false);
  }

  private loadMessagesBatch(beforeId?: number, prepend = false): void {
    const id = this.bookingId();
    if (!id) {
      this.isLoadingOlderMessages.set(false);
      return;
    }

    this.srvApi.getBookingMessagesBatch(id, beforeId, BookingWorkspaceComponent.CHAT_BATCH_SIZE).subscribe({
      next: (batch) => {
        const items = (batch?.items || []).sort((a, b) => {
          const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0;
          const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0;
          return ta - tb;
        });

        items.forEach((m) => {
          if (m.id != null) {
            this.messageIds.add(m.id);
          }
        });

        if (prepend) {
          this.messages.update((existing) => [...items, ...existing]);
        } else if (items.length > 0 || this.messages().length > 0) {
          this.messages.set(items);
        }

        this.hasOlderMessages.set(!!batch?.hasMore);
        this.isLoadingOlderMessages.set(false);
      },
      error: () => {
        this.isLoadingOlderMessages.set(false);
      }
    });
  }

  private connectRealtimeChat(): void {
    this.loadLatestMessagesBatch();

    const id = this.bookingId();
    if (!id) {
      return;
    }

    this.chatUnsubscribe = this.bookingChatRealtime.subscribeToBookingMessages(
      id,
      (incoming: BookingRealtimeMessage) => {
        if (!incoming?.id) {
          this.loadMessages();
          return;
        }

        if (this.messageIds.has(incoming.id)) {
          return;
        }

        this.messages.update((existing) => {
          if (existing.some((m) => m.id === incoming.id)) {
            return existing;
          }
          this.messageIds.add(incoming.id!);
          return [...existing, incoming];
        });
      }
    );

    this.chatPollTimer = setInterval(() => {
      this.loadMessages();
    }, 5000);
  }

  private loadRelatedServices(b: BookingResponse): void {
    if (b.status !== 'COMPLETED' || !b.serviceId) return;
    this.srvApi.getRelatedServices(b.serviceId, 0, 6).subscribe({
      next: (page) => {
        this.relatedServices.set((page.content || []).filter((s: any) => s.id !== b.serviceId));
      },
      error: () => {}
    });
  }

  goToService(serviceId: number): void {
    this.router.navigate(['/services', serviceId]);
  }

  private loadBookingPrediction(b: BookingResponse): void {
    const activeStatuses: string[] = ['PENDING', 'PENDING_EVALUATION', 'TENTATIVE', 'APPROVED', 'CONFIRMED', 'IN_PROGRESS'];
    if (activeStatuses.includes(b.status ?? '') && b.id) {
      this.srvApi.getBookingMlPrediction(b.id).subscribe({
        next: (pred) => this.bookingPrediction.set(pred),
        error: () => {}
      });
    }
  }
}
