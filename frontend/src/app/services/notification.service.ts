import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';

import { ApiConfigService } from './api/api-config.service';
import { AuthService } from './auth.service';

export interface RealtimeNotification {
  id: number;
  recipientId: number;
  type: string;
  title: string;
  message: string;
  relatedEntityId?: number | null;
  relatedEntityType?: string | null;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly apiConfig = inject(ApiConfigService);
  private readonly authService = inject(AuthService);

  private readonly notificationsState = signal<RealtimeNotification[]>([]);
  readonly notifications = computed(() => this.notificationsState());

  private stompClient: Client | null = null;
  private notificationSubscription: StompSubscription | null = null;
  private connectedUserId: number | null = null;

  constructor() {
    effect(() => {
      const user = this.authService.currentUser();
      const loggedIn = this.authService.isLoggedIn();

      if (loggedIn && user?.id) {
        this.requestBrowserPermissionIfNeeded();
        this.connect(user.id);
        return;
      }

      this.disconnect();
    });
  }

  dismiss(id: number): void {
    this.notificationsState.update((items) => items.filter((item) => item.id !== id));
  }

  private connect(userId: number): void {
    if (this.connectedUserId === userId && this.stompClient?.active) {
      return;
    }

    this.disconnect();

    const wsUrl = this.buildNativeWsUrl('/ws-marketplace-native');
    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      debug: () => undefined,
    });

    client.onConnect = () => {
      this.connectedUserId = userId;
      this.notificationSubscription = client.subscribe(
        `/topic/notifications/users/${userId}`,
        (message: IMessage) => {
          this.handleIncomingMessage(message.body);
        }
      );
    };

    client.onStompError = (frame) => {
      console.error('[Notifications] STOMP error:', frame.headers['message'], frame.body);
    };

    client.onWebSocketError = (event) => {
      console.error('[Notifications] WebSocket error:', event);
    };

    client.activate();
    this.stompClient = client;
  }

  private disconnect(): void {
    this.notificationSubscription?.unsubscribe();
    this.notificationSubscription = null;
    this.connectedUserId = null;

    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
  }

  private handleIncomingMessage(body: string): void {
    try {
      const parsed = JSON.parse(body) as Partial<RealtimeNotification>;
      const notification: RealtimeNotification = {
        id: Date.now() + Math.floor(Math.random() * 1000),
        recipientId: Number(parsed.recipientId || 0),
        type: String(parsed.type || 'NOTIFICATION'),
        title: String(parsed.title || 'Notification'),
        message: String(parsed.message || ''),
        relatedEntityId: parsed.relatedEntityId ?? null,
        relatedEntityType: parsed.relatedEntityType ?? null,
        timestamp: String(parsed.timestamp || new Date().toISOString())
      };

      this.notificationsState.update((items) => [notification, ...items].slice(0, 5));
      this.showBrowserNotification(notification);

      window.setTimeout(() => this.dismiss(notification.id), 8000);
    } catch (error) {
      console.error('[Notifications] Failed to parse incoming payload:', error);
    }
  }

  private requestBrowserPermissionIfNeeded(): void {
    if (!this.canUseBrowserNotifications()) {
      return;
    }

    if (Notification.permission === 'default') {
      Notification.requestPermission().catch(() => undefined);
    }
  }

  private showBrowserNotification(notification: RealtimeNotification): void {
    if (!this.canUseBrowserNotifications() || Notification.permission !== 'granted') {
      return;
    }

    const browserNotification = new Notification(notification.title, {
      body: notification.message,
      tag: `order-${notification.relatedEntityId ?? notification.id}`,
    });

    window.setTimeout(() => browserNotification.close(), 7000);
  }

  private canUseBrowserNotifications(): boolean {
    return typeof window !== 'undefined' && 'Notification' in window;
  }

  private buildNativeWsUrl(path: string): string {
    const httpUrl = this.apiConfig.buildUrl(path);
    if (httpUrl.startsWith('https://')) {
      return `wss://${httpUrl.substring('https://'.length)}`;
    }
    return `ws://${httpUrl.substring('http://'.length)}`;
  }
}