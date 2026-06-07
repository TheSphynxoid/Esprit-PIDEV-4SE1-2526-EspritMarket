import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventSchedulerService, EventStatus, TicketReminder } from './events-scheduler-dashboard.service';
import { FooterComponent } from '../../../shared/layout';

@Component({
  selector: 'app-events-scheduler-dashboard',
  standalone: true,
  imports: [CommonModule, FooterComponent],
  templateUrl: './events-scheduler-dashboard.component.html',
  styleUrls: ['./events-scheduler-dashboard.component.css']
})
export class EventsSchedulerDashboardComponent implements OnInit {
  private schedulerService = inject(EventSchedulerService);

  events: EventStatus[] = [];
  ticketReminders: TicketReminder[] = [];
  loading = true;
  error: string | null = null;

  ngOnInit(): void {
    this.loadEventStatuses();
  }

  loadEventStatuses(): void {
    this.loading = true;
    this.error = null;

    this.schedulerService.getEventStatusDashboard().subscribe({
      next: (data) => {
        this.events = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load event statuses';
        console.error('Error loading event statuses:', err);
        this.loading = false;
      }
    });

    this.schedulerService.getTicketSaleReminders().subscribe({
      next: (data) => {
        this.ticketReminders = data;
      },
      error: (err) => {
        console.error('Error loading ticket reminders:', err);
        this.ticketReminders = [];
      }
    });
  }

  getStatusBadgeColor(status: string): string {
    switch (status) {
      case 'FINISHED':
        return 'badge-error';
      case 'ONGOING':
        return 'badge-warning';
      case 'UPCOMING':
        return 'badge-info';
      default:
        return 'badge-secondary';
    }
  }

  getEquipmentStatusBadgeColor(status: string): string {
    switch (status) {
      case 'AVAILABLE':
        return 'badge-success';
      case 'IN_USE':
        return 'badge-warning';
      case 'MAINTENANCE':
        return 'badge-error';
      default:
        return 'badge-secondary';
    }
  }

  getStallStatusBadgeColor(status: string): string {
    switch (status) {
      case 'AVAILABLE':
        return 'badge-success';
      case 'ASSIGNED':
        return 'badge-warning';
      default:
        return 'badge-secondary';
    }
  }

  refresh(): void {
    this.loadEventStatuses();
  }
}
