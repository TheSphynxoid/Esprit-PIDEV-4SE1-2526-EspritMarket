import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import { AuthService, CartService, OrderService, ProductService } from '../../../services';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule, RouterLink, HeaderComponent, FooterComponent],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.css'
})
export class CheckoutComponent {
  private readonly cartService = inject(CartService);
  private readonly orderService = inject(OrderService);
  private readonly productService = inject(ProductService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly cartItems = computed(() => this.cartService.cartItems());
  readonly summary = computed(() => this.cartService.summary());

  isSubmitting = signal(false);
  errorMessage = signal('');

  grandTotal = computed(() =>
    Number(this.summary().total.toFixed(2))
  );

  get packageWeightLabel(): string {
    const weight = this.getCartWeight();
    return weight > 0 ? `${weight.toFixed(1)} kg` : 'N/A';
  }

  get packageDimensionsLabel(): string {
    const dimensions = this.getCartDimensions();
    if (!dimensions) {
      return 'N/A';
    }

    return `${dimensions.length} x ${dimensions.width} x ${dimensions.height} cm`;
  }

  isImageSource(value: string | undefined | null): boolean {
    if (!value) return false;
    return value.startsWith('http://') ||
           value.startsWith('https://') ||
           value.startsWith('data:image/');
  }

  getProductDimensionsLabel(product: any): string {
    const raw = String(product?.dimensionsLabel || '').trim();
    return raw && raw !== 'N/A' ? raw : 'N/A';
  }

  getProductWeightLabel(product: any): string {
    const weight = Number(product?.weight || 0);
    return weight > 0 ? `${weight} kg` : 'N/A';
  }

  private getCartWeight(): number {
    return this.cartItems().reduce((total, item) => {
      const itemWeight = Number(item?.product?.weight || 0);
      const quantity = Number(item?.quantity || 0);
      return total + (Number.isFinite(itemWeight) && itemWeight > 0 ? itemWeight * Math.max(quantity, 1) : 0);
    }, 0);
  }

  private getCartDimensions(): { length: number; width: number; height: number } | null {
    for (const item of this.cartItems()) {
      const dimensions = this.parseDimensionsLabel(item?.product?.dimensionsLabel);
      if (dimensions) {
        return dimensions;
      }
    }

    return null;
  }

  private parseDimensionsLabel(label: string | undefined | null): { length: number; width: number; height: number } | null {
    const values = String(label || '')
      .match(/\d+(?:[.,]\d+)?/g)
      ?.map((value) => Number(value.replace(',', '.')))
      .filter((value) => Number.isFinite(value) && value > 0) ?? [];

    if (values.length < 3) {
      return null;
    }

    return {
      length: values[0],
      width: values[1],
      height: values[2]
    };
  }

  increase(productId: number): void {
    this.cartService.increaseQuantity(productId);
  }

  decrease(productId: number): void {
    this.cartService.decreaseQuantity(productId);
  }

  remove(productId: number): void {
    this.cartService.removeItem(productId);
  }

  placeOrder(): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: '/checkout' } });
      return;
    }

    if (!this.cartItems().length) {
      this.errorMessage.set('Votre panier est vide');
      return;
    }

    const productsMissingId = this.cartItems().filter(
      item => !Number.isFinite(Number(item?.product?.id)) || Number(item?.product?.id) <= 0
    );

    if (productsMissingId.length > 0) {
      this.errorMessage.set('Certains produits ne sont pas encore enregistrés en base.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const now = new Date();

    const cartItemsWithIds = this.cartItems().map(item => ({
      ...item,
      product: { ...item.product }
    }));

    this.orderService.createOrder({
      orderNumber: `ORD-${Date.now()}`,
      date: now,
      status: 'pending',
      total: this.grandTotal(),
      itemCount: this.summary().itemCount,
      userId: this.authService.currentUser()?.id,
      currency: 'DT',
      buyer: this.authService.currentUser()?.email || 'Customer',
      seller: this.cartItems()[0]?.product?.seller || 'Marketplace'
    }, cartItemsWithIds).subscribe({
      next: (createdOrder) => {
        this.applyPurchasedStockReduction();
        this.cartService.clearCart();
        this.isSubmitting.set(false);

        const orderId = Number(createdOrder.id);
        this.router.navigate(['/confirmation-de-delivery'], {
          queryParams: Number.isFinite(orderId) ? { orderId } : undefined
        });
      },
      error: () => {
        this.isSubmitting.set(false);
        this.errorMessage.set('Echec enregistrement commande en base. Reessayez.');
      }
    });
  }

  private applyPurchasedStockReduction(): void {
    const purchasedItems = this.cartItems()
      .filter(item => item.product?.id && item.quantity > 0)
      .map(item => ({ productId: item.product.id, quantity: item.quantity }));
    this.productService.consumeStockFromPurchase(purchasedItems);
  }
}