import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import { CartService, ProductService } from '../../../services';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, HeaderComponent, FooterComponent],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.css'
})
export class ProductDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);

  productId = Number(this.route.snapshot.paramMap.get('id') || 0);
  selectedImage = signal('');
  selectedVariantIndex = signal(0);
  hoverRating = signal(0);
  quantity = signal(1);

  product = computed(() => this.productService.getProductById(this.productId));
  images = computed<string[]>(() => {
    const currentProduct = this.product();
    if (!currentProduct) {
      return [];
    }

    if (currentProduct.images && currentProduct.images.length > 0) {
      return currentProduct.images;
    }

    return [currentProduct.image || '🛍️'];
  });
  activeImage = computed(() => {
    const gallery = this.images() || [];
    return this.selectedImage() || gallery[0] || '';
  });
  selectedVariant = computed(() => {
    const variants = this.product()?.variants || [];
    if (!variants.length) {
      return null;
    }
    return variants[this.selectedVariantIndex()] ?? variants[0];
  });

  displayPrice = computed(() => {
    const base = this.product()?.price || 0;
    const delta = this.selectedVariant()?.priceDelta || 0;
    return Number((base + delta).toFixed(2));
  });

  cleanDescription = computed(() => {
    const raw = String(this.product()?.description || '').trim();
    if (!raw) {
      return '';
    }

    let cleaned = raw;
    const splitterIndex = cleaned.indexOf('---product-meta---');
    if (splitterIndex >= 0) {
      cleaned = cleaned.slice(0, splitterIndex);
    }

    const jsonMetaIndex = cleaned.indexOf('{"sku"');
    if (jsonMetaIndex > 0) {
      cleaned = cleaned.slice(0, jsonMetaIndex);
    }

    const legacyMetaIndex = cleaned.indexOf("{'sku'");
    if (legacyMetaIndex > 0) {
      cleaned = cleaned.slice(0, legacyMetaIndex);
    }

    return cleaned.trim();
  });

  stockLeft = computed(() => {
    const current = this.product();
    return Math.max(0, Number(current?.stock || 0));
  });

  estimatedDelivery = computed(() => {
    const delivery = new Date();
    delivery.setDate(delivery.getDate() + 6);
    return delivery.toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' });
  });

  visitorRating = computed(() => {
    const current = this.product();
    if (!current) {
      return 0;
    }
    return this.productService.getVisitorProductRating(current.id);
  });

  constructor() {
    this.productService.refresh();
  }

  chooseImage(url: string): void {
    this.selectedImage.set(url);
  }

  chooseVariant(index: number): void {
    this.selectedVariantIndex.set(index);
  }

  addToCart(): void {
    const product = this.product();
    if (!product) {
      return;
    }
    this.cartService.addItem(product, this.quantity());
  }

  buyNow(): void {
    const product = this.product();
    if (!product) {
      return;
    }

    this.cartService.addItem(product, this.quantity());
    this.router.navigate(['/checkout']);
  }

  decreaseQuantity(): void {
    this.quantity.update((current) => Math.max(1, current - 1));
  }

  increaseQuantity(): void {
    const max = this.stockLeft();
    this.quantity.update((current) => {
      if (max <= 0) {
        return 1;
      }
      return Math.min(max, current + 1);
    });
  }

  formatPrice(price: number): string {
    const safePrice = Number.isFinite(Number(price)) ? Number(price) : 0;
    return new Intl.NumberFormat('fr-TN', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2
    }).format(safePrice);
  }

  rateProduct(stars: number): void {
    const current = this.product();
    if (!current) {
      return;
    }
    this.productService.rateProduct(current.id, stars);
  }

  previewRating(stars: number): void {
    this.hoverRating.set(stars);
  }

  clearPreviewRating(): void {
    this.hoverRating.set(0);
  }

  isStarActive(star: number): boolean {
    const preview = this.hoverRating();
    const selected = this.visitorRating();
    const activeLevel = preview > 0 ? preview : selected;
    return star <= activeLevel;
  }
}
