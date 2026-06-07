import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { loadStripe, Stripe } from '@stripe/stripe-js';
import { environment } from '../../../environments/environment';
import { catchError } from 'rxjs/operators';

interface PaymentIntentResponse {
  clientSecret: string;
  paymentIntentId: string;
}

interface CreateOrderPayload {
  orderNumber: string;
  total: number;
  itemCount: number;
  buyer: string;
  seller: string;
  shippingAddress: string;
  deliveryZone: string;
  currency: string;
}

@Injectable({
  providedIn: 'root'
})
export class StripePaymentService {
  private readonly httpClient = inject(HttpClient);
  private stripe: Stripe | null = null;
  private readonly STRIPE_PUBLIC_KEY = String((environment as any).stripePublishableKey || '').trim();
  private configurationError = '';

  constructor() {
    this.initStripe();
  }

  private async initStripe(): Promise<void> {
    if (!this.STRIPE_PUBLIC_KEY || this.isPlaceholderKey(this.STRIPE_PUBLIC_KEY)) {
      this.configurationError = 'Stripe API key is missing or still using placeholder value. Configure environment.stripePublishableKey.';
      console.warn(this.configurationError);
      this.stripe = null;
      return;
    }

    this.configurationError = '';
    this.stripe = await loadStripe(this.STRIPE_PUBLIC_KEY);
  }

  private isPlaceholderKey(value: string): boolean {
    const normalized = value.toLowerCase();
    return normalized.includes('your_')
      || normalized.includes('placeholder')
      || normalized.includes('replace_with_real_key')
      || normalized === 'pk_test_51234567890';
  }

  hasValidPublishableKey(): boolean {
    return !!this.STRIPE_PUBLIC_KEY && !this.isPlaceholderKey(this.STRIPE_PUBLIC_KEY);
  }

  getConfigurationError(): string {
    return this.configurationError;
  }

  /**
   * Crée un PaymentIntent côté backend
   */
  createPaymentIntent(orderData: CreateOrderPayload): Observable<PaymentIntentResponse> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    const apiUrl = (environment as any).apiUrl || (environment as any).apiBaseUrl;
    const normalizedCurrency = String(orderData.currency || 'TND').toUpperCase();
    const total = Number(orderData.total);
    const amountInMinorUnit = this.toMinorUnitAmount(total, normalizedCurrency);

    // Contract used by current backend documentation.
    const payload = {
      orderNumber: orderData.orderNumber,
      total,
      itemCount: Number(orderData.itemCount),
      buyer: orderData.buyer,
      seller: orderData.seller,
      shippingAddress: orderData.shippingAddress,
      deliveryZone: orderData.deliveryZone,
      currency: normalizedCurrency,
      amount: amountInMinorUnit
    };

    // Legacy contract kept as fallback for older backend implementations.
    const legacyPayload = {
      amount: amountInMinorUnit,
      total,
      orderId: null,
      userId: null,
      orderNumber: orderData.orderNumber,
      itemCount: Number(orderData.itemCount),
      customerEmail: orderData.buyer,
      buyer: orderData.buyer,
      seller: orderData.seller,
      shippingAddress: orderData.shippingAddress,
      deliveryZone: orderData.deliveryZone,
      currency: normalizedCurrency.toLowerCase(),
      description: `Order ${orderData.orderNumber} - ${orderData.itemCount} items`,
      paymentMethod: 'STRIPE'
    };

    const modernEndpoint = `${apiUrl}/api/marketplace/orders/stripe/create-payment-intent`;
    const legacyEndpoint = `${apiUrl}/api/payments/create-intent`;

    return this.httpClient.post<PaymentIntentResponse>(modernEndpoint, payload, { headers }).pipe(
      catchError(() =>
        this.httpClient.post<PaymentIntentResponse>(modernEndpoint, legacyPayload, { headers }).pipe(
          catchError(() =>
            this.httpClient.post<PaymentIntentResponse>(legacyEndpoint, payload, { headers }).pipe(
              catchError(() =>
                this.httpClient.post<PaymentIntentResponse>(legacyEndpoint, legacyPayload, { headers })
              )
            )
          )
        )
      )
    );
  }

  private toMinorUnitAmount(total: number, currency: string): number {
    const safeTotal = Number.isFinite(total) ? total : 0;
    // Stripe uses 3 decimal minor units for TND.
    const factor = currency.toUpperCase() === 'TND' ? 1000 : 100;
    return Math.max(0, Math.round(safeTotal * factor));
  }

  /**
   * Confirme le paiement avec Stripe
   */
  async confirmPayment(
    clientSecret: string,
    cardElement: any
  ): Promise<{ error?: any; paymentIntent?: any }> {
    if (!this.stripe) {
      throw new Error('Stripe non initialisé');
    }

    return this.stripe.confirmCardPayment(clientSecret, {
      payment_method: {
        card: cardElement,
        billing_details: {}
      }
    });
  }

  /**
   * Récupère une instance de Stripe
   */
  getStripe(): Promise<Stripe | null> {
    return Promise.resolve(this.stripe);
  }

  /**
   * Crée les éléments de paiement Stripe
   */
  async createElements(appearance: any = {}, cardOptions: any = {}) {
    if (!this.stripe) {
      throw new Error('Stripe non initialisé');
    }

    const elements = this.stripe.elements({ appearance });
    const cardElement = elements.create('card', cardOptions);
    return { elements, cardElement };
  }

  async createSplitCardElements(appearance: any = {}, commonOptions: any = {}, cardNumberOptions: any = {}) {
    if (!this.stripe) {
      throw new Error('Stripe non initialisé');
    }

    const elements = this.stripe.elements({ appearance });
    const cardNumberElement = elements.create('cardNumber', {
      ...commonOptions,
      ...cardNumberOptions
    });
    const cardExpiryElement = elements.create('cardExpiry', commonOptions);
    const cardCvcElement = elements.create('cardCvc', commonOptions);

    return {
      elements,
      cardNumberElement,
      cardExpiryElement,
      cardCvcElement
    };
  }

  /**
   * Traite un paiement complet
   */
  async processPayment(
    clientSecret: string,
    cardElement: any
  ): Promise<{ success: boolean; error?: string; paymentIntentId?: string }> {
    try {
      const result = await this.confirmPayment(clientSecret, cardElement);

      if (result.error) {
        return {
          success: false,
          error: result.error.message
        };
      }

      if (result.paymentIntent?.status === 'succeeded') {
        return {
          success: true,
          paymentIntentId: result.paymentIntent.id
        };
      }

      return {
        success: false,
        error: 'Le paiement n\'a pas pu être complété'
      };
    } catch (error: any) {
      return {
        success: false,
        error: error.message || 'Erreur lors du traitement du paiement'
      };
    }
  }
}
