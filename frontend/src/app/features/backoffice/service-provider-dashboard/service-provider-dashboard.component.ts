import { Component, ViewChild, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { BackofficeLayoutComponent, BackofficeNavItem } from '../../../shared/backoffice-layout';
import {
  AuthService,
  SrvApiService
} from '../../../services';
import { ApiConfigService } from '../../../services/api';
import {
  ServiceResponse,
  ProjectResponse,
  ServiceUpsertRequest as ServiceWritePayload,
  BookingResponse,
  WeeklyTemplateResponse as WeeklyTemplateResource,
  ProviderExceptionResponse as ProviderExceptionResource,
  ServiceMandateResponse as ServiceMandateResource,
  ProviderMandateResponse as ProviderMandateResource,
  WeeklyTemplateBatchRequest as WeeklyTemplateBatchPayload
} from '@esprit-market/api-types';
import { ModalComponent } from '../../../shared/components/modal.component';
import { ServiceFormComponent } from '../service-form';
import { WeeklyScheduleEditorComponent } from '../weekly-schedule-editor';
import { BrnTooltipImports, provideBrnTooltipDefaultOptions } from '@spartan-ng/brain/tooltip';
import { bookingStatusBadgeClass, serviceStatusBadgeClass, formatStatusLabel, resolveHttpError } from '../../../shared/utils';

type ServiceDashboardTab = 'overview' | 'services' | 'projects' | 'scheduling' | 'bookings' | 'availability';

const TABS: ServiceDashboardTab[] = ['overview', 'services', 'projects', 'scheduling', 'bookings', 'availability'];
const PAGE_SIZE = 10;
type BookingFilterStatus = 'ALL' | BookingResponse.StatusEnum;

@Component({
  selector: 'app-service-provider-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, BackofficeLayoutComponent, ModalComponent, ServiceFormComponent, WeeklyScheduleEditorComponent, BrnTooltipImports],
  providers: [provideBrnTooltipDefaultOptions({
    tooltipContentClasses: 'bg-neutral-800 text-white text-xs px-3 py-1.5 rounded-md shadow-lg',
  })],
  templateUrl: './service-provider-dashboard.component.html',
  styleUrls: ['./service-provider-dashboard.component.css']
})
export class ServiceProviderDashboardComponent implements OnInit {
  @ViewChild('serviceForm') serviceFormComponent?: ServiceFormComponent;

  private readonly srvApi = inject(SrvApiService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);

  readonly tabs = TABS;
  readonly activeTab = signal<ServiceDashboardTab>('overview');
  readonly navItems: BackofficeNavItem[] = [
    { id: 'overview', label: 'Overview', icon: 'overview' },
    { id: 'services', label: 'Services', icon: 'services' },
    { id: 'projects', label: 'Projects', icon: 'projects' },
    { id: 'scheduling', label: 'Schedule', icon: 'scheduling' },
    { id: 'bookings', label: 'Bookings', icon: 'bookings' },
    { id: 'availability', label: 'Availability', icon: 'availability' }
  ];
  readonly BookingResponse = BookingResponse;
  readonly bookingStatusBadgeClass = bookingStatusBadgeClass;
  readonly serviceStatusBadgeClass = serviceStatusBadgeClass;
  readonly formatStatusLabel = formatStatusLabel;
  readonly bookingFilterOptions: { value: BookingFilterStatus; label: string }[] = [
    { value: 'ALL', label: 'All Statuses' },
    { value: BookingResponse.StatusEnum.Pending, label: 'Pending' },
    { value: BookingResponse.StatusEnum.Approved, label: 'Approved' },
    { value: BookingResponse.StatusEnum.InProgress, label: 'In Progress' },
    { value: BookingResponse.StatusEnum.PendingReview, label: 'Pending Review' },
    { value: BookingResponse.StatusEnum.Disputed, label: 'Disputed' },
    { value: BookingResponse.StatusEnum.Completed, label: 'Completed' },
    { value: BookingResponse.StatusEnum.Cancelled, label: 'Cancelled' },
    { value: BookingResponse.StatusEnum.Rejected, label: 'Rejected' },
  ];

  readonly isServiceModalOpen = signal(false);
  readonly isEditingService = signal(false);
  readonly isSubmittingService = signal(false);
  readonly editingServiceId = signal<number | null>(null);

  readonly services = signal<ServiceResponse[]>([]);
  readonly projects = signal<ProjectResponse[]>([]);
  readonly allBookings = signal<BookingResponse[]>([]);
  readonly providerCalMonth = signal(new Date().toISOString().slice(0, 7));
  readonly providerCalSelectedDate = signal<string | null>(null);
  readonly recentCompletedReviews = signal<{ id?: number; rating?: number; comment?: string; userName?: string }[]>([]);
  readonly bookingPredictions = signal<Map<number, { completionProbability?: number; riskLevel?: string; recommendation?: string }>>(new Map());

  readonly bookingsSearch = signal('');
  readonly bookingsStatusFilter = signal<BookingFilterStatus>('ALL');
  readonly bookingsCurrentPage = signal(0);

  readonly globalTemplates = signal<WeeklyTemplateResource[]>([]);
  readonly serviceTemplates = signal<WeeklyTemplateResource[]>([]);
  readonly selectedScheduleServiceId = signal<number | null>(null);
  readonly providerExceptions = signal<ProviderExceptionResource[]>([]);
  readonly serviceMandates = signal<ServiceMandateResource[]>([]);
  readonly providerMandate = signal<ProviderMandateResource | null>(null);

  readonly newExceptionForm = signal<{ date: string; type: ProviderExceptionResource.TypeEnum; startHour: string; endHour: string; reason: string }>({
    date: '', type: ProviderExceptionResource.TypeEnum.Blocked, startHour: '', endHour: '', reason: ''
  });
  readonly newMandateForm = signal({ serviceId: 0, maxBookings: 5 });
  readonly newProviderMandateForm = signal({ maxBookings: 10 });

  readonly availableServicesCount = computed(() =>
    this.services().filter(s => s.status === 'AVAILABLE').length
  );

  readonly scheduledBookings = computed(() =>
    this.allBookings()
      .filter(b => b.status === BookingResponse.StatusEnum.Approved || b.status === BookingResponse.StatusEnum.InProgress)
      .sort((a, b) => new Date(a.date ?? '').getTime() - new Date(b.date ?? '').getTime())
  );

  readonly pendingBookingsCount = computed(() =>
    this.allBookings().filter(b => b.status === BookingResponse.StatusEnum.Pending).length
  );

  readonly completedReviewsCount = computed(() => this.recentCompletedReviews().length);

  readonly filteredBookings = computed(() => {
    let list = this.allBookings();
    const filter = this.bookingsStatusFilter();
    const search = this.bookingsSearch().toLowerCase().trim();

    if (filter !== 'ALL') {
      list = list.filter(b => b.status === filter);
    }
    if (search) {
      list = list.filter(b =>
        (b.serviceName ?? '').toLowerCase().includes(search) ||
        (b.userName ?? '').toLowerCase().includes(search) ||
        String(b.id).includes(search)
      );
    }
    return list;
  });

  readonly pendingFiltered = computed(() =>
    this.filteredBookings().filter(b => b.status === BookingResponse.StatusEnum.Pending)
  );

  readonly otherFiltered = computed(() =>
    this.filteredBookings().filter(b => b.status !== BookingResponse.StatusEnum.Pending)
  );

  readonly bookingsTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.filteredBookings().length / PAGE_SIZE))
  );

  readonly paginatedBookings = computed(() => {
    const page = this.bookingsCurrentPage();
    const pending = this.pendingFiltered();
    const others = this.otherFiltered();

    if (page === 0) {
      const pendingSlice = pending.slice(0, PAGE_SIZE);
      const remaining = PAGE_SIZE - pendingSlice.length;
      return { pending: pendingSlice, others: others.slice(0, remaining) };
    }

    const pendingOnEarlierPages = Math.min(pending.length, PAGE_SIZE);
    const otherOffset = (page * PAGE_SIZE) - pendingOnEarlierPages;
    return { pending: [], others: others.slice(otherOffset, otherOffset + PAGE_SIZE) };
  });

  readonly selectedScheduleService = computed(() => {
    const id = this.selectedScheduleServiceId();
    return id ? this.services().find(s => s.id === id) ?? null : null;
  });

  readonly providerId = computed(() => this.authService.currentUser()?.id ?? null);
  readonly bookingActionError = signal('');
  readonly providerProjectParticipationEnabled = signal(true);

  ngOnInit(): void {
    this.refreshData();
  }

  setActiveTab(tab: ServiceDashboardTab): void {
    this.activeTab.set(tab);
  }

  onTabChange(tabId: string): void {
    if (TABS.includes(tabId as ServiceDashboardTab)) {
      this.activeTab.set(tabId as ServiceDashboardTab);
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  onBookingsFilterChange(): void {
    this.bookingsCurrentPage.set(0);
  }

  bookingsGoToPage(page: number): void {
    if (page >= 0 && page < this.bookingsTotalPages()) {
      this.bookingsCurrentPage.set(page);
    }
  }

  bookingsPageRange(): number[] {
    const total = this.bookingsTotalPages();
    const current = this.bookingsCurrentPage();
    const start = Math.max(0, current - 2);
    const end = Math.min(total, start + 5);
    return Array.from({ length: end - start }, (_, i) => start + i);
  }

  openCreateServiceModal(): void {
    this.isEditingService.set(false);
    this.editingServiceId.set(null);
    this.isServiceModalOpen.set(true);
    setTimeout(() => this.serviceFormComponent?.resetForm());
  }

  openEditServiceModal(service: ServiceResponse): void {
    this.isEditingService.set(true);
    this.editingServiceId.set(service.id ?? null);
    this.isServiceModalOpen.set(true);
    setTimeout(() => {
      this.serviceFormComponent?.setFormData({
        name: service.name,
        category: service.category,
        description: service.description,
        pricingType: service.pricingType,
        price: service.price,
        imageUrl: service.imageUrl,
        status: service.status
      });
    });
  }

  closeServiceModal(): void {
    this.isServiceModalOpen.set(false);
    this.isEditingService.set(false);
    this.editingServiceId.set(null);
    this.isSubmittingService.set(false);
  }

  submitServiceForm(): void {
    if (!this.serviceFormComponent?.isFormValid()) return;

    const formData = this.serviceFormComponent.getFormData();
    const currentUserId = this.authService.currentUser()?.id;
    const editId = this.editingServiceId();
    const existing = editId ? this.services().find(s => s.id === editId) : null;
    const file = this.serviceFormComponent.selectedFile;
    const schedulePayload = this.serviceFormComponent.getSchedulePayload();

    if (!currentUserId) return;

    const payload: ServiceWritePayload = {
      name: formData.name,
      description: formData.description,
      category: formData.category,
      pricingType: formData.pricingType,
      price: Number(formData.price),
      status: formData.status,
      rating: existing?.rating ?? 0,
      location: 'N/A',
      imageUrl: formData.imageUrl || undefined,
      allowProjectParticipation: existing?.allowProjectParticipation ?? true,
      providerId: currentUserId,
      tags: formData.tags
    };

    this.isSubmittingService.set(true);

    const request = editId
      ? this.srvApi.updateService(editId, payload)
      : this.srvApi.createService(payload);

    request.subscribe({
      next: (savedService) => {
        const serviceId = savedService.id;
        const afterImage = () => {
          if (schedulePayload && serviceId) {
            schedulePayload.providerId = currentUserId;
            schedulePayload.serviceId = serviceId;
            this.srvApi.saveWeeklyTemplatesBatch(schedulePayload).subscribe({
              next: () => this.finishServiceModal(),
              error: () => this.finishServiceModal()
            });
          } else {
            this.finishServiceModal();
          }
        };

        if (file && serviceId) {
          this.srvApi.uploadServiceImage(serviceId, file).subscribe({
            next: afterImage,
            error: afterImage
          });
        } else {
          afterImage();
        }
      },
      error: () => this.isSubmittingService.set(false)
    });
  }

  private finishServiceModal(): void {
    this.activeTab.set('services');
    this.closeServiceModal();
    this.refreshData();
  }

  deleteService(serviceId: number | undefined): void {
    if (serviceId == null) return;
    if (!confirm('Are you sure you want to delete this service? This action cannot be undone.')) return;
    this.srvApi.deleteService(serviceId).subscribe({
      next: () => {
        this.services.update(list => list.filter(s => s.id !== serviceId));
      }
    });
  }

  toggleServiceProjectParticipation(service: ServiceResponse): void {
    if (!service.id) return;
    const next = !(service.allowProjectParticipation ?? true);
    this.srvApi.updateServiceProjectParticipation(service.id, next).subscribe({
      next: (updated) => {
        this.services.update((list) => list.map((s) => s.id === updated.id ? updated : s));
      }
    });
  }

  toggleProviderProjectParticipation(): void {
    const providerId = this.providerId();
    if (!providerId) return;
    const next = !this.providerProjectParticipationEnabled();
    this.srvApi.updateProviderProjectParticipation(providerId, next).subscribe({
      next: () => {
        this.providerProjectParticipationEnabled.set(next);
        this.services.update((list) => list.map((s) => ({ ...s, allowProjectParticipation: next })));
      }
    });
  }

  updateBookingStatus(booking: BookingResponse, status: BookingResponse.StatusEnum): void {
    if (booking.id == null) return;
    this.srvApi.updateBookingStatus(booking.id, { status }).subscribe({
      next: () => {
        this.allBookings.update(list =>
          list.map(b => b.id === booking.id ? { ...b, status } : b)
        );
      }
    });
  }

  cancelBooking(booking: BookingResponse): void {
    if (booking.id == null) return;
    this.srvApi.cancelBooking(booking.id).subscribe({
      next: () => {
        this.allBookings.update(list =>
          list.map(b => b.id === booking.id ? { ...b, status: BookingResponse.StatusEnum.Cancelled } : b)
        );
      }
    });
  }

  submitWork(booking: BookingResponse): void {
    if (booking.id == null) return;
    this.router.navigate(['/bookings', booking.id], { queryParams: { source: 'provider-dashboard' } });
  }

  viewDeliverable(booking: BookingResponse): void {
    if (booking.id == null) return;
    this.router.navigate(['/bookings', booking.id], { queryParams: { source: 'provider-dashboard' } });
  }

  downloadDeliverable(booking: BookingResponse): void {
    if (booking.id == null) return;
    this.bookingActionError.set('');

    this.srvApi.getDeliverablesByBooking(booking.id).subscribe({
      next: (deliverables) => {
        const latest = deliverables[0];
        if (!latest?.id) {
          this.bookingActionError.set('No deliverable found for this booking.');
          return;
        }

        this.srvApi.getDeliverableById(latest.id).subscribe({
          next: (detail) => {
            const attachment = detail.attachments?.[0];
            if (!attachment?.fileUrl) {
              this.bookingActionError.set('No attachment found for this deliverable.');
              return;
            }
            this.downloadFile(attachment.fileUrl, attachment.fileName || 'deliverable-file');
          },
          error: (err) => {
            this.bookingActionError.set(resolveHttpError(err, 'Could not load deliverable details.'));
          }
        });
      },
      error: (err) => {
        this.bookingActionError.set(resolveHttpError(err, 'Could not load deliverables.'));
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
        this.bookingActionError.set(resolveHttpError(err, 'Could not download the file.'));
      }
    });
  }

  onGlobalScheduleSaved(templates: WeeklyTemplateResource[]): void {
    this.globalTemplates.set(templates);
  }

  onServiceScheduleSelected(serviceId: number): void {
    this.selectedScheduleServiceId.set(serviceId);
    const userId = this.providerId();
    if (userId && serviceId) {
      this.srvApi.getWeeklyTemplatesForService(userId, serviceId).subscribe({
        next: (templates) => this.serviceTemplates.set(templates),
        error: () => this.serviceTemplates.set([])
      });
    } else {
      this.serviceTemplates.set([]);
    }
  }

  onServiceScheduleSaved(templates: WeeklyTemplateResource[]): void {
    this.serviceTemplates.set(templates);
  }

  createException(): void {
    const userId = this.providerId();
    if (!userId) return;
    const form = this.newExceptionForm();
    if (!form.date) return;

    this.srvApi.createProviderException({
      providerId: userId,
      date: form.date,
      type: form.type,
      startHour: form.type === ProviderExceptionResource.TypeEnum.CustomHours ? form.startHour : undefined,
      endHour: form.type === ProviderExceptionResource.TypeEnum.CustomHours ? form.endHour : undefined,
      reason: form.reason || undefined
    }).subscribe({
      next: () => {
        this.loadAvailabilityData(userId);
        this.newExceptionForm.set({ date: '', type: ProviderExceptionResource.TypeEnum.Blocked, startHour: '', endHour: '', reason: '' });
      }
    });
  }

  deleteException(id: number | undefined): void {
    if (id == null) return;
    const userId = this.providerId();
    if (!userId) return;
    this.srvApi.deleteProviderException(id).subscribe({
      next: () => this.loadAvailabilityData(userId)
    });
  }

  createServiceMandate(): void {
    const userId = this.providerId();
    if (!userId) return;
    const form = this.newMandateForm();
    if (!form.serviceId) return;

    this.srvApi.createServiceMandate({
      providerId: userId,
      serviceId: form.serviceId,
      maxBookings: form.maxBookings
    }).subscribe({
      next: () => {
        this.loadAvailabilityData(userId);
        this.newMandateForm.set({ serviceId: 0, maxBookings: 5 });
      }
    });
  }

  deleteServiceMandate(id: number | undefined): void {
    if (id == null) return;
    const userId = this.providerId();
    if (!userId) return;
    this.srvApi.deleteServiceMandate(id).subscribe({
      next: () => this.loadAvailabilityData(userId)
    });
  }

  saveProviderMandate(): void {
    const userId = this.providerId();
    if (!userId) return;
    const form = this.newProviderMandateForm();

    this.srvApi.createProviderMandate({
      providerId: userId,
      maxBookings: form.maxBookings
    }).subscribe({
      next: (mandate) => {
        this.providerMandate.set(mandate);
      }
    });
  }

  readonly openPositionProjects = signal<ProjectResponse[]>([]);

  private refreshData(): void {
    const userId = this.authService.currentUser()?.id;
    if (!userId) return;

    this.srvApi.getServices(0, 100).subscribe({
      next: (page) => {
        const mine = page.content.filter(s => s.providerId === userId);
        this.services.set(mine);
        this.providerProjectParticipationEnabled.set(!mine.some(s => s.allowProjectParticipation === false));
      },
      error: () => this.services.set([])
    });

    this.srvApi.getMyProjects(0, 100).subscribe({
      next: (page) => {
        this.projects.set(page.content);
      },
      error: () => this.projects.set([])
    });

    this.srvApi.getOpenPositionProjects(0, 100).subscribe({
      next: (page) => {
        this.openPositionProjects.set(page.content);
      },
      error: () => this.openPositionProjects.set([])
    });

    this.srvApi.getBookingsByProvider(userId, 0, 100).subscribe({
      next: (page) => {
        this.allBookings.set(page.content);
        this.loadBookingPredictions();
      },
      error: () => this.allBookings.set([])
    });

    this.srvApi.getProviderServiceReviews(userId, BookingResponse.StatusEnum.Completed, 0, 5).subscribe({
      next: (page) => {
        this.recentCompletedReviews.set(page.content || []);
      },
      error: () => this.recentCompletedReviews.set([])
    });

    this.loadAvailabilityData(userId);
  }

  private loadBookingPredictions(): void {
    const pending = this.allBookings().filter(b => b.status === BookingResponse.StatusEnum.Pending);
    pending.forEach(b => {
      this.srvApi.getBookingMlPrediction(b.id!).subscribe({
        next: (pred) => {
          const map = new Map(this.bookingPredictions());
          map.set(b.id!, pred);
          this.bookingPredictions.set(map);
        },
        error: () => {}
      });
    });
  }

  private loadAvailabilityData(userId: number): void {
    this.srvApi.getGlobalWeeklyTemplates(userId).subscribe({
      next: (templates) => this.globalTemplates.set(templates),
      error: () => this.globalTemplates.set([])
    });

    this.srvApi.getProviderExceptions(userId).subscribe({
      next: (exceptions) => this.providerExceptions.set(exceptions),
      error: () => this.providerExceptions.set([])
    });

    this.srvApi.getServiceMandates(userId).subscribe({
      next: (mandates) => this.serviceMandates.set(mandates),
      error: () => this.serviceMandates.set([])
    });

    this.srvApi.getProviderMandate(userId).subscribe({
      next: (mandate) => this.providerMandate.set(mandate),
      error: () => this.providerMandate.set(null)
    });

    const selectedId = this.selectedScheduleServiceId();
    if (selectedId) {
      this.srvApi.getWeeklyTemplatesForService(userId, selectedId).subscribe({
        next: (templates) => this.serviceTemplates.set(templates),
        error: () => this.serviceTemplates.set([])
      });
    }
  }

  providerCalMonthLabel(): string {
    const [y, m] = this.providerCalMonth().split('-').map(Number);
    return new Date(y, m - 1, 1).toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
  }

  providerCalNav(delta: number): void {
    const [y, m] = this.providerCalMonth().split('-').map(Number);
    const d = new Date(y, m - 1 + delta, 1);
    this.providerCalMonth.set(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  }

  providerCalToggleDate(date: string): void {
    this.providerCalSelectedDate.set(this.providerCalSelectedDate() === date ? null : date);
  }

  providerCalWeekDays(): string[] {
    return ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  }

  providerCalDays(): Array<{ date: string; dayNum: number; isCurrentMonth: boolean; isToday: boolean; bookings: BookingResponse[] }> {
    const [y, m] = this.providerCalMonth().split('-').map(Number);
    const first = new Date(y, m - 1, 1);
    const startDay = (first.getDay() + 6) % 7;
    const daysInMonth = new Date(y, m, 0).getDate();
    const todayStr = `${new Date().getFullYear()}-${String(new Date().getMonth() + 1).padStart(2, '0')}-${String(new Date().getDate()).padStart(2, '0')}`;
    const bookingsByDate = new Map<string, BookingResponse[]>();
    for (const b of this.allBookings()) {
      if (b.date) {
        const key = b.date.slice(0, 10);
        const arr = bookingsByDate.get(key) || [];
        arr.push(b);
        bookingsByDate.set(key, arr);
      }
    }
    const result: Array<{ date: string; dayNum: number; isCurrentMonth: boolean; isToday: boolean; bookings: BookingResponse[] }> = [];
    for (let i = startDay - 1; i >= 0; i--) {
      const d = new Date(y, m - 1, -i);
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
      result.push({ date: key, dayNum: d.getDate(), isCurrentMonth: false, isToday: key === todayStr, bookings: bookingsByDate.get(key) || [] });
    }
    for (let i = 1; i <= daysInMonth; i++) {
      const d = new Date(y, m - 1, i);
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
      result.push({ date: key, dayNum: i, isCurrentMonth: true, isToday: key === todayStr, bookings: bookingsByDate.get(key) || [] });
    }
    const remaining = 42 - result.length;
    for (let i = 1; i <= remaining; i++) {
      const d = new Date(y, m, i);
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
      result.push({ date: key, dayNum: d.getDate(), isCurrentMonth: false, isToday: key === todayStr, bookings: bookingsByDate.get(key) || [] });
    }
    return result;
  }

  providerCalSelectedBookings(): BookingResponse[] {
    const d = this.providerCalSelectedDate();
    if (!d) return [];
    return this.allBookings().filter(b => b.date && b.date.slice(0, 10) === d);
  }

  providerCalBookingClass(status?: BookingResponse.StatusEnum): string {
    switch (status) {
      case BookingResponse.StatusEnum.Completed: return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400';
      case BookingResponse.StatusEnum.Confirmed: case BookingResponse.StatusEnum.Approved: return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400';
      case BookingResponse.StatusEnum.InProgress: return 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400';
      case BookingResponse.StatusEnum.Cancelled: return 'bg-neutral-100 text-neutral-500 line-through';
      case BookingResponse.StatusEnum.Rejected: case BookingResponse.StatusEnum.Disputed: return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400';
      default: return 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400';
    }
  }

  providerCalBarClass(status?: BookingResponse.StatusEnum): string {
    switch (status) {
      case BookingResponse.StatusEnum.Completed: return 'bg-green-500';
      case BookingResponse.StatusEnum.Confirmed: case BookingResponse.StatusEnum.Approved: return 'bg-blue-500';
      case BookingResponse.StatusEnum.InProgress: return 'bg-purple-500';
      case BookingResponse.StatusEnum.Cancelled: return 'bg-neutral-300';
      case BookingResponse.StatusEnum.Rejected: case BookingResponse.StatusEnum.Disputed: return 'bg-red-500';
      default: return 'bg-amber-500';
    }
  }
}
