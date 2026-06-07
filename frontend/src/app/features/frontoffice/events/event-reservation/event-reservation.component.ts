import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HeaderComponent, FooterComponent } from '../../../../shared/layout';
import { EventPlanningApiService } from '../../../../services';
import { AuthService } from '../../../../services/auth.service';
import {
  EventResource,
  StallResource,
  EquipmentResource,
  ReservationResource,
  StallWritePayload,
  EquipmentWritePayload,
  EquipmentReservationWritePayload
} from '../../../../services/api/models/api-resource.model';
import { forkJoin, switchMap } from 'rxjs';

interface PurchasedTicket {
  id: number;
  quantity: number;
  type: string;
  price: number;
  totalPrice: number;
  purchasedAt: string;
}

@Component({
  selector: 'app-event-reservation',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule, HeaderComponent, FooterComponent],
  templateUrl: './event-reservation.component.html',
  styleUrls: []
})
export class EventReservationComponent implements OnInit {
  private readonly eventPlanningApi = inject(EventPlanningApiService);
  private readonly authService = inject(AuthService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);

  // Event and reservation data
  event: EventResource | null = null;
  stalls: StallResource[] = [];
  equipment: EquipmentResource[] = [];
  eventId: number | null = null;

  // Selected items for reservation
  selectedStalls: StallWritePayload[] = [];
  selectedEquipment: { equipmentName: string; quantity: number; usageDetails: string; name: string; email: string; phone: string }[] = [];

  // Forms
  stallReservationForm: FormGroup;
  equipmentReservationForm: FormGroup;

  // UI State
  loading = false;
  submitMessage: { type: 'success' | 'error'; text: string } | null = null;
  stallReservationSubmitting = false;
  equipmentReservationSubmitting = false;
  activeReservationTab: 'stall' | 'equipment' = 'stall';

  // Tickets
  ticketQuantity = 1;
  ticketType: 'VIP' | 'REGULAR' | 'STUDENT' = 'REGULAR';
  ticketPrice = 0;
  baseTicketPrice = 0;
  private promoOfferByDate: Map<string, { date: string; label: string; discountRate: number; discountLabel: string }> = new Map();
  activePromoOffer: { date: string; label: string; discountRate: number; discountLabel: string } | null = null;
  // Manual promo inputs
  manualPromoLabel = '';
  manualPromoDiscount = 0; // percent 0-100
  purchasedTickets: PurchasedTicket[] = [];
  private ticketIdCounter = 1;

  ticketTypes = ['VIP', 'REGULAR', 'STUDENT'];
  ticketPrices: { [key: string]: number } = {
    'VIP': 50,
    'REGULAR': 25,
    'STUDENT': 15
  };

