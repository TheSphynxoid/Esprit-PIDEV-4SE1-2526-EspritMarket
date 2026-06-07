import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import { EventPlanningApiService } from '../../../services';
import { EventResource } from '../../../services/api/models/api-resource.model';

@Component({
  selector: 'app-events',
  standalone: true,
  imports: [CommonModule, HeaderComponent, FooterComponent],
  templateUrl: './events.component.html',
  styleUrls: []
})
export class EventsComponent {
  private readonly eventPlanningApi = inject(EventPlanningApiService);
  private readonly router = inject(Router);

  events: Array<{
    id: number;
    name: string;
    date: string;
    time: string;
    location: string;
    description: string;
    emoji: string;
    attendees: number;
    price: number;
    hasStands: boolean;
    availableStands: number;
    totalStands: number;
  }> = [];
  loading = true;
  error: string | null = null;

  constructor() {
    this.loadEvents();
  }

  loadEvents(): void {
    this.loading = true;
    this.error = null;

    this.eventPlanningApi.getEventsWithParticipants().subscribe({
      next: (events) => {
        this.events = events.map((event) => ({
          id: event.id,
          name: event.name,
          date: event.date,
          time: event.online ? 'Online event' : '09:00 - 17:00',
          location: event.location,
          description: event.description ?? `Event organized at ${event.location}`,
          emoji: event.online ? '🌐' : '🎪',
          attendees: event.tickets?.length ?? 0,
          price: 0,
          hasStands: false,
          availableStands: 0,
          totalStands: 0
        }));
        this.loading = false;
      },
      error: (error) => {
        console.error('Failed to load events:', error);
        this.events = [];
        this.error = 'Impossible de charger les événements.';
        this.loading = false;
      }
    });
  }

  navigateToReservation(eventId: number): void {
    this.router.navigate(['/event-reservation', eventId]);
  }
}
