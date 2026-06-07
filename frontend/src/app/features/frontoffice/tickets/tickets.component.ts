import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import { EventPlanningApiService, EventResource } from '../../../services';
import { TicketPromoOfferResource, TicketPromoSelectionWritePayload } from '../../../services/api/models/api-resource.model';

interface PurchasedTicket {
  id: number;
  eventId: number;
  eventName: string;
  eventDate: string;
  quantity: number;
  type: string;
  price: number;
  totalPrice: number;
  purchasedAt: string;
}

interface EventTicket {
  id: number;
  name: string;
  date: string;
  location: string;
  online: boolean;
  nbTickets: number;
  price?: number;
  promoOffer?: TicketPromoOffer | null;
}

interface TicketPromoOffer {
  date: string;
  label: string;
  discountRate: number;
  discountLabel: string;
}

interface TicketTypeStat {
  type: 'VIP' | 'REGULAR' | 'STUDENT';
  quantity: number;
  revenue: number;
}

interface Seat {
  id: string; // e.g. 'A-1'
  row: string;
  col: number;
  status: 'free' | 'reserved' | 'sold';
  reservedBy?: string | null;
  reservationId?: string | null;
  reservationExpiresAt?: string | null; // ISO
}

@Component({
  selector: 'app-tickets',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, HeaderComponent, FooterComponent],
  templateUrl: './tickets.component.html',
  styleUrls: ['./tickets.component.css']
})
export class TicketsComponent {
  private eventPlanningApi = inject(EventPlanningApiService);
  private fb = inject(FormBuilder);

  events: EventTicket[] = [];
  private promoOfferByDate = new Map<string, TicketPromoOffer>();
  private activePromoEventId: number | null = null;
  activePromoOffer: TicketPromoOffer | null = null;
  promoDiscountPercentByEventId: Record<number, number> = {};
  promoSelectionSubmittingEventId: number | null = null;
  purchasedTickets: PurchasedTicket[] = [];
  ticketSelectedEventId: number | null = null;
  ticketQuantity = 1;
  ticketType: 'VIP' | 'REGULAR' | 'STUDENT' = 'REGULAR';
  baseTicketPrice = 0;
  ticketPrice = 0;
  private ticketIdCounter = 1;
  searchQuery = '';
  showLowStockToast = true;
  promoCalendarImageUrl = 'https://timesles.com/calendars-pdf/tunisia-212/years/calendar-yearly-2026-P-fr-tunisia-212.jpg';
  promoCalendarUrl = 'https://calculatorian.com/fr/time-and-date/year-calendar/tn/all/2026';

  ticketTypes = ['VIP', 'REGULAR', 'STUDENT'];
  ticketPrices: { [key: string]: number } = {
    'VIP': 50,
    'REGULAR': 25,
    'STUDENT': 15
  };

  ticketForm: FormGroup = this.fb.group({
    quantity: [1, [Validators.required, Validators.min(1), Validators.max(10)]]
  });

  // Seating map
  seatRows: string[] = ['A','B','C','D','E','F','G','H','I','J'];
  seatCols = 14;
  seats: Seat[] = [];
  selectedSeats: Seat[] = [];
  // reservation timers per seat id (seconds remaining)
  reservationRemaining: Record<string, number> = {};
  reservationIntervalHandles: Record<string, any> = {};
  reservationHolderId = `user-${Math.floor(Math.random()*1000000)}`; // lightweight holder id for POC

  get seatColumns(): number[] { return Array.from({ length: this.seatCols }, (_, i) => i + 1); }


  constructor() {
    this.baseTicketPrice = this.ticketPrices['REGULAR'];
    this.ticketPrice = this.baseTicketPrice;
    this.loadPromoOffers();
    this.loadEvents();
    // listen to local seat updates from other tabs (POC)
    window.addEventListener('seats-updated', () => this.loadSeatsForSelectedEvent());
    window.addEventListener('storage', (e) => {
      if ((e as StorageEvent).key?.startsWith('seats-event-')) {
        this.loadSeatsForSelectedEvent();
      }
    });
  }

