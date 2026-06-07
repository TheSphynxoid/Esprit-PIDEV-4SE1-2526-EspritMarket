import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, forkJoin, of, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import {
  DeliveryApiService,
  DeliveryResource,
  MarketplaceApiService,
  OrderResource,
  OrderWritePayload
} from './api';
import { AuthService } from './auth.service';

export interface Order {
  id: string;
  orderNumber: string;
  date: Date;
  status: 'pending' | 'processing' | 'shipped' | 'delivered' | 'cancelled';
  total: number;
  itemCount: number;
  userId?: number;
  seller?: string;
  buyer?: string;
  currency: string;
  estimatedDelivery?: Date;
  shippingAddress?: string;
  deliveryZone?: string;
  orderLines?: Array<{
    quantity: number;
    price: number;
    subtotal: number;
    product?: {
      name: string;
      description: string;
      price: number;
      imageUrl?: string;
      dimensionsLabel?: string;
      weight?: number;
    };
  }>;
}

export interface OrderStatus {
  stage: 'pending' | 'processing' | 'shipped' | 'delivered';
  completedAt: Date | null;
  currentStep: number;
  totalSteps: number;
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly marketplaceApi = inject(MarketplaceApiService);
  private readonly deliveryApi = inject(DeliveryApiService);
  private readonly authService = inject(AuthService);

  private orders = signal<Order[]>([]);

  allOrders = computed(() => this.orders());
  pendingOrders = computed(() => this.orders().filter(o => o.status === 'pending'));
  shippedOrders = computed(() => this.orders().filter(o => o.status === 'shipped'));
  deliveredOrders = computed(() => this.orders().filter(o => o.status === 'delivered'));
  totalOrders = computed(() => this.orders().length);
  totalSpent = computed(() => this.orders().reduce((sum, o) => sum + o.total, 0));

  constructor() {
    this.clearLegacyOrderStorage();
    this.refresh();
  }

  refresh(): void {
    const role = this.authService.currentUser()?.role;
    const canReadDeliveries = role === 'deliverer' || role === 'admin_delivery';
    const deliveries$ = canReadDeliveries
      ? this.deliveryApi.deliveries.list().pipe(catchError(() => of([] as DeliveryResource[])))
      : of([] as DeliveryResource[]);

    forkJoin({
      orders: this.marketplaceApi.orders.list(),
      deliveries: deliveries$
    }).subscribe({
      next: ({ orders, deliveries }) => {
        const currentOrders = this.orders();
        const deliveryMap = new Map<number, DeliveryResource>();
        for (const delivery of deliveries) {
          const orderId = delivery.order?.id;
          if (orderId) deliveryMap.set(orderId, delivery);
        }

        const mappedOrders = orders.map(order =>
          this.mapOrderResource(order, deliveryMap.get(order.id))
        );

        const sortedMapped = mappedOrders.sort((a, b) => b.date.getTime() - a.date.getTime());
        const mergedOrders = this.mergeOrdersWithLocalLines(sortedMapped, currentOrders);
        const finalSorted = mergedOrders.sort((a, b) => b.date.getTime() - a.date.getTime());

        this.orders.set(finalSorted);
        this.saveOrdersToStorage(finalSorted);
      },
      error: err => console.error('Error refreshing orders:', err)
    });
  }

