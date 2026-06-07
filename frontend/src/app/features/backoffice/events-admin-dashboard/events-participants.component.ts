import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventPlanningApiService, EventResource, TicketResource } from '../../../services';

@Component({
  selector: 'app-events-participants',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="p-6 bg-white dark:bg-neutral-800 rounded-lg shadow-md">
      <h2 class="text-2xl font-bold text-primary mb-6">Events with Participants</h2>

      <div *ngIf="loading" class="text-center py-8">
        <p class="text-secondary">Loading events with participants...</p>
      </div>

      <div *ngIf="!loading && error" class="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
        <p class="text-red-800 font-medium">Error: {{ error }}</p>
      </div>

      <div *ngIf="!loading && eventsWithParticipants.length === 0" class="text-center py-8">
        <p class="text-secondary">No events found</p>
      </div>

      <div *ngIf="!loading && eventsWithParticipants.length > 0" class="space-y-6">
        <div *ngFor="let event of eventsWithParticipants" class="border border-neutral-200 rounded-lg p-6 hover:shadow-md transition-shadow">
          <!-- Event Header -->
          <div class="flex justify-between items-start mb-4">
            <div>
              <h3 class="text-xl font-bold text-primary">{{ event.name }}</h3>
              <div class="flex gap-4 mt-2 text-sm text-secondary">
                <span>📅 {{ event.date | date: 'dd MMM yyyy' }}</span>
                <span>📍 {{ event.location }}</span>
                <span *ngIf="event.online" class="text-blue-600">🌐 Online</span>
              </div>
            </div>
            <span class="bg-esprit-red/10 text-esprit-red px-3 py-1 rounded-full text-sm font-medium">
              {{ getParticipantCount(event) }} Participants
            </span>
          </div>

          <!-- Participants Table -->
          <div class="overflow-x-auto bg-neutral-50 dark:bg-neutral-700 rounded-lg">
            <table class="w-full text-sm">
              <thead class="bg-neutral-100 dark:bg-neutral-600">
                <tr>
                  <th class="text-left px-4 py-3 font-semibold">Participant Name</th>
                  <th class="text-left px-4 py-3 font-semibold">Ticket Type</th>
                  <th class="text-left px-4 py-3 font-semibold">Ticket Price</th>
                  <th class="text-left px-4 py-3 font-semibold">User ID</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngIf="!event.tickets || event.tickets.length === 0" class="border-t border-neutral-200 dark:border-neutral-600">
                  <td colspan="4" class="px-4 py-3 text-center text-secondary">No tickets sold yet</td>
                </tr>
                <tr *ngFor="let ticket of event.tickets" class="border-t border-neutral-200 dark:border-neutral-600 hover:bg-neutral-100 dark:hover:bg-neutral-600 transition-colors">
                  <td class="px-4 py-3">{{ ticket.user?.name || 'Anonymous' }}</td>
                  <td class="px-4 py-3 font-medium">{{ ticket.type }}</td>
                  <td class="px-4 py-3">{{ ticket.price | currency }}</td>
                  <td class="px-4 py-3 text-secondary">{{ ticket.user?.id || '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Event Stats -->
          <div class="mt-4 pt-4 border-t border-neutral-200 grid grid-cols-3 gap-4 text-sm">
            <div class="text-center">
              <p class="text-secondary">Total Revenue</p>
              <p class="text-lg font-bold text-esprit-red">{{ getTotalRevenue(event) | currency }}</p>
            </div>
            <div class="text-center">
              <p class="text-secondary">Avg Ticket Price</p>
              <p class="text-lg font-bold text-esprit-red">{{ getAverageTicketPrice(event) | currency }}</p>
            </div>
            <div class="text-center">
              <p class="text-secondary">Event Status</p>
              <p class="text-lg font-bold" [class]="getStatusClass(event.status)">{{ event.status }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class EventsParticipantsComponent implements OnInit {
  private eventPlanningApi = inject(EventPlanningApiService);

  eventsWithParticipants: EventResource[] = [];
  loading = false;
  error: string | null = null;

  ngOnInit(): void {
    this.loadEventsWithParticipants();
  }

  loadEventsWithParticipants(): void {
    this.loading = true;
    this.error = null;

    this.eventPlanningApi.getEventsWithParticipants().subscribe({
      next: (events) => {
        this.eventsWithParticipants = events;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load events with participants';
        this.loading = false;
      }
    });
  }

  getParticipantCount(event: EventResource): number {
    return event.tickets?.length ?? 0;
  }

  getTotalRevenue(event: EventResource): number {
    return (event.tickets ?? []).reduce((sum: number, ticket: TicketResource) => sum + (ticket.price ?? 0), 0);
  }

  getAverageTicketPrice(event: EventResource): number {
    const tickets = event.tickets ?? [];
    if (tickets.length === 0) return 0;
    return this.getTotalRevenue(event) / tickets.length;
  }

  getStatusClass(status?: string): string {
    const baseClass = 'text-lg font-bold';
    switch (status?.toLowerCase()) {
      case 'upcoming':
        return `${baseClass} text-blue-600`;
      case 'ongoing':
        return `${baseClass} text-green-600`;
      case 'finished':
        return `${baseClass} text-gray-600`;
      default:
        return `${baseClass} text-secondary`;
    }
  }
}
