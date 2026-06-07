import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EquipmentStatus {
  id: number;
  name: string;
  type: string;
  status: string;
}

export interface StallStatus {
  id: number;
  name: string;
  number: number;
  location: string;
  status: string;
}

export interface EventStatus {
  id: number;
  name: string;
  date: string;
  location: string;
  online: boolean;
  status: string;
  equipments: EquipmentStatus[];
  stalls: StallStatus[];
}

export interface TicketReminder {
  eventId: number;
  eventName: string;
  remainingTickets: number;
  title: string;
  message: string;
  severity: 'info' | 'warning' | 'danger';
  status: string;
}

@Injectable({ providedIn: 'root' })
export class EventSchedulerService {
  private http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8088/api/eventplanning/events';

  getEventStatusDashboard(): Observable<EventStatus[]> {
    return this.http.get<EventStatus[]>(`${this.API_URL}/status/dashboard`);
  }

  getTicketSaleReminders(): Observable<TicketReminder[]> {
    return this.http.get<TicketReminder[]>(`${this.API_URL}/ticket-reminders`);
  }
}
