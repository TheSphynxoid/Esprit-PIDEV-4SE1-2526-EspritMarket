import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { FooterComponent, HeaderComponent } from '../../../shared/layout';
import { AuthService, ChatMessage, ChatService, CourierResource, DeliveryApiService, DeliveryResource } from '../../../services';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

type CourierContact = {
  courierId: number | null;
  name: string;
  phoneNumber: string;
};

type DeliveryTone = {
  accent: string;
  badge: string;
  dot: string;
  banner: string;
};

@Component({
  selector: 'app-my-deliveries',
  imports: [CommonModule, HeaderComponent, FooterComponent],
  templateUrl: './my-deliveries.component.html',
  styleUrls: [],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MyDeliveriesComponent {
  private readonly deliveryTones: DeliveryTone[] = [
    {
      accent: 'border-red-500',
      badge: 'bg-red-50 text-red-700 ring-1 ring-red-200',
      dot: 'bg-red-500',
      banner: 'from-red-600 via-rose-600 to-orange-500'
    },
    {
      accent: 'border-amber-500',
      badge: 'bg-amber-50 text-amber-800 ring-1 ring-amber-200',
      dot: 'bg-amber-500',
      banner: 'from-amber-600 via-orange-600 to-red-500'
    },
    {
      accent: 'border-emerald-500',
      badge: 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200',
      dot: 'bg-emerald-500',
      banner: 'from-emerald-600 via-teal-600 to-cyan-500'
    },
    {
      accent: 'border-sky-500',
      badge: 'bg-sky-50 text-sky-700 ring-1 ring-sky-200',
      dot: 'bg-sky-500',
      banner: 'from-sky-600 via-blue-600 to-indigo-500'
    },
    {
      accent: 'border-violet-500',
      badge: 'bg-violet-50 text-violet-700 ring-1 ring-violet-200',
      dot: 'bg-violet-500',
      banner: 'from-violet-600 via-fuchsia-600 to-pink-500'
    }
  ];

  private readonly deliveryApi = inject(DeliveryApiService);
  private readonly authService = inject(AuthService);
  private readonly chatService = inject(ChatService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly sanitizer = inject(DomSanitizer);

  readonly deliveries = signal<DeliveryResource[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  private readonly couriersById = signal<Map<number, CourierResource>>(new Map());
  private readonly courierContactsByVehiculeId = signal<Map<number, CourierContact>>(new Map());

  readonly hasDeliveries = computed(() => this.deliveries().length > 0);
  readonly expandedDeliveryId = signal<number | null>(null);
  readonly showMapForDeliveryId = signal<number | null>(null);

  readonly chatOpenDeliveryId = signal<number | null>(null);
  readonly chatReceiverId = signal<string | null>(null);
  readonly chatMessages = signal<ChatMessage[]>([]);
  readonly chatLoading = signal(false);
  readonly chatErrorMessage = signal<string | null>(null);
  readonly chatDraft = signal('');

  constructor() {
    this.loadDeliveries();

    this.chatService.incomingMessages$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((message) => {
        if (!this.isMessageInOpenConversation(message)) {
          return;
        }

        this.chatMessages.update((messages) => [...messages, message]);
      });
  }

  reload(): void {
    this.loadDeliveries();
  }

  isDeliveryExpanded(delivery: DeliveryResource): boolean {
    const deliveryId = typeof delivery.id === 'number' ? delivery.id : Number(delivery.id);
    return this.expandedDeliveryId() === deliveryId;
  }

  toggleDeliveryDetails(delivery: DeliveryResource): void {
    const deliveryId = typeof delivery.id === 'number' ? delivery.id : Number(delivery.id);
    if (!Number.isFinite(deliveryId)) {
      return;
    }

    if (this.expandedDeliveryId() === deliveryId) {
      this.expandedDeliveryId.set(null);
      return;
    }

    this.expandedDeliveryId.set(deliveryId);
  }

  closeDeliveryDetails(): void {
    this.expandedDeliveryId.set(null);
  }

  openInvoicePdf(delivery: DeliveryResource): void {
    const deliveryId = typeof delivery.id === 'number' ? delivery.id : Number(delivery.id);
    if (!Number.isFinite(deliveryId)) {
      return;
    }

    this.deliveryApi.getInvoice(deliveryId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank', 'noopener,noreferrer');
        window.setTimeout(() => window.URL.revokeObjectURL(url), 15000);
      },
      error: () => {
        this.errorMessage.set('Unable to open the invoice PDF right now.');
      }
    });
  }

  getStatusLabel(status: string | null | undefined): string {
    const normalized = (status || 'pending').toLowerCase();
    const labels: Record<string, string> = {
      delivered: 'Delivered',
      shipped: 'Shipped',
      confirmed: 'Confirmed',
      processing: 'Processing',
      pending: 'Pending',
      cancelled: 'Cancelled',
      canceled: 'Cancelled',
      'in progress': 'In progress',
      in_progress: 'In progress'
    };

    return labels[normalized] || (status || 'Pending');
  }

  getStatusClasses(status: string | null | undefined): string {
    const normalized = (status || 'pending').toLowerCase();
    if (normalized === 'delivered') {
      return 'bg-emerald-100 text-emerald-800 ring-1 ring-emerald-200';
    }

    if (normalized === 'shipped' || normalized === 'in progress' || normalized === 'in_progress') {
      return 'bg-blue-100 text-blue-800 ring-1 ring-blue-200';
    }

    if (normalized === 'confirmed' || normalized === 'processing') {
      return 'bg-amber-100 text-amber-800 ring-1 ring-amber-200';
    }

    if (normalized === 'cancelled' || normalized === 'canceled') {
      return 'bg-red-100 text-red-800 ring-1 ring-red-200';
    }

    return 'bg-neutral-100 text-neutral-700 ring-1 ring-neutral-200';
  }

  getDeliveryAccentClass(delivery: DeliveryResource): string {
    return this.getDeliveryTone(delivery).accent;
  }

  getDeliveryBadgeClass(delivery: DeliveryResource): string {
    return this.getDeliveryTone(delivery).badge;
  }

  getDeliveryDotClass(delivery: DeliveryResource): string {
    return this.getDeliveryTone(delivery).dot;
  }

  getDeliveryBannerClass(delivery: DeliveryResource): string {
    return this.getDeliveryTone(delivery).banner;
  }

  getOrderAmount(delivery: DeliveryResource): string {
    const amount = delivery.order?.totalAmount;
    if (typeof amount !== 'number') {
      return '—';
    }

    return `${amount.toFixed(2)} TND`;
  }

  getDeliveryDate(delivery: DeliveryResource): string {
    const date = delivery.deliverydate || delivery.order?.date;
    if (!date) return '—';
    
    const dateStr = String(date);
    const formatted = dateStr.split('T')[0];
    return formatted || dateStr;
  }

  getDeliveryRecipient(delivery: DeliveryResource): string {
    return delivery.deliveryAddress || delivery.address || delivery.order?.shippingAddress || '—';
  }

  hasAssignedCourier(delivery: DeliveryResource): boolean {
    const vehiculeId = this.getVehiculeId(delivery);
    if (vehiculeId != null && this.courierContactsByVehiculeId().has(vehiculeId)) {
      return true;
    }

    const courierId = delivery.courier?.id ?? delivery.vehicule?.courier?.id;
    const courierName = delivery.courier?.name?.trim() ?? delivery.vehicule?.courier?.name?.trim();
    return !!courierId || !!courierName;
  }

  getCourierName(delivery: DeliveryResource): string {
    const directName = this.readCourierDisplayName(delivery.courier) || this.readCourierDisplayName(delivery.vehicule?.courier);
    if (directName) return directName;

    const courierId = this.getCourierId(delivery);
    if (courierId != null) {
      const cached = this.couriersById().get(courierId);
      const cachedName = this.readCourierDisplayName(cached);
      if (cachedName) return cachedName;
    }

    const vehiculeId = this.getVehiculeId(delivery);
    if (vehiculeId != null) {
      const contact = this.courierContactsByVehiculeId().get(vehiculeId);
      if (contact?.name) {
        return contact.name;
      }
    }

    return '—';
  }

  getCourierPhone(delivery: DeliveryResource): string {
    const courierId = this.getCourierId(delivery);
    if (courierId != null) {
      const cached = this.couriersById().get(courierId) as unknown as { phoneNumber?: string; phone_number?: string; tel?: string } | undefined;
      const cachedPhone = cached?.phoneNumber || cached?.phone_number || cached?.tel;
      if (cachedPhone) {
        return cachedPhone;
      }
    }

    const maybeCourierPhone = (delivery.courier as unknown as { phoneNumber?: string; phone_number?: string; tel?: string } | null)?.phoneNumber
      || (delivery.courier as unknown as { phoneNumber?: string; phone_number?: string; tel?: string } | null)?.phone_number
      || (delivery.courier as unknown as { phoneNumber?: string; phone_number?: string; tel?: string } | null)?.tel;

    if (maybeCourierPhone) {
      return maybeCourierPhone;
    }

    const vehiculeId = this.getVehiculeId(delivery);
    if (vehiculeId != null) {
      const contact = this.courierContactsByVehiculeId().get(vehiculeId);
      if (contact?.phoneNumber) {
        return contact.phoneNumber;
      }
    }

    return '—';
  }

  getVehicleSerie(delivery: DeliveryResource): string {
    return (
      delivery.vehicule?.registrationnumbers ||
      delivery.courier?.vehicule?.registrationnumbers ||
      '—'
    );
  }

  isChatOpenForDelivery(delivery: DeliveryResource): boolean {
    const deliveryId = typeof delivery.id === 'number' ? delivery.id : Number(delivery.id);
    return this.chatOpenDeliveryId() === deliveryId;
  }

  toggleChat(delivery: DeliveryResource): void {
    if (this.isChatOpenForDelivery(delivery)) {
      this.closeChat();
      return;
    }

    void this.openChatAsync(delivery);
  }

  closeChat(): void {
    this.chatOpenDeliveryId.set(null);
    this.chatReceiverId.set(null);
    this.chatMessages.set([]);
    this.chatLoading.set(false);
    this.chatErrorMessage.set(null);
    this.chatDraft.set('');
  }

  onChatDraftInput(event: Event): void {
    const target = event.target as HTMLTextAreaElement | null;
    this.chatDraft.set(target?.value ?? '');
  }

  async sendChatMessage(): Promise<void> {
    const receiverId = this.chatReceiverId();
    if (!receiverId) {
      this.chatErrorMessage.set('Impossible d\'envoyer: identifiant du livreur introuvable.');
      return;
    }

    const content = this.chatDraft().trim();
    if (!content) {
      return;
    }

    this.chatErrorMessage.set(null);
    this.chatDraft.set('');

    this.chatMessages.update((messages) => [...messages, this.buildLocalMessage(receiverId, content)]);

    try {
      await this.chatService.sendMessage(receiverId, content);
    } catch (error) {
      this.chatErrorMessage.set(this.readErrorMessage(error, 'Impossible d\'envoyer le message.'));
    }
  }

  isSentByMe(message: ChatMessage): boolean {
    const receiver = this.chatReceiverId();
    if (!receiver) {
      return false;
    }
    return message.senderId !== receiver;
  }

  private loadDeliveries(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.deliveryApi
      .getMyDeliveries()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (deliveries) => {
          this.deliveries.set(deliveries ?? []);
          this.loadCouriersForDeliveries(deliveries ?? []);
          this.loading.set(false);
        },
        error: () => {
          this.deliveries.set([]);
          this.errorMessage.set('Impossible de charger vos livraisons pour le moment.');
          this.loading.set(false);
        }
      });
  }

  private getCourierId(delivery: DeliveryResource): number | null {
    const directId = delivery.courier?.id ?? delivery.vehicule?.courier?.id;
    if (typeof directId === 'number') return directId;

    const vehicle = delivery.vehicule as unknown as { courierId?: unknown; courier_id?: unknown; courierUserId?: unknown } | undefined;
    const candidates = [vehicle?.courierId, vehicle?.courier_id, vehicle?.courierUserId];
    for (const candidate of candidates) {
      const asNumber = typeof candidate === 'string' ? Number(candidate) : candidate;
      if (typeof asNumber === 'number' && Number.isFinite(asNumber)) {
        return asNumber;
      }
    }

    const vehiculeId = this.getVehiculeId(delivery);
    if (vehiculeId != null) {
      const contact = this.courierContactsByVehiculeId().get(vehiculeId);
      if (contact?.courierId != null) {
        return contact.courierId;
      }
    }

    return null;
  }

  private readCourierDisplayName(courier: unknown): string {
    if (!courier || typeof courier !== 'object') return '';

    const maybe = courier as {
      name?: string;
      nom?: string;
      prenom?: string;
      firstName?: string;
      lastName?: string;
      user?: { name?: string };
    };

    const name = maybe.name?.trim();
    if (name) return name;

    const fullFr = `${maybe.nom ?? ''} ${maybe.prenom ?? ''}`.trim();
    if (fullFr) return fullFr;

    const fullEn = `${maybe.firstName ?? ''} ${maybe.lastName ?? ''}`.trim();
    if (fullEn) return fullEn;

    const userName = maybe.user?.name?.trim();
    if (userName) return userName;

    return '';
  }

  private loadCouriersForDeliveries(deliveries: DeliveryResource[]): void {
    const existing = this.couriersById();
    const courierIds = new Set<number>();
    const vehiculeIdsNeedingContact = new Set<number>();

    for (const delivery of deliveries) {
      const courierId = this.getCourierId(delivery);
      if (courierId == null) {
        const vehiculeId = this.getVehiculeId(delivery);
        if (vehiculeId != null && !this.courierContactsByVehiculeId().has(vehiculeId)) {
          vehiculeIdsNeedingContact.add(vehiculeId);
        }
        continue;
      }

      const hasNameInline = !!(delivery.courier?.name?.trim() || delivery.vehicule?.courier?.name?.trim());
      if (hasNameInline) {
        continue;
      }

      if (!existing.has(courierId)) {
        courierIds.add(courierId);
      }
    }

    const idsToFetch = Array.from(courierIds);
    if (idsToFetch.length === 0) {
      this.loadCourierContactsByVehiculeIds(Array.from(vehiculeIdsNeedingContact));
      return;
    }

    forkJoin(
      idsToFetch.map((id) =>
        this.deliveryApi.couriers.getById(id).pipe(
          catchError(() => of(null))
        )
      )
    )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((couriers) => {
        const nextMap = new Map(this.couriersById());
        for (const courier of couriers) {
          if (courier && typeof courier.id === 'number') {
            nextMap.set(courier.id, courier);
          }
        }
        this.couriersById.set(nextMap);
        this.loadCourierContactsByVehiculeIds(Array.from(vehiculeIdsNeedingContact));
      });
  }

  private loadCourierContactsByVehiculeIds(vehiculeIds: number[]): void {
    if (vehiculeIds.length === 0) {
      return;
    }

    forkJoin(
      vehiculeIds.map((vehiculeId) =>
        this.deliveryApi.getCourierByVehiculeId(vehiculeId).pipe(
          catchError(() => of(null))
        )
      )
    )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((contacts) => {
        const nextMap = new Map(this.courierContactsByVehiculeId());
        contacts.forEach((contact, index) => {
          if (!contact) {
            return;
          }

          const vehiculeId = vehiculeIds[index];
          const name = (contact.name ?? '').trim();
          const phoneNumber = (contact.phoneNumber ?? '').trim();
          const courierId = typeof contact.courierId === 'number' ? contact.courierId : null;
          nextMap.set(vehiculeId, { courierId, name, phoneNumber });
        });
        this.courierContactsByVehiculeId.set(nextMap);
      });
  }

  private getVehiculeId(delivery: DeliveryResource): number | null {
    const rawId = (delivery.vehicule as { id?: unknown } | null | undefined)?.id;
    const vehiculeId = typeof rawId === 'string' ? Number(rawId) : rawId;
    if (typeof vehiculeId === 'number' && Number.isFinite(vehiculeId)) {
      return vehiculeId;
    }

    return null;
  }

  private async openChatAsync(delivery: DeliveryResource): Promise<void> {
    this.chatErrorMessage.set(null);
    this.chatMessages.set([]);

    const deliveryId = typeof delivery.id === 'number' ? delivery.id : Number(delivery.id);
    this.chatOpenDeliveryId.set(Number.isFinite(deliveryId) ? deliveryId : null);

    const receiverId = this.getCourierChatReceiverId(delivery);
    if (!receiverId) {
      this.chatReceiverId.set(null);
      this.chatErrorMessage.set('Impossible de déterminer l\'identifiant du livreur pour la messagerie.');
      return;
    }

    this.chatReceiverId.set(receiverId);
    this.chatLoading.set(true);

    try {
      await this.chatService.ensureConnected();
    } catch (error) {
      this.chatLoading.set(false);
      this.chatErrorMessage.set(this.readErrorMessage(error, 'Impossible de se connecter à la messagerie.'));
      return;
    }

    const openReceiverId = receiverId;
    this.chatService
      .getConversation(receiverId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (messages) => {
          if (this.chatReceiverId() !== openReceiverId) {
            return;
          }
          this.chatMessages.set(messages);
          this.chatLoading.set(false);
        },
        error: () => {
          if (this.chatReceiverId() !== openReceiverId) {
            return;
          }
          this.chatLoading.set(false);
          this.chatErrorMessage.set('Impossible de charger l\'historique du chat.');
        }
      });
  }

  private getCourierChatReceiverId(delivery: DeliveryResource): string | null {
    const courier = delivery.courier ?? delivery.vehicule?.courier;
    const fromCourier = this.readReceiverIdFromUnknown(courier);
    if (fromCourier) {
      return fromCourier;
    }

    const courierId = this.getCourierId(delivery);
    if (courierId != null) {
      return String(courierId);
    }

    return null;
  }

  private readReceiverIdFromUnknown(value: unknown): string {
    if (!value || typeof value !== 'object') {
      return '';
    }

    const maybe = value as {
      email?: string;
      username?: string;
      userId?: unknown;
      user_id?: unknown;
      user?: { email?: string; username?: string; id?: unknown };
    };

    const email = maybe.email?.trim() || maybe.user?.email?.trim();
    if (email) {
      return email;
    }

    const username = maybe.username?.trim() || maybe.user?.username?.trim();
    if (username) {
      return username;
    }

    const candidates = [maybe.userId, maybe.user_id, maybe.user?.id];
    for (const candidate of candidates) {
      const asNumber = typeof candidate === 'string' ? Number(candidate) : candidate;
      if (typeof asNumber === 'number' && Number.isFinite(asNumber)) {
        return String(asNumber);
      }
    }

    return '';
  }

  private isMessageInOpenConversation(message: ChatMessage): boolean {
    const receiverId = this.chatReceiverId();
    if (!receiverId) {
      return false;
    }

    return message.senderId === receiverId || message.receiverId === receiverId;
  }

  private buildLocalMessage(receiverId: string, content: string): ChatMessage {
    const me = this.authService.currentUser()?.email?.trim() || 'me';
    const sentAt = new Date().toISOString();
    const randomPart = String(Math.random()).slice(2);

    return {
      clientId: `local-${sentAt}-${randomPart}`,
      senderId: me,
      receiverId,
      content,
      sentAt,
      read: false
    };
  }

  private readErrorMessage(error: unknown, fallback: string): string {
    if (!error) {
      return fallback;
    }

    if (error instanceof Error) {
      return error.message || fallback;
    }

    if (typeof error === 'string') {
      return error;
    }

    return fallback;
  }

  private getDeliveryTone(delivery: DeliveryResource): DeliveryTone {
    const rawId = typeof delivery.id === 'number' ? delivery.id : Number(delivery.id);
    const index = Number.isFinite(rawId) ? Math.abs(rawId) % this.deliveryTones.length : 0;
    return this.deliveryTones[index];
  }

  canShowLiveLocation(delivery: DeliveryResource): boolean {
    const status = (delivery.status || '').toLowerCase();
    return status === 'in progress' || status === 'in_progress' || status === 'shipped';
  }

  isLiveLocationMapShown(delivery: DeliveryResource): boolean {
    const deliveryId = typeof delivery.id === 'number' ? delivery.id : Number(delivery.id);
    return Number.isFinite(deliveryId) && this.showMapForDeliveryId() === deliveryId;
  }

  toggleLiveLocationMap(delivery: DeliveryResource): void {
    const deliveryId = typeof delivery.id === 'number' ? delivery.id : Number(delivery.id);
    if (!Number.isFinite(deliveryId)) {
      return;
    }

    if (this.showMapForDeliveryId() === deliveryId) {
      this.showMapForDeliveryId.set(null);
      return;
    }

    this.showMapForDeliveryId.set(deliveryId);
  }

  private buildMapUrl(currentLocation?: string, fallbackAddress?: string): SafeResourceUrl {
    const locationQuery = (currentLocation || fallbackAddress || 'Tunis, Tunisia').trim();
    const embedUrl = `https://maps.google.com/maps?q=${encodeURIComponent(locationQuery)}&output=embed`;
    return this.sanitizer.bypassSecurityTrustResourceUrl(embedUrl);
  }

  getLiveLocationMapUrl(delivery: DeliveryResource): SafeResourceUrl {
    return this.buildMapUrl(delivery.tracking?.currentLocation, delivery.deliveryAddress || delivery.address || delivery.order?.shippingAddress);
  }
}
