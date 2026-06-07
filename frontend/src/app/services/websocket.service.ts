import { Injectable, OnDestroy, NgZone, inject } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { ApiConfigService } from './api/api-config.service';

export interface NotificationMessage {
  id: number;
  message: string;
  type: 'APPLICATION' | 'INTERVIEW_REMINDER' | 'RESULT';
  createdAt: string;
  isRead: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements OnDestroy {
  private ngZone = inject(NgZone);
  private http = inject(HttpClient);
  private apiConfig = inject(ApiConfigService);

  private ws: WebSocket | null = null;
  private connected = false;
  private userId: number | null = null;

  // Streams
  private notificationSubject = new Subject<NotificationMessage>();
  private notificationsListSubject = new BehaviorSubject<NotificationMessage[]>([]);
  private unreadCountSubject = new BehaviorSubject<number>(0);

  public notification$ = this.notificationSubject.asObservable();
  public notifications$ = this.notificationsListSubject.asObservable();
  public unreadCount$ = this.unreadCountSubject.asObservable();

  /**
   * Connect to WebSocket and subscribe to user-specific notifications.
   */
  connect(userId: number): void {
    if (this.connected && this.userId === userId) return;
    this.userId = userId;

    // Load existing notifications from DB
    this.loadNotifications(userId);

    try {
      const ws = new WebSocket('ws://localhost:8088/ws/websocket');

      ws.onopen = () => {
        console.log('[WebSocket] Connection opened');
        ws.send('CONNECT\naccept-version:1.2\nhost:localhost\n\n\0');
      };

      ws.onmessage = (event) => {
        const data = event.data as string;

        if (data.startsWith('CONNECTED')) {
          console.log('[WebSocket] STOMP connected');
          this.connected = true;
          // Subscribe to user-specific notifications
          ws.send(`SUBSCRIBE\nid:sub-0\ndestination:/topic/notifications/${userId}\n\n\0`);
        }

        if (data.startsWith('MESSAGE')) {
          const bodyStart = data.indexOf('\n\n');
          if (bodyStart !== -1) {
            const body = data.substring(bodyStart + 2).replace('\0', '').trim();
            if (body) {
              try {
                const parsed = JSON.parse(body);
                const notification: NotificationMessage = {
                  id: parsed.id,
                  message: parsed.message,
                  type: parsed.type,
                  createdAt: parsed.createdAt,
                  isRead: false
                };
                this.ngZone.run(() => {
                  // Push to stream
                  this.notificationSubject.next(notification);
                  // Add to list
                  const current = this.notificationsListSubject.value;
                  this.notificationsListSubject.next([notification, ...current]);
                  // Increment unread count
                  this.unreadCountSubject.next(this.unreadCountSubject.value + 1);
                });
              } catch (e) {
                console.warn('[WebSocket] Failed to parse message:', body);
              }
            }
          }
        }
      };

      ws.onerror = () => {
        console.warn('[WebSocket] Connection error');
      };

      ws.onclose = () => {
        console.log('[WebSocket] Closed, retrying in 5s');
        this.connected = false;
        setTimeout(() => {
          if (this.userId) this.connect(this.userId);
        }, 5000);
      };

      this.ws = ws;
    } catch (e) {
      console.warn('[WebSocket] Failed to connect:', e);
    }
  }

  /**
   * Load existing notifications from the REST API.
   */
  private loadNotifications(userId: number): void {
    this.http.get<any[]>(
      this.apiConfig.buildUrl(`/api/partnership/notifications/${userId}`)
    ).subscribe({
      next: (notifications) => {
        const mapped: NotificationMessage[] = notifications.map(n => ({
          id: n.id,
          message: n.message,
          type: n.type,
          createdAt: n.createdAt,
          isRead: n.isRead ?? n.read ?? false
        }));
        this.notificationsListSubject.next(mapped);
        this.unreadCountSubject.next(mapped.filter(n => !n.isRead).length);
      },
      error: (err) => console.warn('[WebSocket] Failed to load notifications:', err)
    });
  }

  /**
   * Mark a notification as read.
   */
  markAsRead(notificationId: number): void {
    this.http.put(
      this.apiConfig.buildUrl(`/api/partnership/notifications/${notificationId}/read`),
      {}
    ).subscribe({
      next: () => {
        const current = this.notificationsListSubject.value.map(n =>
          n.id === notificationId ? { ...n, isRead: true } : n
        );
        this.notificationsListSubject.next(current);
        this.unreadCountSubject.next(current.filter(n => !n.isRead).length);
      }
    });
  }

  /**
   * Mark all notifications as read.
   */
  markAllAsRead(): void {
    if (!this.userId) return;
    this.http.put(
      this.apiConfig.buildUrl(`/api/partnership/notifications/${this.userId}/read-all`),
      {}
    ).subscribe({
      next: () => {
        const current = this.notificationsListSubject.value.map(n => ({ ...n, isRead: true }));
        this.notificationsListSubject.next(current);
        this.unreadCountSubject.next(0);
      }
    });
  }

  disconnect(): void {
    if (this.ws) {
      try { this.ws.close(); } catch (e) { /* ignore */ }
      this.ws = null;
      this.connected = false;
    }
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
