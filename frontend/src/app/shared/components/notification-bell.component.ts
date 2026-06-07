import { Component, inject, signal, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services';

@Component({
  selector: 'appNotificationBell',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button (click)="toggleDropdown()" class="relative p-2 hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-lg transition-colors" title="Notifications">
      <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 text-neutral-700 dark:text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6-6v.009l.01-.009" />
      </svg>
      <span *ngIf="unreadCount() > 0" class="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] rounded-full bg-esprit-red text-white text-[10px] font-bold flex items-center justify-center">{{ unreadCount() > 9 ? '9+' : unreadCount() }}</span>
    </button>

    <div *ngIf="showDropdown()" class="absolute right-0 mt-2 w-80 max-h-96 overflow-y-auto bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 rounded-xl shadow-lg z-50">
      <div class="flex items-center justify-between p-3 border-b border-neutral-200 dark:border-neutral-700">
        <span class="text-sm font-bold text-primary dark:text-white">Notifications</span>
        <button *ngIf="unreadCount() > 0" (click)="markAllRead()" class="text-[11px] text-esprit-red hover:underline">Mark all read</button>
      </div>
      <div *ngIf="loading()" class="p-4 text-center text-sm text-secondary">Loading...</div>
      <div *ngIf="!loading() && notifications().length === 0" class="p-4 text-center text-sm text-secondary">No notifications</div>
      <div *ngFor="let n of notifications()" class="p-3 border-b border-neutral-100 dark:border-neutral-700 hover:bg-neutral-50 dark:hover:bg-neutral-750 cursor-pointer transition-colors" (click)="markRead(n.id)">
        <div class="flex items-start gap-2">
          <span class="mt-0.5 w-2 h-2 rounded-full flex-shrink-0" [class]="!n.isRead ? 'bg-esprit-red' : 'bg-neutral-300'"></span>
          <div class="flex-1 min-w-0">
            <div class="flex items-center justify-between gap-1">
              <span class="text-xs font-semibold text-primary dark:text-white truncate">{{ n.title }}</span>
              <span class="text-[10px] px-1.5 py-0.5 rounded-full flex-shrink-0"
                [class]="n.priority === 'HIGH' ? 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400' : n.priority === 'MEDIUM' ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400' : 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400'"></span>
            </div>
            <p *ngIf="n.message" class="text-[11px] text-secondary mt-0.5 line-clamp-2">{{ n.message }}</p>
            <p class="text-[10px] text-secondary mt-1">{{ formatTime(n.createdAt) }}</p>
          </div>
        </div>
      </div>
    </div>
  `
})
export class NotificationBellComponent implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  readonly showDropdown = signal(false);
  readonly notifications = signal<any[]>([]);
  readonly unreadCount = signal(0);
  readonly loading = signal(false);

  private pollInterval: any;

  ngOnInit() {
    this.loadNotifications();
    this.pollInterval = setInterval(() => this.loadSummary(), 15000);
  }

  ngOnDestroy() {
    if (this.pollInterval) clearInterval(this.pollInterval);
  }

  private loadNotifications() {
    this.loading.set(true);
    this.http.get<any>('/api/srv/notifications?size=20').subscribe({
      next: (data) => this.notifications.set(data.content || []),
      error: () => this.loading.set(false)
    });
    this.http.get<any>('/api/srv/notifications/summary').subscribe({
      next: (data) => this.unreadCount.set(data.unreadCount || 0),
      error: () => {}
    });
  }

  private loadSummary() {
    this.http.get<any>('/api/srv/notifications/summary').subscribe({
      next: (data) => this.unreadCount.set(data.unreadCount || 0),
      error: () => {}
    });
  }

  toggleDropdown() {
    if (this.showDropdown()) {
      this.showDropdown.set(false);
    } else {
      this.loadNotifications();
      this.showDropdown.set(true);
    }
  }

  markRead(id: number) {
    this.http.put('/api/srv/notifications/' + id + '/read', {}).subscribe({
      next: () => this.loadSummary(),
      error: () => {}
    });
  }

  markAllRead() {
    this.http.put('/api/srv/notifications/read-all', {}).subscribe({
      next: () => {
        this.notifications.update(notifs => notifs.map(n => ({ ...n, isRead: true })));
        this.unreadCount.set(0);
      },
      error: () => {}
    });
  }

  formatTime(dateStr: string): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - d.getTime();
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin < 1) return 'Just now';
    if (diffMin < 60) return diffMin + 'm ago';
    const diffH = Math.floor(diffMin / 60);
    if (diffH < 24) return diffH + 'h ago';
    const diffD = Math.floor(diffH / 24);
    if (diffD < 7) return diffD + 'd ago';
    return d.toLocaleDateString();
  }
}
