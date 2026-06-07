import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { CrudApiService } from './crud-api.service';
import { ApiConfigService } from './api-config.service';
import {
  CollaborationResource,
  CollaborationWritePayload,
  EquipmentReservationResource,
  EquipmentReservationWritePayload,
  EquipmentResource,
  EquipmentWritePayload,
  EventResource,
  EventWritePayload,
  ReservationResource,
  ReservationWritePayload,
  StallResource,
  StallWritePayload,
  TicketPromoSelectionWritePayload,
  TicketResource,
  TicketPromoOfferResource,
  TicketWritePayload
} from './models/api-resource.model';

@Injectable({ providedIn: 'root' })
export class EventPlanningApiService {
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);

  readonly collaborations = new CrudApiService<CollaborationResource, CollaborationWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/eventplanning/collaborations')
  );

  readonly equipment = new CrudApiService<EquipmentResource, EquipmentWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/eventplanning/equipment')
  );

  readonly equipmentReservations = new CrudApiService<EquipmentReservationResource, EquipmentReservationWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/eventplanning/equipment-reservations')
  );

  readonly events = new CrudApiService<EventResource, EventWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/eventplanning/events')
  );

  readonly reservations = new CrudApiService<ReservationResource, ReservationWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/eventplanning/reservations')
  );

  readonly stalls = new CrudApiService<StallResource, StallWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/eventplanning/stalls')
  );

  readonly tickets = new CrudApiService<TicketResource, TicketWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/eventplanning/tickets')
  );

  /**
   * Get all events with their tickets and associated users (participants)
   */
  getEventsWithParticipants() {
    return this.http.get<EventResource[]>(
      this.apiConfig.buildUrl('/api/eventplanning/events/with-participants')
    );
  }

  /**
   * Get a specific event with tickets and participants
   */
  getEventWithParticipants(eventId: number) {
    return this.http.get<EventResource>(
      this.apiConfig.buildUrl(`/api/eventplanning/events/${eventId}/with-participants`)
    );
  }

  /**
   * Create reservation WITH equipment and quantities in one call ✅
   */
  createReservationWithEquipment(payload: any) {
    return this.http.post<ReservationResource>(
      this.apiConfig.buildUrl('/api/eventplanning/reservations/with-equipment'),
      payload
    );
  }

  getTicketPromoDates() {
    return this.http.get<string[]>(
      this.apiConfig.buildUrl('/api/eventplanning/tickets/promo-dates')
    );
  }

  getTicketPromoOffers() {
    return this.http.get<TicketPromoOfferResource[]>(
      this.apiConfig.buildUrl('/api/eventplanning/tickets/promo-offers')
    );
  }

  submitTicketPromoSelection(payload: TicketPromoSelectionWritePayload) {
    return this.http.post<void>(
      this.apiConfig.buildUrl('/api/eventplanning/tickets/promo-selection'),
      payload
    );
  }

  // Seats / seating plan (basic endpoints used by frontend POC)
  getSeats(eventId: number) {
    return this.http.get<any[]>(
      this.apiConfig.buildUrl(`/api/eventplanning/events/${eventId}/seats`)
    );
  }

  reserveSeat(eventId: number, seatId: string, payload: { holderId?: string; expiresInSec?: number }) {
    return this.http.post<any>(
      this.apiConfig.buildUrl(`/api/eventplanning/events/${eventId}/seats/${encodeURIComponent(seatId)}/reserve`),
      payload
    );
  }

  confirmSeat(eventId: number, seatId: string, payload: { reservationId: string }) {
    return this.http.post<any>(
      this.apiConfig.buildUrl(`/api/eventplanning/events/${eventId}/seats/${encodeURIComponent(seatId)}/confirm`),
      payload
    );
  }

  releaseSeat(eventId: number, seatId: string, payload: { reservationId?: string }) {
    return this.http.post<any>(
      this.apiConfig.buildUrl(`/api/eventplanning/events/${eventId}/seats/${encodeURIComponent(seatId)}/release`),
      payload
    );
  }

  /**
   * Get upcoming event alerts (events happening today or tomorrow)
   */
  getUpcomingEventAlerts() {
    return this.http.get<UpcomingEventAlert[]>(
      this.apiConfig.buildUrl('/api/eventplanning/events/upcoming-alerts')
    );
  }
}

export interface UpcomingEventAlert {
  id: number;
  name: string;
  date: string;
  location: string;
  online: boolean;
  nbTickets: number;
  daysText: string;  // "aujourd'hui" or "demain"
}