  constructor() {
    this.stallReservationForm = this.formBuilder.group({
      stallNumber: ['', [Validators.required, Validators.min(1)]],
      stallSize: ['', Validators.required],
      stallDescription: ['', [Validators.required, Validators.minLength(5)]],
      name: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^\d{8,}$/)]]
    });

    this.equipmentReservationForm = this.formBuilder.group({
      equipmentName: ['', [Validators.required, Validators.minLength(2)]],
      quantity: [1, [Validators.required, Validators.min(1)]],
      usageDetails: ['', [Validators.required, Validators.minLength(5)]],
      name: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^\d{8,}$/)]]
    });

    this.ticketPrice = this.ticketPrices['REGULAR'];
    this.baseTicketPrice = this.ticketPrices['REGULAR'];
  }

  ngOnInit(): void {
    this.activatedRoute.params.subscribe((params) => {
      this.eventId = params['id'];
      if (this.eventId) {
        this.loadEventData();
      }
    });

    // Load promo offers for tickets (used by random promo selector)
    this.loadPromoOffers();
  }

  private normalizeDate(date: string): string {
    return date ? date.split('T')[0] : '';
  }

  private loadPromoOffers(): void {
    this.eventPlanningApi.getTicketPromoOffers().subscribe({
      next: (offers: any[]) => {
        const normalized = offers.map((offer) => ({
          date: this.normalizeDate(offer.date),
          label: offer.label,
          discountRate: offer.discountRate,
          discountLabel: offer.discountLabel
        }));

        this.promoOfferByDate = new Map(normalized.map((o) => [o.date, o]));
      },
      error: () => {
        this.promoOfferByDate = new Map();
      }
    });
  }

  private recalculateTicketPrice(): void {
    const discountRate = this.activePromoOffer?.discountRate ?? 0;
    const discounted = this.baseTicketPrice * (1 - discountRate);
    this.ticketPrice = Math.round(discounted * 100) / 100;
  }

  applyManualPromo(): void {
    const pct = Number(this.manualPromoDiscount);
    if (isNaN(pct) || pct < 0 || pct > 100) {
      this.showMessage('error', 'Please enter a valid percentage (0–100).');
      return;
    }

    const label = this.manualPromoLabel?.trim() || `${pct}%`;
    this.activePromoOffer = {
      date: this.event ? this.normalizeDate(this.event.date) : this.normalizeDate(new Date().toISOString()),
      label,
      discountRate: pct / 100,
      discountLabel: label
    };

    this.baseTicketPrice = this.ticketPrices[this.ticketType];
    this.recalculateTicketPrice();
    this.showMessage('success', `Promo applied: ${this.activePromoOffer.discountLabel} (${pct}% off)`);
  }

  clearManualPromo(): void {
    this.activePromoOffer = null;
    this.manualPromoLabel = '';
    this.manualPromoDiscount = 0;
    this.baseTicketPrice = this.ticketPrices[this.ticketType];
    this.recalculateTicketPrice();
    this.showMessage('success', 'Promo removed');
  }

  chooseRandomPromo(): void {
    const offers = Array.from(this.promoOfferByDate.values());
    if (offers.length === 0) {
      this.showMessage('error', 'No promo offers available');
      return;
    }

    // Prefer offers that match this event date
    const eventDateNorm = this.event ? this.normalizeDate(this.event.date) : '';
    const matching = offers.filter(o => o.date === eventDateNorm);
    const pool = matching.length > 0 ? matching : offers;

    const chosen = pool[Math.floor(Math.random() * pool.length)];
    this.activePromoOffer = chosen;
    // apply to current ticket type
    this.baseTicketPrice = this.ticketPrices[this.ticketType];
    this.recalculateTicketPrice();
    this.showMessage('success', `Promo applied: ${chosen.discountLabel} (${Math.round(chosen.discountRate * 100)}% OFF)`);
  }

  private loadEventData(): void {
    this.loading = true;

    // Load event details
    this.eventPlanningApi.events.getById(this.eventId!).subscribe({
      next: (event: EventResource) => {
        this.event = event;
        this.loadStallsAndEquipment();
      },
      error: () => {
        this.showMessage('error', 'Failed to load event details');
        this.loading = false;
      }
    });
  }

  private loadStallsAndEquipment(): void {
    // Load stalls and equipment in parallel using forkJoin
    forkJoin([
      this.eventPlanningApi.stalls.list(),
      this.eventPlanningApi.equipment.list()
    ]).subscribe({
      next: ([stalls, equipmentList]) => {
        this.stalls = stalls.filter(stall => stall.event?.id === this.eventId);
        this.equipment = equipmentList.filter(eq => eq.event?.id === this.eventId);
        this.loading = false;
      },
      error: () => {
        console.error('Failed to load stalls and equipment');
        this.stalls = [];
        this.equipment = [];
        this.loading = false;
      }
    });
  }

  selectReservationTab(tab: 'stall' | 'equipment'): void {
    this.activeReservationTab = tab;
    this.submitMessage = null;
  }

  addStall(): void {
    if (this.stallReservationForm.invalid) {
      this.showMessage('error', 'Please fill in all required fields correctly');
      return;
    }

    const formValue = this.stallReservationForm.value;
    const user = this.authService.currentUser();

    const stallPayload: StallWritePayload = {
      name: `${formValue.stallDescription} (${formValue.stallSize})`,
      number: formValue.stallNumber,
      location: 'A',
      event: { id: this.eventId! }
    };

    if (user) {
      stallPayload.user = { id: user.id };
    }

    this.selectedStalls.push(stallPayload);
    this.stallReservationForm.reset({ stallNumber: '', stallSize: '', stallDescription: '', name: '', email: '', phone: '' });
    this.showMessage('success', 'Stall added to reservation');
  }

  removeStall(index: number): void {
    this.selectedStalls.splice(index, 1);
  }

  addEquipment(): void {
    if (this.equipmentReservationForm.invalid) {
      this.showMessage('error', 'Please fill in all required fields correctly');
      return;
    }

    const formValue = this.equipmentReservationForm.value;
    this.selectedEquipment.push({
      equipmentName: formValue.equipmentName,
      quantity: formValue.quantity,
      usageDetails: formValue.usageDetails,
      name: formValue.name,
      email: formValue.email,
      phone: formValue.phone
    });
    this.equipmentReservationForm.reset({ equipmentName: '', quantity: 1, usageDetails: '', name: '', email: '', phone: '' });
    this.showMessage('success', 'Equipment added to reservation');
  }

  removeEquipment(index: number): void {
    this.selectedEquipment.splice(index, 1);
  }

  submitAllReservations(): void {
    if (this.selectedStalls.length === 0 && this.selectedEquipment.length === 0) {
      this.showMessage('error', 'Please add at least one stall or equipment to the reservation');
      return;
    }

    this.stallReservationSubmitting = true;
    const user = this.authService.currentUser();

    const reservation: ReservationResource = {
      id: 0,
      name: `Reservation for ${this.event?.name} by ${user?.name || 'User'}`,
      date: new Date().toISOString().split('T')[0],
      event: { id: this.eventId! } as EventResource
    };

    this.eventPlanningApi.reservations.create(reservation).subscribe({
      next: (createdReservation) => {
        const observables = [];

        // Create stalls
        for (const stall of this.selectedStalls) {
          observables.push(this.eventPlanningApi.stalls.create(stall));
        }

        // Create equipment and their reservations
        for (const eq of this.selectedEquipment) {
          const equipmentPayload: EquipmentWritePayload = {
            name: eq.equipmentName,
            type: eq.usageDetails,
            status: 'BOOKED',
            event: { id: this.eventId! }
          };

          const equipmentObs = this.eventPlanningApi.equipment.create(equipmentPayload);
          const equipmentReservationObs = equipmentObs.pipe(
            switchMap(equipment =>
              this.eventPlanningApi.equipmentReservations.create({
                quantity: eq.quantity,
                equipment: { id: equipment.id }
              })
            )
          );
          observables.push(equipmentReservationObs);
        }

        if (observables.length > 0) {
          forkJoin(observables).subscribe({
            next: () => {
              this.showMessage('success', 'Reservation submitted successfully!');
              this.selectedStalls = [];
              this.selectedEquipment = [];
              this.stallReservationSubmitting = false;
              this.loadStallsAndEquipment();
            },
            error: () => {
              this.showMessage('error', 'Failed to save some items in the reservation.');
              this.stallReservationSubmitting = false;
            }
          });
        } else {
          this.showMessage('success', 'Reservation submitted successfully!');
          this.selectedStalls = [];
          this.selectedEquipment = [];
          this.stallReservationSubmitting = false;
        }
      },
      error: () => {
        this.showMessage('error', 'Failed to create reservation.');
        this.stallReservationSubmitting = false;
      }
    });
  }

  private showMessage(type: 'success' | 'error', text: string): void {
    this.submitMessage = { type, text };
    // Auto-clear message after 5 seconds
    setTimeout(() => {
      this.submitMessage = null;
    }, 5000);
  }

  get totalTicketPrice(): number {
    return this.ticketPrice * this.ticketQuantity;
  }

  onTicketTypeChange(type: string): void {
    this.ticketType = type as 'VIP' | 'REGULAR' | 'STUDENT';
    this.baseTicketPrice = this.ticketPrices[type];
    this.recalculateTicketPrice();
  }

  buyTickets(): void {
    if (!this.eventId || this.ticketQuantity <= 0) {
      this.showMessage('error', 'Please specify a valid quantity of tickets');
      return;
    }

    this.purchasedTickets.unshift({
      id: this.ticketIdCounter++,
      quantity: this.ticketQuantity,
      type: this.ticketType,
      price: this.ticketPrice,
      totalPrice: this.totalTicketPrice,
      purchasedAt: new Date().toISOString()
    });

    this.showMessage('success', `${this.ticketQuantity} ticket(s) (${this.ticketType}) purchased successfully!`);
    this.ticketQuantity = 1;
    this.ticketType = 'REGULAR';
    this.ticketPrice = this.ticketPrices['REGULAR'];
  }
}