  private loadPromoOffers(): void {
    this.eventPlanningApi.getTicketPromoOffers().subscribe({
      next: (offers: TicketPromoOfferResource[]) => {
        const normalizedOffers = offers.map((offer) => ({
          date: this.normalizeDate(offer.date),
          label: offer.label,
          discountRate: offer.discountRate,
          discountLabel: offer.discountLabel
        }));

        this.promoOfferByDate = new Map(normalizedOffers.map((offer) => [offer.date, offer]));
        this.recalculateTicketPrice();
        this.decorateEventsWithPromos();
      },
      error: () => {
        this.promoOfferByDate = new Map();
        this.decorateEventsWithPromos();
      }
    });
  }

  private loadEvents(): void {
    this.eventPlanningApi.getEventsWithParticipants().subscribe({
      next: (events) => {
        this.events = events
          .filter((event) => !(event as any).online)
          .map((event) => ({
            id: event.id,
            name: event.name,
            date: event.date,
            location: event.location || 'N/A',
            online: (event as any).online || false,
            nbTickets: event.nbTickets ?? 0,
            promoOffer: null
          }));

        this.decorateEventsWithPromos();
      },
      error: () => {
        this.events = [];
      }
    });
  }

  private decorateEventsWithPromos(): void {
    this.events = this.events.map((event) => ({
      ...event,
      promoOffer: this.promoOfferByDate.get(this.normalizeDate(event.date)) ?? null
    }));

    this.ensurePromoDefaults();
  }

  private ensurePromoDefaults(): void {
    for (const event of this.events) {
      if (this.promoDiscountPercentByEventId[event.id] === undefined) {
        this.promoDiscountPercentByEventId[event.id] = Math.round((event.promoOffer?.discountRate ?? 0) * 100);
      }
    }
  }

  private normalizeDate(date: string): string {
    return date ? date.split('T')[0] : '';
  }

  isPromoEvent(event: EventTicket): boolean {
    return Boolean(event.promoOffer);
  }

  getPromoOfferForEvent(event: EventTicket): TicketPromoOffer | null {
    return event.promoOffer ?? this.promoOfferByDate.get(this.normalizeDate(event.date)) ?? null;
  }

  get activeDiscountRate(): number {
    return this.activePromoOffer?.discountRate ?? 0;
  }

  get currentBasePrice(): number {
    return this.baseTicketPrice;
  }

  get currentDiscountedPrice(): number {
    return this.ticketPrice;
  }

  getPromoPercent(event: EventTicket): number {
    const currentPercent = this.promoDiscountPercentByEventId[event.id];
    if (currentPercent !== undefined) {
      return this.clampPromoPercent(currentPercent);
    }

    const defaultPercent = Math.round((this.getPromoOfferForEvent(event)?.discountRate ?? 0) * 100);
    this.promoDiscountPercentByEventId[event.id] = defaultPercent;
    return defaultPercent;
  }

  setPromoPercent(eventId: number, value: number | string): void {
    this.promoDiscountPercentByEventId[eventId] = this.clampPromoPercent(Number(value));
  }

  getPromoButtonLabel(event: EventTicket): string {
    return `Promo -${this.getPromoPercent(event)}%`;
  }

  private clampPromoPercent(value: number): number {
    if (Number.isNaN(value)) {
      return 0;
    }

    return Math.min(100, Math.max(0, Math.round(value)));
  }

  private recalculateTicketPrice(): void {
    const discountRate = this.activeDiscountRate;
    const discountedPrice = this.baseTicketPrice * (1 - discountRate);
    this.ticketPrice = Math.round(discountedPrice * 100) / 100;
  }

  private clearPromo(): void {
    this.activePromoEventId = null;
    this.activePromoOffer = null;
    this.promoSelectionSubmittingEventId = null;
    this.recalculateTicketPrice();
  }

