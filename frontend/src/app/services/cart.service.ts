import { Injectable, effect, inject } from '@angular/core';
import { signal, computed } from '@angular/core';
import { AuthService, User } from './auth.service';
import { Product } from './product.service';

export interface CartItem {
  product: Product;
  quantity: number;
  addedAt: Date;
}

export interface CartSummary {
  itemCount: number;
  uniqueItemCount: number;
  subtotal: number;
  total: number;
  currency: string;
}

interface SavedCartData {
  productId: number;
  quantity: number;
  addedAt: string;
  productData?: {
    name: string;
    description: string;
    price: number;
    image: string;
    seller: string;
    stock: number;
    category: string;
    rating: number;
    reviews: number;
    dimensionsLabel?: string;
    weight?: number;
  };
}

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private readonly authService = inject(AuthService);
  private items = signal<CartItem[]>([]);
  private activeCartStorageKey = this.resolveCartStorageKey();

  // Computed values
  cartItems = computed(() => this.items());

  cartCount = computed(() => {
    return this.items().reduce((total, item) => total + item.quantity, 0);
  });

  uniqueItemCount = computed(() => this.items().length);

  subtotal = computed(() => {
    return this.items().reduce((total, item) => total + (item.product.price * item.quantity), 0);
  });

  total = computed(() => {
    return Math.round(this.subtotal() * 100) / 100;
  });

  summary = computed((): CartSummary => ({
    itemCount: this.cartCount(),
    uniqueItemCount: this.uniqueItemCount(),
    subtotal: this.subtotal(),
    total: this.total(),
    currency: 'DT'
  }));

  isEmpty = computed(() => this.items().length === 0);
  isNotEmpty = computed(() => this.items().length > 0);

  constructor() {
    this.loadCart();

    // Keep cart isolated per account and switch instantly on login/logout.
    effect(() => {
      const isLoggedIn = this.authService.isLoggedIn();
      const currentUser = this.authService.currentUser();
      const nextKey = this.resolveCartStorageKey(isLoggedIn ? currentUser : null);

      if (nextKey !== this.activeCartStorageKey) {
        this.activeCartStorageKey = nextKey;
        this.loadCart();
      }
    });
  }

  // Cart operations
  addItem(product: Product, quantity: number = 1): void {
    if (quantity <= 0) return;

    this.items.update(current => {
      const maxStock = this.getMaxAllowedQuantity(product);
      if (maxStock <= 0) {
        return current;
      }

      const existing = current.find(item => item.product.id === product.id);
      if (existing) {
        const nextQuantity = Math.min(maxStock, existing.quantity + quantity);
        // Update quantity and refresh product payload (dimensions/weight can change).
        return current.map(item =>
          item.product.id === product.id
            ? { ...item, product: { ...item.product, ...product }, quantity: nextQuantity }
            : item
        );
      } else {
        const safeQuantity = Math.min(maxStock, Math.max(1, Math.floor(quantity)));
        // Add new item
        return [...current, { product, quantity: safeQuantity, addedAt: new Date() }];
      }
    });

    this.saveCart();
  }

  removeItem(productId: number): void {
    this.items.update(current => current.filter(item => item.product.id !== productId));
    this.saveCart();
  }

  updateQuantity(productId: number, quantity: number): void {
    if (quantity <= 0) {
      this.removeItem(productId);
      return;
    }

    this.items.update(current => {
      const target = current.find(item => item.product.id === productId);
      if (!target) {
        return current;
      }

      const maxStock = this.getMaxAllowedQuantity(target.product);
      if (maxStock <= 0) {
        return current.filter(item => item.product.id !== productId);
      }

      const safeQuantity = Math.min(maxStock, Math.max(1, Math.floor(quantity)));
      return current.map(item =>
        item.product.id === productId
          ? { ...item, quantity: safeQuantity }
          : item
      );
    });
    this.saveCart();
  }

  increaseQuantity(productId: number): void {
    const item = this.items().find(i => i.product.id === productId);
    if (item) {
      this.updateQuantity(productId, item.quantity + 1);
    }
  }

  decreaseQuantity(productId: number): void {
    const item = this.items().find(i => i.product.id === productId);
    if (item && item.quantity > 1) {
      this.updateQuantity(productId, item.quantity - 1);
    }
  }

  clearCart(): void {
    this.items.set([]);
    this.saveCart();
  }

  enforceStockLimits(): void {
    this.items.update((current) => current
      .map((item) => {
        const maxStock = this.getMaxAllowedQuantity(item.product);
        if (maxStock <= 0) {
          return null;
        }

        const safeQuantity = Math.min(maxStock, Math.max(1, Math.floor(item.quantity || 1)));
        return { ...item, quantity: safeQuantity };
      })
      .filter((item): item is CartItem => item !== null)
    );

    this.saveCart();
  }

  getItem(productId: number): CartItem | undefined {
    return this.items().find(item => item.product.id === productId);
  }

  hasItem(productId: number): boolean {
    return this.items().some(item => item.product.id === productId);
  }

  // Persistence
  private saveCart(): void {
    const cartData = this.items().map(item => ({
      productId: item.product.id,
      quantity: item.quantity,
      addedAt: item.addedAt.toISOString(),
      // Save essential product data for restoration
      productData: {
        name: item.product.name,
        description: item.product.description,
        price: item.product.price,
        image: item.product.image,
        seller: item.product.seller,
        stock: item.product.stock,
        category: item.product.category,
        rating: item.product.rating,
        reviews: item.product.reviews,
        dimensionsLabel: item.product.dimensionsLabel,
        weight: item.product.weight
      }
    } as SavedCartData));
    sessionStorage.setItem(this.activeCartStorageKey, JSON.stringify(cartData));
  }

  private loadCart(): void {
    // Reset first to avoid showing another account's cart during account switch.
    this.items.set([]);

    const saved = sessionStorage.getItem(this.activeCartStorageKey);
    if (saved) {
      try {
        const cartData = JSON.parse(saved) as SavedCartData[];
        if (!Array.isArray(cartData)) {
          return;
        }

        const restoreItems: CartItem[] = [];

        for (const data of cartData) {
          if (!Number.isFinite(Number(data.productId))) {
            continue;
          }

          // Reconstruct product from saved data
          if (data.productData && Number.isFinite(Number(data.quantity)) && data.quantity > 0) {
            const product: Product = {
              id: data.productId,
              name: data.productData.name,
              description: data.productData.description,
              price: data.productData.price,
              image: data.productData.image,
              seller: data.productData.seller,
              stock: data.productData.stock,
              category: data.productData.category as any,
              rating: data.productData.rating,
              reviews: data.productData.reviews,
              dimensionsLabel: data.productData.dimensionsLabel,
              weight: data.productData.weight,
              currency: 'DT',
              createdAt: new Date(),
              updatedAt: new Date()
            };

            restoreItems.push({
              product,
              quantity: Math.min(Number(data.quantity), this.getMaxAllowedQuantity(product)),
              addedAt: new Date(data.addedAt)
            });
          }
        }

        if (restoreItems.length > 0) {
          this.items.set(restoreItems);
        }
      } catch (e) {
        console.error('Error loading cart from storage:', e);
        sessionStorage.removeItem(this.activeCartStorageKey);
      }
    }
  }

  private resolveCartStorageKey(user?: User | null): string {
    const resolvedUser = user ?? this.authService.currentUser();
    const userId = Number(resolvedUser?.id || 0);
    const email = String(resolvedUser?.email || '').trim().toLowerCase();

    if (Number.isFinite(userId) && userId > 0) {
      return `esprit_cart_user_${userId}`;
    }

    if (email) {
      return `esprit_cart_email_${email}`;
    }

    return 'esprit_cart_guest';
  }

  // Checkout simulation
  checkout(): Promise<{ orderId: string; totalAmount: number }> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const orderId = `ORDER-${Date.now()}`;
        resolve({
          orderId,
          totalAmount: this.total()
        });
        this.clearCart();
      }, 1500);
    });
  }

  private getMaxAllowedQuantity(product: Product): number {
    const stock = Number(product?.stock);
    if (!Number.isFinite(stock) || stock <= 0) {
      return 0;
    }

    return Math.floor(stock);
  }
}
