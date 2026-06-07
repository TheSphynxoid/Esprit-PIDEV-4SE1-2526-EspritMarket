import { Injectable, OnDestroy, inject, signal } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

import { ApiConfigService } from './api/api-config.service';

export type BookingRealtimeMessage = {
  id?: number;
  bookingId?: number;
  senderId?: number;
  senderName?: string;
  message?: string;
  createdAt?: string;
};

@Injectable({ providedIn: 'root' })
export class BookingChatRealtimeService implements OnDestroy {
  private readonly apiConfig = inject(ApiConfigService);
  private readonly client = new Client({
    reconnectDelay: 3000,
    heartbeatIncoming: 20000,
    heartbeatOutgoing: 20000
  });

  readonly isConnected = signal(false);
  private activeSubscriptionBookingId: number | null = null;

  constructor() {
    this.client.webSocketFactory = () => {
      const baseUrl = this.apiConfig.baseUrl;
      const wsUrl = baseUrl ? `${baseUrl}/ws-marketplace` : '/ws-marketplace';
      return new SockJS(wsUrl);
    };

    this.client.onConnect = () => {
      this.isConnected.set(true);
    };

    this.client.onWebSocketClose = () => {
      this.isConnected.set(false);
    };

    this.client.onStompError = () => {
      this.isConnected.set(false);
    };

    this.client.activate();
  }

  subscribeToBookingMessages(bookingId: number, onMessage: (message: BookingRealtimeMessage) => void): () => void {
    if (!this.client.connected) {
      const timeout = setInterval(() => {
        if (this.client.connected) {
          clearInterval(timeout);
          this.subscribeInternal(bookingId, onMessage);
        }
      }, 250);

      return () => clearInterval(timeout);
    }

    return this.subscribeInternal(bookingId, onMessage);
  }

  private subscribeInternal(bookingId: number, onMessage: (message: BookingRealtimeMessage) => void): () => void {
    if (this.activeSubscriptionBookingId === bookingId) {
      return () => {};
    }

    const subscription = this.client.subscribe(
      `/topic/srv/bookings/${bookingId}/messages`,
      (frame: IMessage) => {
        try {
          const payload = JSON.parse(frame.body) as BookingRealtimeMessage;
          onMessage(payload);
        } catch {
          // ignore malformed payload
        }
      }
    );

    this.activeSubscriptionBookingId = bookingId;
    return () => {
      subscription.unsubscribe();
      if (this.activeSubscriptionBookingId === bookingId) {
        this.activeSubscriptionBookingId = null;
      }
    };
  }

  ngOnDestroy(): void {
    this.client.deactivate();
  }
}
