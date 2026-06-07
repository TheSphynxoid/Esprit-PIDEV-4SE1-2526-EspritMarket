import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { EventPlanningApiService, EventResource, EventWritePayload, ReservationWritePayload, UpcomingEventAlert } from '../../../services';
import { ModalComponent } from '../../../shared/components/modal.component';
import { ToastContainerComponent } from '../../../shared/components/toast-container.component';
import { ToastService } from '../../../shared/services/toast.service';
import { FooterComponent } from '../../../shared/layout/footer.component';

interface EventDashboardItem {
  id: number;
  name: string;
  description: string;
  date: string;
  location: string;
  online: boolean;
}

interface EquipmentStockItem {
  key: string;
  label: string;
  total: number;
  reserved: number;
}

interface PendingEquipmentReservationItem {
  equipmentKey: string;
  label: string;
  quantity: number;
}

interface EquipmentReservationEntry {
  id: number;
  eventId: number;
  eventName: string;
  equipmentKey: string;
  equipmentLabel: string;
  quantity: number;
  reservedAt: string;
  imageUrl?: string;
}

interface StallReservationEntry {
  id: number;
  eventId: number;
  eventName: string;
  stallName: string;
  stallNumber: number;
  block: string;
  reservedAt: string;
}

@Component({
  selector: 'app-events-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, ModalComponent, ToastContainerComponent, FooterComponent],
  templateUrl: './events-admin-dashboard.component.html',
  styleUrls: ['./events-admin-dashboard.component.css']
})
export class EventsAdminDashboardComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  private eventPlanningApi = inject(EventPlanningApiService);
  private fb = inject(FormBuilder);
  private toastService = inject(ToastService);
  
  activeTab = 'events';
  eventsSubTab = 'events'; // 'events'
  reservationsSubTab = 'reservations'; // 'reservations', 'stalls', 'equipments', 'equipmentReservations'

  events: EventDashboardItem[] = [];
  filteredEvents: EventDashboardItem[] = [];
  upcomingAlerts: UpcomingEventAlert[] = [];

  selectedType: 'all' | 'online' | 'offline' = 'all';
  searchQuery = '';

  // Pagination
  recentEventsCurrentPage = 1;
  recentEventsPerPage = 4;

  get totalEvents(): number {
    return this.events.length;
  }

  get onlineEvents(): number {
    return this.events.filter((event) => event.online).length;
  }

  get offlineEvents(): number {
    return this.events.filter((event) => !event.online).length;
  }

  get inPersonEvents(): EventDashboardItem[] {
    return this.events.filter((event) => !event.online);
  }

  get recentEvents(): EventDashboardItem[] {
    const sorted = [...this.events]
      .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
    const start = (this.recentEventsCurrentPage - 1) * this.recentEventsPerPage;
    const end = start + this.recentEventsPerPage;
    return sorted.slice(start, end);
  }

  get totalRecentPages(): number {
    return Math.ceil(this.events.length / this.recentEventsPerPage);
  }

  nextRecentPage(): void {
    if (this.recentEventsCurrentPage < this.totalRecentPages) {
      this.recentEventsCurrentPage++;
    }
  }

  previousRecentPage(): void {
    if (this.recentEventsCurrentPage > 1) {
      this.recentEventsCurrentPage--;
    }
  }

  organizers: Array<{ name: string; organization: string; eventCount: number; rating: number }> = [];

  submissions: Array<{ id: number; title: string; description: string; date: string; location: string; submittedBy: string; eventId?: number }> = [];

  // Stalls and Equipments
  stalls: any[] = [];
  equipments: any[] = [];

  // Equipment list filtering & pagination (show one equipment per page for a selected event)
  selectedEquipmentEventId: number | null = null;
  equipmentCurrentPage = 1;
  equipmentPerPage = 1; // one equipment per page as requested

  // Search by event name for equipments
  equipmentEventSearch = '';

  get filteredEquipmentsByEvent(): any[] {
    const search = String(this.equipmentEventSearch || '').toLowerCase().trim();
    let list = this.equipments;

    if (this.selectedEquipmentEventId) {
      list = list.filter((e: any) => e.event?.id === this.selectedEquipmentEventId);
    } else if (search) {
      list = list.filter((e: any) => (e.event?.name || '').toLowerCase().includes(search));
    }

    return list;
  }

  get totalEquipmentPages(): number {
    return Math.max(1, Math.ceil(this.filteredEquipmentsByEvent.length / this.equipmentPerPage));
  }

  get equipmentsForCurrentPage(): any[] {
    const list = this.filteredEquipmentsByEvent;
    const start = (this.equipmentCurrentPage - 1) * this.equipmentPerPage;
    return list.slice(start, start + this.equipmentPerPage);
  }

  nextEquipmentPage(): void {
    if (this.equipmentCurrentPage < this.totalEquipmentPages) this.equipmentCurrentPage++;
  }

  previousEquipmentPage(): void {
    if (this.equipmentCurrentPage > 1) this.equipmentCurrentPage--;
  }

  selectEquipmentEvent(eventId: number | null): void {
    this.selectedEquipmentEventId = eventId;
    this.equipmentCurrentPage = 1;
  }

  onEquipmentEventSearchChange(value: string): void {
    this.equipmentEventSearch = value;
    this.equipmentCurrentPage = 1;
    if (value && value.trim().length > 0) {
      this.selectedEquipmentEventId = null;
    }
  }

  /**
   * Automatically delete equipments whose event date has passed (compared to today)
   */
  private deleteExpiredEquipments(): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // Find equipments linked to events with past dates
    const expiredEquipmentIds: number[] = [];

    for (const equipment of this.equipments) {
      if (equipment.event?.id && equipment.event?.date) {
        const eventDate = new Date(equipment.event.date);
        eventDate.setHours(0, 0, 0, 0);

        // If event date is in the past, mark equipment for deletion
        if (eventDate < today) {
          expiredEquipmentIds.push(equipment.id);
        }
      }
    }

    // Delete all expired equipments
    for (const equipmentId of expiredEquipmentIds) {
      this.eventPlanningApi.equipment.delete(equipmentId).subscribe({
        next: () => {
          console.log(`Equipment ${equipmentId} deleted due to expired event date`);
          this.equipments = this.equipments.filter(e => e.id !== equipmentId);
        },
        error: (err) => {
          console.error(`Failed to delete equipment ${equipmentId}:`, err);
        }
      });
    }
  }

  /**
   * Automatically delete events whose date has passed (compared to today)
   */
  private deleteExpiredEvents(): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // Find events with past dates
    const expiredEventIds: number[] = [];

    for (const event of this.events) {
      const eventDate = new Date(event.date);
      eventDate.setHours(0, 0, 0, 0);

      // If event date is in the past, mark event for deletion
      if (eventDate < today) {
        expiredEventIds.push(event.id);
      }
    }

    // Delete all expired events
    for (const eventId of expiredEventIds) {
      this.eventPlanningApi.events.delete(eventId).subscribe({
        next: () => {
          console.log(`Event ${eventId} deleted due to expired date`);
          this.events = this.events.filter(e => e.id !== eventId);
          this.applyFilters();
        },
        error: (err) => {
          console.error(`Failed to delete event ${eventId}:`, err);
        }
      });
    }
  }

  // CRUD modal properties
  isModalOpen = false;
  modalTitle = '';
  isSubmitting = false;
  isViewMode = false;
  editingEvent: EventResource | null = null;

  // Reservation modal properties
  isReservationModalOpen = false;
  reservationModalTitle = '';
  isReservationSubmitting = false;
  reservationEvent: EventDashboardItem | null = null;
  selectedEquipments: Array<{ id: number; name: string; quantity: number }> = [];

  // Stall modal properties
  isStallModalOpen = false;
  stallModalTitle = '';
  isStallSubmitting = false;
  selectedStallBlock = 'A';
  espritBlocks: string[] = ['A', 'B', 'C', 'D', 'E', 'IJK', 'G', 'M'];
  editingStall: any = null;

  editingEquipment: any = null;

  // Equipment modal properties
  isEquipmentModalOpen = false;
  equipmentModalTitle = '';
  isEquipmentSubmitting = false;

  eventForm: FormGroup = this.fb.group({
    name: ['', Validators.required],
    date: ['', [Validators.required, this.futureOrPresentValidator.bind(this)]],
    description: [''],
    location: ['', Validators.required],
    online: [false],
    nbTickets: [0, [Validators.min(0), Validators.max(140)]]
  });

  reservationForm: FormGroup = this.fb.group({
    name: ['', Validators.required],
    date: ['', [Validators.required, this.futureOrPresentValidator.bind(this)]]
  });

  stallForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    number: [1, [Validators.required, Validators.min(1)]],
    location: ['A', [Validators.required, this.stallLocationValidator.bind(this)]],
    eventId: ['', Validators.required]
  });

  equipmentForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, this.equipmentNameValidator.bind(this)]],
    type: ['', [Validators.required, this.equipmentTypeValidator.bind(this)]],
    status: ['AVAILABLE', Validators.required],
    quantity: [1, [Validators.required, Validators.min(1)]],
    eventId: ['', Validators.required]
  });

  // Valid equipment names (from backend pattern)
  validEquipmentNames = ['Chairs', 'Tables', 'Microphone', 'Speaker', 'Projector', 'Screen', 'Lighting', 'Stage', 'Tent'];
  
  // Valid equipment types (from backend pattern)
  validEquipmentTypes = ['Furniture', 'Audio', 'Decoration', 'Visual', 'Lighting', 'Structure'];
  
  // Valid statuses (from backend pattern)
  validStatuses = ['AVAILABLE', 'IN_USE', 'MAINTENANCE'];

  // Custom validators
  equipmentNameValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    return this.validEquipmentNames.includes(control.value) ? null : { invalidEquipmentName: true };
  }

  equipmentTypeValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    return this.validEquipmentTypes.includes(control.value) ? null : { invalidEquipmentType: true };
  }

  stallLocationValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    return this.espritBlocks.includes(control.value) ? null : { invalidStallLocation: true };
  }

  // Recommended equipment
  recommendedEquipments: Array<{ name: string; type: string; quantity: number; description: string }> = [];
  isShowingRecommendations = false;

  equipmentStock: EquipmentStockItem[] = [
    { key: 'chairs', label: 'Chaises', total: 500, reserved: 0 },
    { key: 'tables', label: 'Tables', total: 100, reserved: 0 },
    { key: 'largeTables', label: 'Grandes tables', total: 20, reserved: 0 },
    { key: 'projectors', label: 'Projecteurs', total: 50, reserved: 0 },
    { key: 'speakers', label: 'Bafles', total: 80, reserved: 0 }
  ];

  pendingEquipmentReservations: PendingEquipmentReservationItem[] = [];
  equipmentReservations: EquipmentReservationEntry[] = [];
  equipmentReservationSearchQuery = '';

  get filteredEquipmentReservations(): EquipmentReservationEntry[] {
    const query = this.normalizeSearchValue(this.equipmentReservationSearchQuery);

    return this.equipmentReservations.filter((reservation) => {
      const matchesSearch =
        !query ||
        this.normalizeSearchValue(reservation.equipmentLabel).includes(query) ||
        this.normalizeSearchValue(reservation.eventName).includes(query) ||
        this.normalizeSearchValue(String(reservation.quantity)).includes(query) ||
        this.normalizeSearchValue(new Date(reservation.reservedAt).toLocaleDateString()).includes(query);

      return matchesSearch;
    });
  }

  constructor() {
    this.eventForm.get('online')?.valueChanges.subscribe((online) => {
      this.toggleLocationControl(!!online);
    });
    this.toggleLocationControl(!!this.eventForm.get('online')?.value);
    this.refreshData();
  }

  private toggleLocationControl(online: boolean): void {
    const locationControl = this.eventForm.get('location');
    if (!locationControl) return;

    // Location is always required according to backend validation
    locationControl.enable({ emitEvent: false });
    locationControl.setValidators(Validators.required);
    locationControl.updateValueAndValidity();
  }

  get minEventDate(): string {
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  futureOrPresentValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (!value) {
      return null;
    }

    const selectedDate = new Date(value);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    selectedDate.setHours(0, 0, 0, 0);

    return selectedDate < today ? { futureOrPresent: true } : null;
  }

  applyFilters(): void {
    const query = this.searchQuery.trim().toLowerCase();

    this.filteredEvents = this.events.filter((event) => {
      const matchesType =
        this.selectedType === 'all' ||
        (this.selectedType === 'online' && event.online) ||
        (this.selectedType === 'offline' && !event.online);

      // Cherche dans le nom, localisation et date
      const matchesSearch = 
        event.name.toLowerCase().includes(query) ||
        event.location.toLowerCase().includes(query) ||
        event.date.includes(query);

      return matchesType && matchesSearch;
    });
  }

  private refreshData(): void {
    this.eventPlanningApi.events.list().subscribe({
      next: (events) => {
        this.events = events.map((event) => ({
          id: event.id,
          name: event.name,
          description: (event as any).description ? (event as any).description : `Location: ${event.location}`,
          date: event.date,
          location: event.location,
          online: (event as any).online !== undefined ? (event as any).online : String(event.location || '').toLowerCase().includes('online')
        }));

        // Delete expired events
        this.deleteExpiredEvents();
        this.applyFilters();
      },
      error: () => {
        this.events = [];
        this.filteredEvents = [];
      }
    });

    // Load upcoming event alerts
    this.eventPlanningApi.getUpcomingEventAlerts().subscribe({
      next: (alerts) => {
        this.upcomingAlerts = alerts;
        
        // Show toast notifications for each upcoming event
        if (alerts.length > 0) {
          alerts.forEach(alert => {
            const timing = alert.daysText === 'aujourd\'hui' ? '🔴 AUJOURD\'HUI' : '🟠 DEMAIN';
            this.toastService.warning(
              `${timing}: ${alert.name} (${alert.location})`,
              8000
            );
          });
        }
      },
      error: () => {
        this.upcomingAlerts = [];
      }
    });

    this.eventPlanningApi.collaborations.list().subscribe({
      next: (collaborations) => {
        const eventCountByName = new Map<string, number>();
        collaborations.forEach((collaboration) => {
          const key = collaboration.name;
          eventCountByName.set(key, (eventCountByName.get(key) ?? 0) + 1);
        });

        this.organizers = collaborations.map((collaboration) => ({
          name: collaboration.name,
          organization: collaboration.type,
          eventCount: eventCountByName.get(collaboration.name) ?? 1,
          rating: 0
        }));
      },
      error: () => {
        this.organizers = [];
      }
    });

    this.eventPlanningApi.reservations.list().subscribe({
      next: (reservations) => {
        this.submissions = reservations.map((reservation) => ({
          id: reservation.id,
          title: reservation.name,
          description: `Reservation for ${reservation.event?.name ?? 'event'}`,
          date: reservation.date,
          location: reservation.event?.location ?? 'N/A',
          submittedBy: reservation.name,
          eventId: reservation.event?.id
        }));
      },
      error: () => {
        this.submissions = [];
      }
    });

    // Load Stalls
    this.eventPlanningApi.stalls.list().subscribe({
      next: (stalls) => {
        this.stalls = stalls;
      },
      error: () => {
        this.stalls = [];
      }
    });

    // Load Equipments
    this.eventPlanningApi.equipment.list().subscribe({
      next: (equipments) => {
        this.equipments = equipments;
        // Auto-delete equipments whose event date has passed
        this.deleteExpiredEquipments();
      },
      error: () => {
        this.equipments = [];
      }
    });

    // Load Equipment Reservations - Get equipments and link to their events
    this.eventPlanningApi.equipment.list().subscribe({
      next: (equipments) => {
        this.equipmentReservations = equipments
          .filter((eq: any) => eq.event?.id) // Only show equipments linked to events
          .map((eq: any) => ({
            id: eq.id,
            eventId: eq.event?.id,
            eventName: eq.event?.name || 'N/A',
            equipmentKey: eq.key || eq.id,
            equipmentLabel: eq.name || 'Unknown',
            quantity: eq.quantity || 1,
            reservedAt: eq.createdAt || new Date().toISOString(),
            imageUrl: eq.imageUrl
          }));
      },
      error: () => {
        this.equipmentReservations = [];
      }
    });
  }

  approveSubmission(submissionId: number): void {
    const submission = this.submissions.find((item) => item.id === submissionId);
    if (!submission) {
      return;
    }

    const payload: ReservationWritePayload = {
      name: submission.title,
      date: submission.date,
      event: submission.eventId ? { id: submission.eventId } : undefined
    };

    this.eventPlanningApi.reservations.update(submissionId, payload).subscribe({
      next: () => {
        this.submissions = this.submissions.filter((item) => item.id !== submissionId);
      }
    });
  }

  rejectSubmission(submissionId: number): void {
    this.eventPlanningApi.reservations.delete(submissionId).subscribe({
      next: () => {
        this.submissions = this.submissions.filter((item) => item.id !== submissionId);
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  // CRUD methods
  openCreateModal(): void {
    this.isViewMode = false;
    this.editingEvent = null;
    this.modalTitle = 'Create New Event';
    this.eventForm.reset({
      name: '',
      date: '',
      description: '',
      location: '',
      online: false,
      nbTickets: 0
    });
    this.toggleLocationControl(false);
    this.eventForm.enable();
    this.recommendedEquipments = [];
    this.isShowingRecommendations = false;
    this.isModalOpen = true;
  }

  openEditModal(event: EventDashboardItem): void {
    this.isViewMode = false;
    this.editingEvent = { id: event.id, name: event.name, date: event.date, location: event.location };
    this.modalTitle = 'Edit Event';
    this.eventForm.patchValue({
      name: event.name,
      date: this.normalizeDate(event.date),
      description: event.description,
      location: event.location,
      online: event.online,
      nbTickets: 0 // Default to 0 for edit mode
    });
    this.eventForm.enable();
    this.toggleLocationControl(!!event.online);
    this.isShowingRecommendations = false;
    this.isModalOpen = true;
  }

  openViewModal(event: EventDashboardItem): void {
    this.isViewMode = true;
    this.editingEvent = { id: event.id, name: event.name, date: event.date, location: event.location };
    this.modalTitle = 'View Event';
    this.eventForm.patchValue({
      name: event.name,
      date: this.normalizeDate(event.date),
      description: event.description,
      location: event.location,
      online: event.online,
      nbTickets: 0
    });
    this.toggleLocationControl(!!event.online);
    this.eventForm.disable();
    this.isShowingRecommendations = false;
    this.isModalOpen = true;
  }

  closeModal(): void {
    this.isModalOpen = false;
    this.isViewMode = false;
    this.eventForm.reset();
    this.eventForm.enable();
    this.editingEvent = null;
    this.recommendedEquipments = [];
    this.isShowingRecommendations = false;
  }

  openReservationModal(event: EventDashboardItem): void {
    if (event.online) {
      return;
    }

    this.reservationEvent = event;
    this.reservationModalTitle = `Reservation - ${event.name}`;
    this.reservationForm.reset({
      name: '',
      date: this.normalizeDate(event.date)
    });
    
    // Load recommended equipments for this event
    this.eventPlanningApi.equipment.list().subscribe({
      next: (allEquipments) => {
        // Filter equipments that belong to this event and show their quantities
        this.selectedEquipments = allEquipments
          .filter((eq: any) => eq.event?.id === event.id)
          .map((eq: any) => ({
            id: eq.id,
            name: eq.name,
            quantity: eq.quantity || 1  // Use the actual recommended quantity
          }));
      },
      error: () => {
        this.selectedEquipments = [];
      }
    });
    
    this.isReservationModalOpen = true;
  }

  closeReservationModal(): void {
    this.isReservationModalOpen = false;
    this.reservationEvent = null;
    this.reservationForm.reset();
    this.selectedEquipments = [];
    this.isReservationSubmitting = false;
  }

  submitReservation(): void {
    if (this.reservationForm.invalid || !this.reservationEvent) {
      this.reservationForm.markAllAsTouched();
      return;
    }

    this.isReservationSubmitting = true;

    const formValue = this.reservationForm.value;
    
    // Create payload with equipments and quantities ✅
    const payload = {
      name: formValue.name,
      date: this.formatDateForBackend(formValue.date),
      eventId: this.reservationEvent.id,
      equipments: this.selectedEquipments.map((eq) => ({
        equipmentId: eq.id,
        quantity: eq.quantity,
        stallId: null
      }))
    };

    // Use new endpoint /with-equipment ✅
    this.eventPlanningApi.createReservationWithEquipment(payload).subscribe({
      next: () => {
        this.closeReservationModal();
        this.refreshData();
      },
      error: () => {
        this.isReservationSubmitting = false;
      }
    });
  }

  openStallModal(): void {
    this.editingStall = null;
    this.selectedStallBlock = 'A';
    this.stallModalTitle = 'Create New Stall';
    this.stallForm.reset({
      name: '',
      number: 1,
      location: 'A',
      eventId: ''
    });
    this.isStallModalOpen = true;
  }

  editStall(stall: any): void {
    this.editingStall = stall;
    this.selectedStallBlock = stall.location || 'A';
    this.stallModalTitle = 'Edit Stall';
    this.stallForm.patchValue({
      name: stall.name,
      number: stall.number,
      location: stall.location || 'A',
      eventId: stall.event?.id || ''
    });
    this.isStallModalOpen = true;
  }

  deleteStall(id: number): void {
    if (confirm('Are you sure you want to delete this stall?')) {
      this.eventPlanningApi.stalls.delete(id).subscribe({
        next: () => {
          this.stalls = this.stalls.filter(s => s.id !== id);
        },
        error: () => {
          alert('Failed to delete stall');
        }
      });
    }
  }

  closeStallModal(): void {
    this.isStallModalOpen = false;
    this.stallForm.reset({ name: '', number: 1, location: 'A', eventId: '' });
    this.isStallSubmitting = false;
    this.editingStall = null;
    this.selectedStallBlock = 'A';
  }

  backToReservationFromStall(): void {
    this.closeStallModal();
    if (this.reservationEvent) {
      this.isReservationModalOpen = true;
    }
  }

  submitStall(): void {
    if (this.stallForm.invalid) {
      this.stallForm.markAllAsTouched();
      console.log('Form validation errors:', this.stallForm.errors);
      console.log('Form control errors:', {
        name: this.stallForm.get('name')?.errors,
        number: this.stallForm.get('number')?.errors,
        location: this.stallForm.get('location')?.errors,
        eventId: this.stallForm.get('eventId')?.errors
      });
      return;
    }

    this.isStallSubmitting = true;
    const formValue = this.stallForm.value;

    // Validate eventId and location
    if (!formValue.eventId) {
      alert('Event is required');
      this.isStallSubmitting = false;
      return;
    }

    if (!formValue.location || !this.espritBlocks.includes(formValue.location)) {
      alert('Please select a valid block location');
      this.isStallSubmitting = false;
      return;
    }

    // Create payload with proper format for backend
    const payload = {
      name: formValue.name,
      number: parseInt(formValue.number, 10),  // Ensure it's a number
      location: formValue.location,  // Already in uppercase from the form
      eventId: parseInt(formValue.eventId, 10)  // Ensure it's a number
    };

    console.log('Submitting stall payload:', payload);

    const operation = this.editingStall
      ? this.eventPlanningApi.stalls.update(this.editingStall.id, payload)
      : this.eventPlanningApi.stalls.create(payload);

    operation.subscribe({
      next: () => {
        alert('Stall saved successfully');
        this.closeStallModal();
        this.refreshData();
      },
      error: (error) => {
        console.error('Stall submission error:', error);
        let errorMessage = 'Failed to save stall';
        
        // Try to get detailed error message from backend
        if (error?.error?.message) {
          errorMessage = error.error.message;
        } else if (error?.error?.detail) {
          errorMessage = error.error.detail;
        } else if (error?.error?.errors && Array.isArray(error.error.errors)) {
          errorMessage = error.error.errors.map((e: any) => e.message || e).join(', ');
        } else if (typeof error?.error === 'string') {
          errorMessage = error.error;
        }
        
        alert(`Error (${error?.status}): ${errorMessage}`);
        this.isStallSubmitting = false;
      }
    });
  }

  selectStallBlock(block: string): void {
    if (!this.espritBlocks.includes(block)) {
      return;
    }

    this.selectedStallBlock = block;
    this.stallForm.patchValue({ location: block });
  }

  private getDefaultBlockFromLocation(location: string | undefined): string {
    const normalized = String(location ?? '').trim().toUpperCase();
    const matched = this.espritBlocks.find((block) => normalized === block || normalized.includes(`BLOC ${block}`));
    return matched ?? 'A';
  }

  // Equipment CRUD methods
  openEquipmentModal(): void {
    this.equipmentModalTitle = this.editingEquipment ? 'Edit Equipment' : 'Create New Equipment';
    this.isEquipmentSubmitting = false;
    if (!this.editingEquipment) {
      this.equipmentForm.reset({
        name: '',
        type: '',
        status: 'AVAILABLE',
        quantity: 1,
        eventId: ''
      });
    }
    this.isEquipmentModalOpen = true;
  }

  editEquipment(equipment: any): void {
    this.editingEquipment = equipment;
    this.equipmentForm.patchValue({
      name: equipment.name,
      type: equipment.type,
      status: equipment.status || 'AVAILABLE',
      quantity: equipment.quantity || 1,
      eventId: equipment.event?.id || ''
    });
    this.openEquipmentModal();
  }

  deleteEquipment(id: number): void {
    if (confirm('Are you sure you want to delete this equipment?')) {
      this.eventPlanningApi.equipment.delete(id).subscribe({
        next: () => {
          this.equipments = this.equipments.filter(e => e.id !== id);
        },
        error: () => {
          alert('Failed to delete equipment');
        }
      });
    }
  }

  submitEquipment(): void {
    if (this.equipmentForm.invalid) {
      this.equipmentForm.markAllAsTouched();
      console.log('Form validation errors:', this.equipmentForm.errors);
      console.log('Form control errors:', {
        name: this.equipmentForm.get('name')?.errors,
        type: this.equipmentForm.get('type')?.errors,
        status: this.equipmentForm.get('status')?.errors,
        eventId: this.equipmentForm.get('eventId')?.errors
      });
      return;
    }

    this.isEquipmentSubmitting = true;
    const formValue = this.equipmentForm.value;

    // Validate eventId is present
    if (!formValue.eventId) {
      alert('Event is required');
      this.isEquipmentSubmitting = false;
      return;
    }

    // Create payload with proper format for backend
    const payload = {
      name: formValue.name,
      type: formValue.type,
      status: formValue.status,  // Already uppercase from select
      quantity: parseInt(formValue.quantity, 10),  // Ensure it's a number
      eventId: parseInt(formValue.eventId, 10)  // Ensure it's a number
    };

    console.log('Submitting equipment payload:', payload);

    const operation = this.editingEquipment
      ? this.eventPlanningApi.equipment.update(this.editingEquipment.id, payload)
      : this.eventPlanningApi.equipment.create(payload);

    operation.subscribe({
      next: () => {
        alert('Equipment saved successfully');
        this.closeEquipmentModal();
        this.refreshData();
      },
      error: (error) => {
        console.error('Equipment submission error:', error);
        const errorMessage = error?.error?.message || error?.error?.detail || 'Failed to save equipment';
        alert(`Error: ${errorMessage}`);
        this.isEquipmentSubmitting = false;
      }
    });
  }

  closeEquipmentModal(): void {
    this.isEquipmentModalOpen = false;
    this.equipmentForm.reset({ name: '', type: '', status: 'AVAILABLE', quantity: 1, eventId: '' });
    this.isEquipmentSubmitting = false;
    this.editingEquipment = null;
  }

  clearReservationSearch(): void {
    this.equipmentReservationSearchQuery = '';
  }

  private normalizeSearchValue(value: string): string {
    return String(value ?? '')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim();
  }

  submitEvent(): void {
    if (this.isViewMode) {
      this.closeModal();
      return;
    }

    if (this.eventForm.invalid) {
      return;
    }

    this.isSubmitting = true;
    const formValue = this.eventForm.value;
    
    // Convertir la date au format ISO 8601 (yyyy-MM-dd) pour le backend
    const dateValue = this.formatDateForBackend(formValue.date);
    
    const payload: EventWritePayload = {
      name: formValue.name,
      date: dateValue,
      location: formValue.location?.trim() || '',
      online: !!formValue.online,
      nbTickets: formValue.nbTickets || 0
    };

    console.log('Submitting event payload', payload);

    const operation = this.editingEvent
      ? this.eventPlanningApi.events.update(this.editingEvent.id, payload)
      : this.eventPlanningApi.events.create(payload);

    operation.subscribe({
      next: () => {
        this.closeModal();
        this.refreshData();
      },
      error: (error) => {
        this.isSubmitting = false;
        console.error('Create event failed:', error);

        const backendMessage = error?.error?.message || error?.error || error?.message;
        const status = error?.status ? ` (${error.status})` : '';
        alert(`La création de l’événement a échoué${status}. ${backendMessage ? backendMessage : 'Vérifie les informations et réessaye.'}`);
      },
      complete: () => {
        this.isSubmitting = false;
      }
    });
  }

  private formatDateForBackend(date: string | Date): string {
    if (!date) return '';
    
    const dateObj = typeof date === 'string' ? new Date(date) : date;
    const year = dateObj.getFullYear();
    const month = String(dateObj.getMonth() + 1).padStart(2, '0');
    const day = String(dateObj.getDate()).padStart(2, '0');
    
    return `${year}-${month}-${day}`;
  }

  private normalizeDate(date: string | Date): string {
    if (!date) return '';
    
    // Si c'est déjà en format ISO (yyyy-MM-dd), retourner tel quel
    if (typeof date === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(date)) {
      return date;
    }
    
    // Sinon, convertir au format ISO
    return this.formatDateForBackend(date);
  }

  /**
   * Recommends equipment based on the number of tickets for in-person events
   * Rules:
   * - Chairs = nbTickets * 1.0 (rounded up)
   * - Tables = nbTickets / 5 (integer division)
   * - Microphones = max(nbTickets / 20, 1) (minimum 1)
   * - Projectors = nbTickets <= 30 ? 1 : 2
   */
  recommendEquipments(): void {
    const nbTickets = this.eventForm.get('nbTickets')?.value || 0;
    const isOnline = this.eventForm.get('online')?.value;

    // Only recommend equipment for in-person events with tickets
    if (isOnline || nbTickets <= 0) {
      this.recommendedEquipments = [];
      this.isShowingRecommendations = false;
      return;
    }

    this.recommendedEquipments = [];

    // Chairs = nbTickets * 1.0 (rounded up)
    const chairsQuantity = Math.ceil(nbTickets * 1.0);
    this.recommendedEquipments.push({
      name: 'Chairs',
      type: 'Furniture',
      quantity: chairsQuantity,
      description: `${chairsQuantity} chairs recommended for seating`
    });

    // Tables = nbTickets / 5 (integer division)
    const tablesQuantity = Math.floor(nbTickets / 5);
    if (tablesQuantity > 0) {
      this.recommendedEquipments.push({
        name: 'Tables',
        type: 'Furniture',
        quantity: tablesQuantity,
        description: `${tablesQuantity} tables recommended`
      });
    }

    // Microphones = max(nbTickets / 20, 1)
    const microphonesQuantity = Math.max(Math.floor(nbTickets / 20), 1);
    this.recommendedEquipments.push({
      name: 'Microphone',
      type: 'Audio',
      quantity: microphonesQuantity,
      description: `${microphonesQuantity} microphone(s) for announcements`
    });

    // Projectors = 1 if nbTickets <= 30, else 2
    const projectorsQuantity = nbTickets <= 30 ? 1 : 2;
    this.recommendedEquipments.push({
      name: 'Projector',
      type: 'Visual',
      quantity: projectorsQuantity,
      description: `${projectorsQuantity} projector(s) for presentations`
    });

    this.isShowingRecommendations = true;
  }

  deleteEvent(eventId: number): void {
    if (confirm('Are you sure you want to delete this event?')) {
      this.eventPlanningApi.events.delete(eventId).subscribe({
        next: () => {
          this.refreshData();
        }
      });
    }
  }
}