  applyPromoForEvent(event: EventTicket): void {
    const offer = this.getPromoOfferForEvent(event);
    if (!offer) {
      return;
    }

    // Afficher l'interface d'achat immédiatement
    this.ticketSelectedEventId = event.id;
    this.ticketQuantity = this.getEventRemainingTickets(event.id) > 0 ? 1 : 0;
    this.activePromoEventId = event.id;

    const discountPercent = this.getPromoPercent(event);
    const discountRate = discountPercent / 100;
    const discountLabel = `Promo -${discountPercent}%`;
    
    this.activePromoOffer = {
      ...offer,
      discountRate,
      discountLabel
    };
    this.recalculateTicketPrice();

    const payload: TicketPromoSelectionWritePayload = {
      eventId: event.id,
      discountPercent,
      discountLabel
    };

    this.promoSelectionSubmittingEventId = event.id;
    this.eventPlanningApi.submitTicketPromoSelection(payload).subscribe({
      next: () => {
        // Interface d'achat déjà affichée
      },
      error: () => {
        alert('Impossible d\'appliquer la promo pour le moment.');
      },
      complete: () => {
        this.promoSelectionSubmittingEventId = null;
      }
    });
  }

  scrollToPromo(): void {
    document.getElementById('promo')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  get filteredEvents(): EventTicket[] {
    if (!this.searchQuery.trim()) {
      return this.events;
    }

    const query = this.searchQuery.toLowerCase();
    return this.events.filter(
      (event) =>
        event.name.toLowerCase().includes(query) ||
        event.location.toLowerCase().includes(query) ||
        event.date.includes(query)
    );
  }

  get lowStockEvents(): EventTicket[] {
    return this.events
      .filter((event) => {
        const remaining = this.getEventRemainingTickets(event.id);
        return remaining > 0 && remaining <= 10;
      })
      .sort((a, b) => this.getEventRemainingTickets(a.id) - this.getEventRemainingTickets(b.id));
  }

  get hasLowStockAlert(): boolean {
    return this.showLowStockToast && this.lowStockEvents.length > 0;
  }

  dismissLowStockAlert(): void {
    this.showLowStockToast = false;
  }

  get selectedEventName(): string {
    const event = this.events.find(e => e.id === this.ticketSelectedEventId);
    return event ? event.name : 'Select an event';
  }

  get selectedEvent(): EventTicket | null {
    if (!this.ticketSelectedEventId) {
      return null;
    }
    return this.events.find(e => e.id === this.ticketSelectedEventId) ?? null;
  }

  get selectedEventMaxTickets(): number {
    return this.selectedEvent?.nbTickets ?? 0;
  }

  get selectedEventSoldTickets(): number {
    if (!this.ticketSelectedEventId) {
      return 0;
    }
    return this.getEventSoldTickets(this.ticketSelectedEventId);
  }

  get selectedEventRemainingTickets(): number {
    return Math.max(0, this.selectedEventMaxTickets - this.selectedEventSoldTickets);
  }

  get isSelectedEventSoldOut(): boolean {
    return this.ticketSelectedEventId !== null && this.selectedEventRemainingTickets <= 0;
  }

  get selectedEventTypeStats(): TicketTypeStat[] {
    if (!this.ticketSelectedEventId) {
      return [];
    }

    const base: TicketTypeStat[] = [
      { type: 'VIP', quantity: 0, revenue: 0 },
      { type: 'REGULAR', quantity: 0, revenue: 0 },
      { type: 'STUDENT', quantity: 0, revenue: 0 }
    ];

    for (const ticket of this.purchasedTickets.filter(t => t.eventId === this.ticketSelectedEventId)) {
      const stat = base.find(s => s.type === ticket.type as 'VIP' | 'REGULAR' | 'STUDENT');
      if (stat) {
        stat.quantity += ticket.quantity;
        stat.revenue += ticket.totalPrice;
      }
    }

    return base;
  }

  get selectedEventTypeStatsTotal(): number {
    return this.selectedEventTypeStats.reduce((sum, stat) => sum + stat.quantity, 0);
  }

  get selectedEventPieBackground(): string {
    const total = this.selectedEventTypeStatsTotal;
    if (total <= 0) {
      return 'conic-gradient(#e5e7eb 0% 100%)';
    }

    const vipPct = this.getTicketTypePercentage('VIP');
    const regularPct = this.getTicketTypePercentage('REGULAR');
    const vipEnd = vipPct;
    const regularEnd = vipPct + regularPct;

    return `conic-gradient(#d88cbc 0% ${vipEnd}%, #eee79a ${vipEnd}% ${regularEnd}%, #a8e0e0 ${regularEnd}% 100%)`;
  }

  get purchasedTypeStats(): TicketTypeStat[] {
    const base: TicketTypeStat[] = [
      { type: 'VIP', quantity: 0, revenue: 0 },
      { type: 'REGULAR', quantity: 0, revenue: 0 },
      { type: 'STUDENT', quantity: 0, revenue: 0 }
    ];

    for (const ticket of this.purchasedTickets) {
      const stat = base.find(s => s.type === ticket.type as 'VIP' | 'REGULAR' | 'STUDENT');
      if (stat) {
        stat.quantity += ticket.quantity;
        stat.revenue += ticket.totalPrice;
      }
    }

    return base;
  }

  get purchasedTypeStatsTotal(): number {
    return this.purchasedTypeStats.reduce((sum, stat) => sum + stat.quantity, 0);
  }

  get purchasedPieBackground(): string {
    const total = this.purchasedTypeStatsTotal;
    if (total <= 0) {
      return 'conic-gradient(#e5e7eb 0% 100%)';
    }

    const vipPct = this.getPurchasedTypePercentage('VIP');
    const regularPct = this.getPurchasedTypePercentage('REGULAR');
    const vipEnd = vipPct;
    const regularEnd = vipPct + regularPct;

    return `conic-gradient(#d88cbc 0% ${vipEnd}%, #eee79a ${vipEnd}% ${regularEnd}%, #a8e0e0 ${regularEnd}% 100%)`;
  }

  /* -------------------- Seat map / reservation helpers -------------------- */
  private buildDefaultSeats(): void {
    const seats: Seat[] = [];
    for (const row of this.seatRows) {
      for (let c = 1; c <= this.seatCols; c++) {
        seats.push({ id: `${row}-${c}`, row, col: c, status: 'free', reservedBy: null, reservationId: null, reservationExpiresAt: null });
      }
    }
    this.seats = seats;
  }

  loadSeatsForSelectedEvent(): void {
    if (!this.ticketSelectedEventId) {
      this.seats = [];
      return;
    }

    // Try API first, fallback to default grid stored in localStorage (POC)
    this.eventPlanningApi.getSeats(this.ticketSelectedEventId).subscribe({
      next: (payload) => {
        // Expect payload as array of seats { id, row, col, status, reservedBy, reservationId, reservationExpiresAt }
        if (Array.isArray(payload) && payload.length > 0) {
          this.seats = payload.map((s: any) => ({ ...s }));
        } else {
          this.buildDefaultSeats();
        }
        // Remove any expired reservations loaded from the API
        this.cleanExpiredReservations();
      },
      error: () => {
        // fallback: load from localStorage or build default
        const key = `seats-event-${this.ticketSelectedEventId}`;
        const raw = localStorage.getItem(key);
        if (raw) {
          try {
            this.seats = JSON.parse(raw) as Seat[];
          } catch {
            this.buildDefaultSeats();
          }
        } else {
          this.buildDefaultSeats();
        }
        // Remove expired reservations loaded from localStorage
        this.cleanExpiredReservations();
      }
    });
  }

  private persistSeatsLocal(): void {
    if (!this.ticketSelectedEventId) return;
    const key = `seats-event-${this.ticketSelectedEventId}`;
    try { localStorage.setItem(key, JSON.stringify(this.seats)); } catch {}
  }

  private isReservationActive(seat: Seat): boolean {
    if (!seat || !seat.reservationExpiresAt) return false;
    try {
      const expires = new Date(seat.reservationExpiresAt).getTime();
      return Date.now() < expires;
    } catch {
      return false;
    }
  }

  private cleanExpiredReservations(): void {
    let changed = false;
    for (const seat of this.seats) {
      if (seat.status === 'reserved') {
        if (!this.isReservationActive(seat)) {
          seat.status = 'free';
          seat.reservedBy = null;
          seat.reservationId = null;
          seat.reservationExpiresAt = null;
          changed = true;
        }
      }
    }
    if (changed) {
      this.persistSeatsLocal();
      try { window.dispatchEvent(new CustomEvent('seats-updated')); } catch {}
    }
  }

  getSeatByPosition(row: string, col: number): Seat | null {
    const seatId = `${row}-${col}`;
    let seat = this.seats.find((s) => s.id === seatId) ?? null;

    if (!seat && this.ticketSelectedEventId !== null) {
      this.buildDefaultSeats();
      seat = this.seats.find((s) => s.id === seatId) ?? null;
    }

    return seat;
  }

  getSeatClass(seat: Seat | null | undefined): string {
    if (!seat) return 'seat-free';
    if (seat.status === 'sold') return 'seat-sold';
    if (this.selectedSeats.find(s => s.id === seat.id)) return 'seat-selected';
    // if reserved but expired, treat as free
    if (seat.status === 'reserved' && !this.isReservationActive(seat)) return 'seat-free';
    if (seat.status === 'reserved') return 'seat-reserved';
    return 'seat-free';
  }

  async selectSeat(seat: Seat | null | undefined): Promise<void> {
    if (!seat) return;
    if (!this.ticketSelectedEventId) return;
    if (seat.status === 'sold') return;

    const alreadySelected = this.selectedSeats.find(s => s.id === seat.id);
    if (alreadySelected) {
      // release
      await this.releaseSeatRemoteOrLocal(seat);
      this.selectedSeats = this.selectedSeats.filter(s => s.id !== seat.id);
      this.clearReservationTimer(seat.id);
      this.ticketQuantity = this.selectedSeats.length > 0 ? this.selectedSeats.length : 1;
      return;
    }

    if (seat.status === 'reserved') {
      alert('Ce siège est temporairement réservé par un autre utilisateur.');
      return;
    }

    // reserve
    const reserveRes = await this.reserveSeatRemoteOrLocal(seat);
    if (reserveRes) {
      this.selectedSeats.push(seat);
      // set ticket quantity to selected seats count
      this.ticketQuantity = this.selectedSeats.length;
      this.startReservationCountdown(seat.id, reserveRes.expiresInSec ?? 600);
    } else {
      alert('Impossible de réserver ce siège pour le moment.');
    }
  }

  private async reserveSeatRemoteOrLocal(seat: Seat): Promise<{ reservationId?: string; expiresInSec?: number } | null> {
    const eventId = this.ticketSelectedEventId!;
    const expiresIn = 600; // 10 minutes
    try {
      const res = await this.eventPlanningApi.reserveSeat(eventId, seat.id, { holderId: this.reservationHolderId, expiresInSec: expiresIn }).toPromise();
      // assume res contains reservationId and expiresAt
      seat.status = 'reserved';
      seat.reservedBy = this.reservationHolderId;
      seat.reservationId = res?.reservationId ?? `${this.reservationHolderId}-${Date.now()}`;
      seat.reservationExpiresAt = res?.reservationExpiresAt ?? new Date(Date.now() + expiresIn * 1000).toISOString();
      this.persistSeatsLocal();
      return { reservationId: seat.reservationId ?? undefined, expiresInSec: expiresIn };
    } catch {
      // fallback to localStorage based reservation (POC)
      try {
        seat.status = 'reserved';
        seat.reservedBy = this.reservationHolderId;
        seat.reservationId = `${this.reservationHolderId}-${Date.now()}`;
        seat.reservationExpiresAt = new Date(Date.now() + expiresIn * 1000).toISOString();
        this.persistSeatsLocal();
        // notify other tabs
        window.dispatchEvent(new CustomEvent('seats-updated'));
        return { reservationId: seat.reservationId ?? undefined, expiresInSec: expiresIn };
      } catch {
        return null;
      }
    }
  }

  private async releaseSeatRemoteOrLocal(seat: Seat): Promise<void> {
    const eventId = this.ticketSelectedEventId!;
    try {
      await this.eventPlanningApi.releaseSeat(eventId, seat.id, { reservationId: seat.reservationId ?? undefined }).toPromise();
    } catch {
      // ignore
    }
    seat.status = 'free';
    seat.reservedBy = null;
    seat.reservationId = null;
    seat.reservationExpiresAt = null;
    this.persistSeatsLocal();
    window.dispatchEvent(new CustomEvent('seats-updated'));
  }

  private startReservationCountdown(seatId: string, seconds: number): void {
    this.reservationRemaining[seatId] = seconds;
    this.clearReservationTimer(seatId);
    this.reservationIntervalHandles[seatId] = setInterval(() => {
      this.reservationRemaining[seatId] = Math.max(0, this.reservationRemaining[seatId] - 1);
      if (this.reservationRemaining[seatId] <= 0) {
        // timeout: release seat
        const s = this.seats.find(x => x.id === seatId);
        if (s) {
          this.releaseSeatRemoteOrLocal(s);
        }
        this.clearReservationTimer(seatId);
        this.selectedSeats = this.selectedSeats.filter(x => x.id !== seatId);
      }
    }, 1000);
  }

  private clearReservationTimer(seatId: string): void {
    const h = this.reservationIntervalHandles[seatId];
    if (h) {
      clearInterval(h);
      delete this.reservationIntervalHandles[seatId];
      delete this.reservationRemaining[seatId];
    }
  }

  async confirmSelectedSeats(): Promise<void> {
    if (!this.ticketSelectedEventId) return;
    if (this.selectedSeats.length === 0) {
      alert('Aucun siège sélectionné.');
      return;
    }

    const eventId = this.ticketSelectedEventId;
    try {
      // confirm remotely where possible, fallback: mark sold locally
      for (const seat of [...this.selectedSeats]) {
        try {
          await this.eventPlanningApi.confirmSeat(eventId, seat.id, { reservationId: seat.reservationId ?? '' }).toPromise();
          seat.status = 'sold';
          this.clearReservationTimer(seat.id);
        } catch {
          // fallback local
          seat.status = 'sold';
          this.clearReservationTimer(seat.id);
        }
      }
      this.persistSeatsLocal();
      // set ticket quantity to selected seats length and proceed to purchase flow
      this.ticketQuantity = this.selectedSeats.length;
      await this.buyTickets();
      // clear selected seats
      this.selectedSeats = [];
    } catch (err) {
      console.error(err);
      alert('Impossible de confirmer les sièges. Réessayez.');
    }
  }

  clearSelectedSeats(): void {
    for (const seat of [...this.selectedSeats]) {
      this.clearReservationTimer(seat.id);
      seat.status = 'free';
      seat.reservedBy = null;
      seat.reservationId = null;
      seat.reservationExpiresAt = null;
    }

    this.selectedSeats = [];
    this.ticketQuantity = 1;
    this.persistSeatsLocal();
  }

  get bookingQuantity(): number {
    return this.selectedSeats.length > 0 ? this.selectedSeats.length : this.ticketQuantity;
  }

  getReservationCountdownLabel(): string {
    if (this.selectedSeats.length === 0) return '';
    // show minimum remaining among selected seats
    const rem = Math.min(...this.selectedSeats.map(s => this.reservationRemaining[s.id] ?? 600));
    const mm = Math.floor(rem / 60).toString().padStart(2, '0');
    const ss = (rem % 60).toString().padStart(2, '0');
    return `${mm}:${ss}`;
  }


  get totalTicketPrice(): number {
    return this.ticketPrice * this.bookingQuantity;
  }

  selectEventForTicket(eventId: number): void {
    const wasSelected = this.ticketSelectedEventId === eventId;
    this.ticketSelectedEventId = wasSelected ? null : eventId;

    if (wasSelected) {
      this.clearPromo();
    } else if (this.activePromoEventId !== eventId) {
      this.clearPromo();
    }

    if (this.ticketSelectedEventId !== null) {
      const remaining = this.selectedEventRemainingTickets;
      this.ticketQuantity = remaining > 0 ? 1 : 0;
      this.recalculateTicketPrice();
      // load seat map for this event
      this.loadSeatsForSelectedEvent();
    }
    if (this.ticketSelectedEventId === null) {
      // cleanup selected seats and timers
      for (const s of this.selectedSeats) {
        this.clearReservationTimer(s.id);
      }
      this.selectedSeats = [];
    }
  }

  buyTickets(): void {
    const quantityToBuy = this.bookingQuantity;

    if (!this.ticketSelectedEventId || quantityToBuy <= 0) {
      alert('Veuillez sélectionner un événement et spécifier une quantité.');
      return;
    }

    const selectedEvent = this.events.find((e) => e.id === this.ticketSelectedEventId);
    if (!selectedEvent) {
      return;
    }

    const remainingTickets = this.getEventRemainingTickets(selectedEvent.id);
    if (remainingTickets <= 0) {
      alert(`Billets épuisés pour ${selectedEvent.name}.`);
      return;
    }

    if (quantityToBuy > remainingTickets) {
      alert(`Maximum disponible: ${remainingTickets} billet(s) pour ${selectedEvent.name}.`);
      if (this.selectedSeats.length === 0) {
        this.ticketQuantity = remainingTickets;
      }
      return;
    }

    const purchasedQuantity = quantityToBuy;
    const purchasedType = this.ticketType;
    const purchasedPrice = this.ticketPrice;
    const originalPrice = this.baseTicketPrice;
    const discountRate = this.activeDiscountRate;
    const discountLabel = this.activePromoOffer?.discountLabel ?? null;
    const purchasedTotal = this.totalTicketPrice;

    const purchaseRequests = Array.from({ length: purchasedQuantity }, () =>
      this.eventPlanningApi.tickets.create({
        type: purchasedType,
        price: purchasedPrice,
        originalPrice,
        discountApplied: discountRate > 0,
        discountRate,
        discountLabel: discountLabel ?? undefined,
        eventId: selectedEvent.id
      })
    );

    forkJoin(purchaseRequests).subscribe({
      next: (createdTickets) => {
        for (let index = 0; index < createdTickets.length; index++) {
          this.purchasedTickets.unshift({
            id: this.ticketIdCounter++,
            eventId: selectedEvent.id,
            eventName: selectedEvent.name,
            eventDate: selectedEvent.date,
            quantity: 1,
            type: purchasedType,
            price: purchasedPrice,
            totalPrice: purchasedPrice,
            purchasedAt: new Date().toISOString()
          });
        }

        const newRemaining = this.getEventRemainingTickets(selectedEvent.id);

        // Reset
        this.ticketSelectedEventId = null;
        this.ticketQuantity = 1;
        this.ticketType = 'REGULAR';
        this.baseTicketPrice = this.ticketPrices['REGULAR'];
        this.clearPromo();
        this.recalculateTicketPrice();
        this.selectedSeats = [];

        if (newRemaining === 0) {
          alert(`${purchasedQuantity} billet(s) (${purchasedType}) acheté(s) pour ${selectedEvent.name}. Les billets sont maintenant épuisés.`);
          return;
        }

        alert(`${purchasedQuantity} billet(s) (${purchasedType}) acheté(s) pour ${selectedEvent.name}. Il reste ${newRemaining} billet(s).`);
      },
      error: (error) => {
        console.error('Ticket creation failed:', error);
        alert(error?.error?.message ?? 'Impossible de créer les tickets côté serveur.');
      }
    });
  }

  onTicketTypeChange(type: string): void {
    this.ticketType = type as 'VIP' | 'REGULAR' | 'STUDENT';
    this.baseTicketPrice = this.ticketPrices[type];
    this.recalculateTicketPrice();
  }

  clearSearch(): void {
    this.searchQuery = '';
  }

  getEventSoldTickets(eventId: number): number {
    return this.purchasedTickets
      .filter(ticket => ticket.eventId === eventId)
      .reduce((sum, ticket) => sum + ticket.quantity, 0);
  }

  getEventRemainingTickets(eventId: number): number {
    const event = this.events.find(e => e.id === eventId);
    if (!event) {
      return 0;
    }
    return Math.max(0, event.nbTickets - this.getEventSoldTickets(eventId));
  }

  getEventSoldPercentage(eventId: number): number {
    const event = this.events.find(e => e.id === eventId);
    if (!event || event.nbTickets <= 0) {
      return 0;
    }
    return (this.getEventSoldTickets(eventId) / event.nbTickets) * 100;
  }

  getTicketTypePercentage(type: 'VIP' | 'REGULAR' | 'STUDENT'): number {
    const total = this.selectedEventTypeStatsTotal;
    if (total <= 0) {
      return 0;
    }

    const stat = this.selectedEventTypeStats.find(s => s.type === type);
    if (!stat) {
      return 0;
    }

    return (stat.quantity / total) * 100;
  }

  getPurchasedTypePercentage(type: 'VIP' | 'REGULAR' | 'STUDENT'): number {
    const total = this.purchasedTypeStatsTotal;
    if (total <= 0) {
      return 0;
    }

    const stat = this.purchasedTypeStats.find(s => s.type === type);
    if (!stat) {
      return 0;
    }

    return (stat.quantity / total) * 100;
  }
}
