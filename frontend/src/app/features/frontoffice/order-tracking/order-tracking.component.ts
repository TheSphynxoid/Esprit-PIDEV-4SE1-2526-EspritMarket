import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import { OrderService, AuthService } from '../../../services';
import { parseProductDescription } from '../../../services/product-meta.util';

@Component({
  selector: 'app-order-tracking',
  standalone: true,
  imports: [CommonModule, HeaderComponent, FooterComponent],
  templateUrl: './order-tracking.component.html',
  styleUrls: []
})
export class OrderTrackingComponent {
  private readonly orderService = inject(OrderService);
  private readonly authService = inject(AuthService);

  readonly isAdmin = computed(() => {
    const userRole = this.authService.currentUser()?.role;
    return userRole === 'admin_market';
  });

  orders = computed(() => {
    const rawOrders = this.orderService.allOrders();
    console.log(`[OrderTrackingComponent] Mapping ${rawOrders.length} raw orders for display.`);
    
    return rawOrders.map((order) => {
      try {
        const products = (order.orderLines || []).map((line) => {
          const product = line.product;
          
          // Use the snapshot values from the order line, fallback to current product metadata if missing or "N/A"
          const snapDimensions = (line as any).dimensionsLabel;
          const dimensionsLabel = (snapDimensions && snapDimensions !== 'N/A' && snapDimensions !== '') 
            ? snapDimensions 
            : (product?.dimensionsLabel || 'N/A');
            
          const snapWeight = (line as any).weight;
          const weight = (snapWeight && snapWeight > 0) 
            ? snapWeight 
            : (product?.weight || 0);

          return {
            name: product?.name || 'Product',
            quantity: line.quantity,
            price: line.price,
            weight,
            dimensionsLabel
          };
        });

        // Robust date formatting to prevent .toISOString() crashes
        const formatShortDate = (date: any) => {
          if (!date) return 'N/A';
          try {
            const d = date instanceof Date ? date : new Date(date);
            if (isNaN(d.getTime())) return 'Invalid Date';
            return d.toISOString().slice(0, 10);
          } catch (e) {
            return 'Date Error';
          }
        };

        return {
          id: order.id,
          orderNumber: order.orderNumber,
          status: this.toTrackingStatus(order.status),
          items: `${order.itemCount || 0} item(s)`,
          amount: order.total || 0,
          address: order.shippingAddress || 'Delivery address',
          estimatedDate: order.estimatedDelivery 
            ? formatShortDate(order.estimatedDelivery)
            : formatShortDate(order.date),
          step: this.toStep(order.status),
          products
        };
      } catch (err) {
        console.error(`[OrderTrackingComponent] Error mapping order ${order.id}:`, err);
        return null as any; // Filtered out below or displayed as error
      }
    }).filter(o => o !== null);
  });

  constructor() {
    this.orderService.refresh();
  }

  onDelete(orderId: string): void {
    if (confirm('Are you sure you want to delete this order?')) {
      this.orderService.deleteOrder(orderId);
    }
  }

  private toTrackingStatus(status: 'pending' | 'processing' | 'shipped' | 'delivered' | 'cancelled'): string {
    if (status === 'processing') {
      return 'confirmed';
    }
    return status;
  }

  private toStep(status: 'pending' | 'processing' | 'shipped' | 'delivered' | 'cancelled'): number {
    switch (status) {
      case 'processing':
        return 1;
      case 'shipped':
        return 2;
      case 'delivered':
        return 3;
      case 'pending':
      case 'cancelled':
      default:
        return 0;
    }
  }
}

