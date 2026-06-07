import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { map } from 'rxjs/operators';

import { ApiConfigService, JwtService } from './api';

export interface ChatMessage {
  id?: number;
  clientId: string;
  senderId: string;
  receiverId: string;
  content: string;
  sentAt: string;
  read?: boolean;
}

interface IncomingMessage {
  id?: number;
  senderId: string;
  receiverId: string;
  content: string;
  sentAt?: string;
  read?: boolean;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);
  private readonly jwtService = inject(JwtService);

  private readonly connected = signal(false);
  readonly isConnected = computed(() => this.connected());

  private client: Client | null = null;
  private connectPromise: Promise<void> | null = null;
  private incomingSubscription: StompSubscription | null = null;

  private readonly incomingSubject = new Subject<ChatMessage>();
  readonly incomingMessages$ = this.incomingSubject.asObservable();

  private stompClientModule: typeof import('@stomp/stompjs') | null = null;
  private sockJsFactory: ((url: string) => WebSocket) | null = null;

  async ensureConnected(): Promise<void> {
    if (this.connected()) {
      return;
    }

    if (this.connectPromise) {
      return this.connectPromise;
    }

    const token = this.jwtService.getToken();
    if (!token) {
      return Promise.reject(new Error('JWT manquant: veuillez vous reconnecter.'));
    }

    const wsUrl = `${this.apiConfig.buildUrl('/ws')}?token=${encodeURIComponent(token)}`;

    const { Client } = await this.loadStompDeps();
    const sockjs = await this.getSockJsFactory();

    this.client = new Client({
      webSocketFactory: () => sockjs(wsUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
        token: token
      },
      debug: (msg) => {
        try {
          console.debug('[STOMP]', msg);
        } catch {
          // ignore console errors in restricted environments
        }
      }
    });

    this.connectPromise = new Promise<void>((resolve, reject) => {
      const client = this.client;
      if (!client) {
        reject(new Error('Client STOMP non initialisé.'));
        return;
      }

      client.onConnect = () => {
        this.connected.set(true);

        if (!this.incomingSubscription) {
          this.incomingSubscription = client.subscribe('/user/queue/messages', (message) => {
            try {
              const parsed = JSON.parse(message.body) as IncomingMessage;
              this.incomingSubject.next(this.normalize(parsed));
            } catch {
              // ignore malformed payloads
            }
          });
        }

        resolve();
      };

      client.onStompError = () => {
        this.connected.set(false);
        reject(new Error('Erreur STOMP: connexion refusée ou serveur indisponible.'));
      };

      client.onWebSocketError = () => {
        this.connected.set(false);
        reject(new Error('Erreur WebSocket: impossible de se connecter au serveur.'));
      };

      client.onWebSocketClose = () => {
        this.connected.set(false);
        reject(new Error('La connexion STOMP a été fermée'));
      };

      client.activate();
    });

    try {
      await this.connectPromise;
    } finally {
      this.connectPromise = null;
    }
  }

  disconnect(): void {
    this.connected.set(false);

    if (this.incomingSubscription) {
      try {
        this.incomingSubscription.unsubscribe();
      } catch {
        // no-op
      }
      this.incomingSubscription = null;
    }

    if (this.client) {
      try {
        this.client.deactivate();
      } catch {
        // no-op
      }
      this.client = null;
    }
  }

  getConversation(receiverId: string): Observable<ChatMessage[]> {
    const safe = encodeURIComponent(receiverId.trim());
    return this.http.get<IncomingMessage[]>(this.apiConfig.buildUrl(`/api/messages/${safe}`)).pipe(
      map((messages) => (messages ?? []).map((m) => this.normalize(m)))
    );
  }

  async sendMessage(receiverId: string, content: string): Promise<void> {
    const safeReceiverId = receiverId.trim();
    const safeContent = content.trim();

    if (!safeReceiverId) {
      throw new Error('ReceiverId manquant.');
    }

    if (!safeContent) {
      throw new Error('Message vide.');
    }

    if (safeContent.length > 2000) {
      throw new Error('Message trop long (max 2000 caractères).');
    }

    await this.ensureConnected();

    if (!this.client || !this.connected()) {
      throw new Error('Non connecté au chat.');
    }

    this.client.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({ receiverId: safeReceiverId, content: safeContent })
    });
  }

  private normalize(message: IncomingMessage): ChatMessage {
    const sentAt = message.sentAt || new Date().toISOString();
    const idPart = typeof message.id === 'number' ? String(message.id) : 'tmp';
    const randomPart = this.randomId();

    return {
      id: message.id,
      clientId: `${idPart}-${sentAt}-${randomPart}`,
      senderId: message.senderId,
      receiverId: message.receiverId,
      content: message.content,
      sentAt,
      read: message.read
    };
  }

  private randomId(): string {
    try {
      return crypto.randomUUID();
    } catch {
      return String(Math.random()).slice(2);
    }
  }

  private async loadStompDeps(): Promise<typeof import('@stomp/stompjs')> {
    if (this.stompClientModule) {
      return this.stompClientModule;
    }

    const module = await import('@stomp/stompjs');
    this.stompClientModule = module;
    return module;
  }

  private async getSockJsFactory(): Promise<(url: string) => WebSocket> {
    if (this.sockJsFactory) {
      return this.sockJsFactory;
    }

    const module = await import('sockjs-client');
    const SockJS = (module as unknown as { default?: unknown }).default ?? module;
    const factory = SockJS as unknown as (url: string) => WebSocket;

    this.sockJsFactory = factory;
    return factory;
  }
}

type Client = import('@stomp/stompjs').Client;
type StompSubscription = import('@stomp/stompjs').StompSubscription;
