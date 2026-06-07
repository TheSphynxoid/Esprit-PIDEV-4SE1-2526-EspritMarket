import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterOutlet, RouterLinkActive } from '@angular/router';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { RecruiterCacheService } from './recruiter-cache.service';
import { WebSocketService, NotificationMessage } from '../../../services/websocket.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-recruiter-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet, RouterLinkActive],
  templateUrl: './recruiter-dashboard.component.html',
  styleUrls: ['./recruiter-dashboard.component.css']
})
export class RecruiterDashboardComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private router = inject(Router);
  private recruiterCache = inject(RecruiterCacheService);
  private webSocketService = inject(WebSocketService);
  private cdr = inject(ChangeDetectorRef);
  private sub = new Subscription();

  // Notification state
  notifications: NotificationMessage[] = [];
  unreadCount = 0;
  showNotificationPanel = false;
  toasts: NotificationMessage[] = [];

  ngOnInit(): void {
    this.recruiterCache.preloadAll();

    // Connect WebSocket with recruiter userId (1 for now)
    this.webSocketService.connect(1);

    // Listen for new notifications (toasts)
    this.sub.add(
      this.webSocketService.notification$.subscribe((notification) => {
        // Use immutable update to trigger Change Detection
        this.toasts = [notification, ...this.toasts];
        this.cdr.detectChanges(); 

        // Auto-remove toast after 5 seconds
        setTimeout(() => {
          this.toasts = this.toasts.filter(t => t.id !== notification.id);
          this.cdr.detectChanges();
        }, 5000);
      })
    );

    // Keep notification list synced
    this.sub.add(
      this.webSocketService.notifications$.subscribe((list) => {
        this.notifications = [...list];
        this.cdr.detectChanges();
      })
    );

    // Keep unread count synced
    this.sub.add(
      this.webSocketService.unreadCount$.subscribe((count) => {
        this.unreadCount = count;
        this.cdr.detectChanges();
      })
    );
  }

  toggleNotificationPanel(): void {
    this.showNotificationPanel = !this.showNotificationPanel;
  }

  markAsRead(notification: NotificationMessage): void {
    if (!notification.isRead) {
      this.webSocketService.markAsRead(notification.id);
    }
  }

  markAllAsRead(): void {
    this.webSocketService.markAllAsRead();
  }

  removeToast(toast: NotificationMessage): void {
    const idx = this.toasts.indexOf(toast);
    if (idx > -1) this.toasts.splice(idx, 1);
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
    this.webSocketService.disconnect();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