  // ─── Crée une commande (panier) ───────────────────────────────────────────
  createOrder(
    order: Omit<Order, 'id'>,
    cartItems?: Array<{
      product: {
        id?: number;
        name: string;
        description: string;
        price: number;
        dimensionsLabel?: string;
        weight?: number;
      };
      quantity: number;
    }>
  ): Observable<Order> {

    // Construire les orderLines depuis le panier
    const orderLines = (cartItems || []).map(item => ({
      quantity: item.quantity,
      price: item.product.price,
      subtotal: item.quantity * item.product.price,
      product: {
        name: item.product.name,
        description: item.product.description || '',
        price: item.product.price,
        imageUrl: undefined,
        // ✅ Snapshot dimension et weight au moment de la commande
        dimensionsLabel: item.product.dimensionsLabel || 'N/A',
        weight: item.product.weight || 0
      }
    }));

    const newOrder: Order = {
      ...order,
      id: `local-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      orderLines: orderLines.length > 0 ? orderLines : undefined
    };

    // Ajouter immédiatement en tête de liste
    this.orders.update(current => [newOrder, ...current]);
    this.saveOrdersToStorage(this.orders());

    // ✅ Payload sans paymentMethod
    const payload: OrderWritePayload = {
      date: newOrder.date.toISOString(),
      status: this.toServerStatus(newOrder.status),
      totalAmount: newOrder.total,
      shippingAddress: newOrder.shippingAddress || `Order for ${newOrder.buyer || 'Customer'}`,
      userId: Number(newOrder.userId || 0) > 0 ? Number(newOrder.userId) : undefined,
      orderLines: (cartItems || [])
        .filter(item => Number(item?.product?.id || 0) > 0)
        .map(item => ({
          quantity: item.quantity,
          price: item.product.price,
          productId: Number(item.product.id),
          dimensionsLabel: item.product.dimensionsLabel || 'N/A',
          weight: item.product.weight && item.product.weight > 0
            ? item.product.weight
            : undefined
        }))
    };

    return this.marketplaceApi.orders.create(payload).pipe(
      map(createdOrder => {
        const persistedOrder: Order = {
          ...newOrder,
          id: String(createdOrder.id),
          orderNumber: `ORD-${createdOrder.id}`,
          date: createdOrder.date ? new Date(createdOrder.date) : newOrder.date,
          status: this.toOrderStatus(createdOrder.status),
          total: Number(createdOrder.totalAmount ?? newOrder.total),
          itemCount: createdOrder.orderLines?.length ?? newOrder.itemCount,
          userId: createdOrder.user?.id ?? newOrder.userId,
          buyer: createdOrder.user?.email ?? newOrder.buyer,
          orderLines: orderLines.length > 0 ? orderLines : newOrder.orderLines
        };

        this.orders.update(current =>
          current.map(o => (o.id === newOrder.id ? persistedOrder : o))
        );
        this.saveOrdersToStorage(this.orders());

        // Re-sync from backend so UI matches persisted PostgreSQL data.
        this.refresh();
        return persistedOrder;
      }),
      catchError(err => {
        console.error('Erreur création commande:', err);
        // Rollback optimistic local order to avoid showing non-persisted data.
        this.orders.update(current => current.filter(o => o.id !== newOrder.id));
        this.saveOrdersToStorage(this.orders());
        return throwError(() => err);
      })
    );
  }

  updateOrderStatus(orderId: string, status: Order['status']): void {
    this.orders.update(current =>
      current.map(o => o.id === orderId ? { ...o, status } : o)
    );
    this.saveOrdersToStorage(this.orders());

    const numericId = Number(orderId);
    if (!Number.isNaN(numericId)) {
      const currentOrder = this.getOrderById(orderId);
      if (currentOrder) {
        const payload: OrderWritePayload = {
          date: currentOrder.date.toISOString(),
          status: this.toServerStatus(status),
          totalAmount: currentOrder.total,
          shippingAddress: currentOrder.shippingAddress || 'UNKNOWN'
        };
        this.marketplaceApi.orders.update(numericId, payload).subscribe({
          error: () => this.refresh()
        });
      }
    }
  }

  getOrderById(id: string): Order | undefined {
    return this.orders().find(o => o.id === id);
  }

  getOrdersByStatus(status: Order['status']): Order[] {
    return this.orders().filter(o => o.status === status);
  }

  cancelOrder(orderId: string): void {
    this.updateOrderStatus(orderId, 'cancelled');
  }

  deleteOrder(orderId: string): void {
    const numericId = Number(orderId);
    if (isNaN(numericId)) {
      this.orders.update(current => current.filter(o => o.id !== orderId));
      this.saveOrdersToStorage(this.orders());
      return;
    }

    this.marketplaceApi.orders.delete(numericId).subscribe({
      next: () => {
        this.orders.update(current => current.filter(o => o.id !== orderId));
        this.saveOrdersToStorage(this.orders());
      },
      error: err => {
        if (err.status === 404) {
          this.orders.update(current => current.filter(o => o.id !== orderId));
          this.saveOrdersToStorage(this.orders());
          return;
        }
        this.refresh();
      }
    });
  }

  getOrderTimeline(orderId: string): OrderStatus | null {
    const order = this.getOrderById(orderId);
    if (!order) return null;

    const statusMap: { [key in Order['status']]: number } = {
      pending: 0, processing: 1, shipped: 2, delivered: 3, cancelled: 0
    };

    return {
      stage: order.status === 'cancelled' ? 'pending'
        : (order.status as 'pending' | 'processing' | 'shipped' | 'delivered'),
      completedAt: order.date,
      currentStep: statusMap[order.status] || 0,
      totalSteps: 4
    };
  }

  trackOrder(orderNumber: string): Promise<Order | null> {
    return new Promise(resolve => {
      const order = this.orders().find(o => o.orderNumber === orderNumber);
      resolve(order || null);
    });
  }

  private mapOrderResource(order: OrderResource, delivery?: DeliveryResource): Order {
    const orderLines = (order.orderLines || []).map((line: any) => ({
      quantity: line.quantity,
      price: line.price,
      subtotal: line.subtotal,
      product: (line.product || line.productName) ? {
        name: line.product?.name || line.productName || 'Product',
        description: line.product?.description || '',
        price: line.product?.price || line.price,
        imageUrl: line.product?.imageUrl || '',
        // ✅ Snapshot depuis la base
        dimensionsLabel: line.dimensionsLabel || 'N/A',
        weight: line.weight || 0
      } : undefined
    }));

    return {
      id: String(order.id),
      orderNumber: `ORD-${order.id}`,
      date: new Date(order.date),
      status: this.toOrderStatus(delivery?.status ?? order.status),
      total: order.totalAmount,
      itemCount: order.orderLines?.length ?? 0,
      userId: order.user?.id,
      seller: order.orderLines?.[0]?.product?.store?.name,
      buyer: order.user?.email,
      currency: 'DT',
      estimatedDelivery: delivery?.deliverydate
        ? new Date(delivery.deliverydate) : undefined,
      orderLines
    };
  }

  private mergeOrdersWithLocalLines(remoteOrders: Order[], localOrders: Order[]): Order[] {
    const localByOrderNumber = new Map<string, Order>();
    const localById = new Map<string, Order>();
    const matchedLocalKeys = new Set<string>();

    for (const order of localOrders) {
      localByOrderNumber.set(String(order.orderNumber || ''), order);
      localById.set(String(order.id || ''), order);
    }

    const mergedRemote = remoteOrders.map(remote => {
      const local = localByOrderNumber.get(String(remote.orderNumber || ''))
                 || localById.get(String(remote.id || ''));

      if (local) {
        matchedLocalKeys.add(String(local.orderNumber || ''));
        matchedLocalKeys.add(String(local.id || ''));
      }

      const hasRemoteLines = Array.isArray(remote.orderLines) && remote.orderLines.length > 0;
      const hasLocalLines = Array.isArray(local?.orderLines) && (local?.orderLines?.length || 0) > 0;

      if (!hasRemoteLines && hasLocalLines) {
        return {
          ...remote,
          orderLines: local?.orderLines,
          itemCount: Math.max(
            Number(remote.itemCount || 0),
            Number(local?.itemCount || 0),
            Number(local?.orderLines?.length || 0)
          )
        };
      }
      return remote;
    });

    const keptLocal = localOrders.filter(local => {
      const isMatched = matchedLocalKeys.has(String(local.orderNumber || ''))
                     || matchedLocalKeys.has(String(local.id || ''));
      if (isMatched) return false;

      // Keep only optimistic local orders (non-numeric ids).
      return isNaN(Number(local.id));
    });

    return [...mergedRemote, ...keptLocal];
  }

  private toOrderStatus(rawStatus: string): Order['status'] {
    if (!rawStatus) return 'pending';
    switch (rawStatus.toLowerCase()) {
      case 'pending': return 'pending';
      case 'in progress':
      case 'shipped': return 'shipped';
      case 'delivered': return 'delivered';
      case 'processing': return 'processing';
      default: return 'pending';
    }
  }

  private toServerStatus(status: Order['status']): string {
    switch (status) {
      case 'pending': return 'PENDING';
      case 'processing': return 'PROCESSING';
      case 'shipped': return 'SHIPPED';
      case 'delivered': return 'DELIVERED';
      case 'cancelled': return 'CANCELLED';
      default: return 'PENDING';
    }
  }

  private saveOrdersToStorage(orders: Order[]): void {
    try {
      const serialized = orders.map(order => ({
        ...order,
        date: order.date instanceof Date
          ? order.date.toISOString() : new Date(order.date).toISOString(),
        estimatedDelivery: order.estimatedDelivery
          ? (order.estimatedDelivery instanceof Date
            ? order.estimatedDelivery.toISOString()
            : new Date(order.estimatedDelivery).toISOString())
          : undefined
      }));
      localStorage.setItem('esprit_orders', JSON.stringify(serialized));
    } catch (err) {
      console.error('Error saving orders to storage:', err);
    }
  }

  private clearLegacyOrderStorage(): void {
    try {
      localStorage.removeItem('esprit_orders');
    } catch {
      // Ignore storage cleanup errors.
    }
  }
}