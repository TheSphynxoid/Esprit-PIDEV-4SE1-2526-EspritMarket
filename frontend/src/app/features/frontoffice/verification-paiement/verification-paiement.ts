import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HeaderComponent } from '../../../shared/layout/header.component';
import { FooterComponent } from '../../../shared/layout/footer.component';
import { DeliveryApiService, DeliveryWritePayload } from '../../../services';

interface CheckoutState {
  orderId: number;
  addressDetailsId?: number;
  distanceKm?: number;
  realWeight?: number;
  length?: number;
  width?: number;
  height?: number;
  deliveryAddress: string;
  city: string;
  postalCode: string;
  phoneNumber: string;
  deliveryType?: 'STANDARD' | 'EXPRESS' | 'SAME_DAY';
  deliveryMode: string;
  shippingPrice?: number;
  cartItems?: Array<{ name: string; price: number }>;
  cartTotal?: number;
}

@Component({
  selector: 'app-verification-paiement',
  standalone: true,
  imports: [CommonModule, FormsModule, HeaderComponent, FooterComponent],
  templateUrl: './verification-paiement.html',
  styleUrl: './verification-paiement.css'
})
export class VerificationPaiementComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly deliveryApi = inject(DeliveryApiService);
  private readonly fiscalStampFee = 1;
  private readonly costPerKm = 0.5;
  private readonly weightRate = 0.4;
  private readonly minimumPrice = 1;
  private readonly deliveryTypeCoefficients: Record<'STANDARD' | 'EXPRESS' | 'SAME_DAY', number> = {
    STANDARD: 1,
    EXPRESS: 1.3,
    SAME_DAY: 1.6
  };

  orderItemsExpanded: boolean = false;
  selectedPaymentMethod: string = '';

  submitting = false;
  checkoutState: CheckoutState | null = null;

  ngOnInit(): void {
    const state = history.state as Partial<CheckoutState>;
    if (typeof state.orderId === 'number') {
      this.checkoutState = {
        orderId: state.orderId,
        addressDetailsId: state.addressDetailsId,
        distanceKm: typeof state.distanceKm === 'number' ? state.distanceKm : 0,
        realWeight: typeof state.realWeight === 'number' ? state.realWeight : 0,
        length: typeof state.length === 'number' ? state.length : 0,
        width: typeof state.width === 'number' ? state.width : 0,
        height: typeof state.height === 'number' ? state.height : 0,
        deliveryAddress: state.deliveryAddress || '',
        city: state.city || '',
        postalCode: state.postalCode || '',
        phoneNumber: state.phoneNumber || '',
        deliveryType: state.deliveryType || 'STANDARD',
        deliveryMode: state.deliveryMode || 'LIVRAISON_A_DOMICILE',
        shippingPrice: state.shippingPrice || 0,
        cartItems: state.cartItems || [],
        cartTotal: state.cartTotal || 0
      };
    }
  }

  toggleOrderItems(): void {
    this.orderItemsExpanded = !this.orderItemsExpanded;
  }

  selectPaymentMethod(method: string): void {
    this.selectedPaymentMethod = method;
  }

  goBack(): void {
    const orderId = this.checkoutState?.orderId;
    this.router.navigate(['/confirmation-de-delivery'], {
      queryParams: orderId ? { orderId } : undefined
    });
  }

  validateOrder(): void {
    if (this.submitting) {
      return;
    }

    if (!this.checkoutState) {
      alert('Informations de livraison manquantes. Retournez à létape précédente.');
      return;
    }

    if (!this.selectedPaymentMethod) {
      alert('Veuillez sélectionner un mode de paiement');
      return;
    }

    const checkoutState = this.checkoutState;
    const orderId = checkoutState.orderId;
    this.submitting = true;

    this.deliveryApi.getMyDeliveries().subscribe({
      next: (deliveries) => {
        const existingDelivery = deliveries.find((delivery) => {
          const deliveryOrderId = delivery.order?.id ?? (delivery as any).orderId;
          return Number(deliveryOrderId) === Number(orderId);
        });

        if (existingDelivery) {
          this.submitting = false;
          alert('Une livraison existe déjà pour cette commande. Redirection vers vos livraisons.');
          this.router.navigate(['/my-deliveries']);
          return;
        }

        const payload: DeliveryWritePayload = {
          deliverytype: checkoutState.deliveryType || 'STANDARD',
          status: 'Pending',
          deliverydate: new Date().toISOString().slice(0, 10),
          addressDetailsId: checkoutState.addressDetailsId,
          address: `${checkoutState.deliveryAddress}, ${checkoutState.city} ${checkoutState.postalCode}`.trim(),
          deliveryAddress: checkoutState.deliveryAddress,
          city: checkoutState.city,
          postalCode: checkoutState.postalCode,
          phoneNumber: checkoutState.phoneNumber,
          distanceKm: this.sanitizedDistanceKm,
          deliveryMode: this.normalizedDeliveryMode,
          paymentMode: this.toBackendPaymentMode(this.selectedPaymentMethod),
          orderId,
          vehiculeId: null,
          realWeight: this.sanitizedRealWeight,
          length: this.sanitizedLength,
          width: this.sanitizedWidth,
          height: this.sanitizedHeight
        };

        this.deliveryApi.deliveries.create(payload).subscribe({
          next: (createdDelivery) => {
            this.submitting = false;
            const deliveryId = Number((createdDelivery as any).id);
            if (Number.isFinite(deliveryId)) {
              this.deliveryApi.getInvoice(deliveryId).subscribe({
                next: (blob) => {
                  const url = window.URL.createObjectURL(blob);
                  const a = document.createElement('a');
                  a.href = url;
                  a.download = `invoice_${deliveryId}.pdf`;
                  document.body.appendChild(a);
                  a.click();
                  a.remove();
                  window.URL.revokeObjectURL(url);
                  alert('Livraison créée et facture téléchargée.');
                  this.router.navigate(['/my-deliveries']);
                },
                error: () => {
                  alert('Livraison créée mais impossible de télécharger la facture. Vous pouvez la récupérer depuis votre espace.');
                  this.router.navigate(['/my-deliveries']);
                }
              });
            } else {
              alert('Livraison créée avec succès.');
              this.router.navigate(['/my-deliveries']);
            }
          },
          error: (error) => {
            this.submitting = false;
            const backendMessage = String(error?.error?.message || error?.error || '').toLowerCase();
            if (backendMessage.includes('already exists') || backendMessage.includes('a delivery already exists')) {
              alert('Une livraison existe déjà pour cette commande. Redirection vers vos livraisons.');
              this.router.navigate(['/my-deliveries']);
              return;
            }

            alert('Échec de création de la livraison. Vérifiez les champs puis réessayez.');
          }
        });
      },
      error: () => {
        this.submitting = false;
        alert('Impossible de vérifier les livraisons existantes. Réessayez.');
      }
    });
  }

  get deliveryAddressSummary(): string {
    if (!this.checkoutState) {
      return 'Adresse indisponible';
    }

    return `${this.checkoutState.deliveryAddress}, ${this.checkoutState.city}, ${this.checkoutState.postalCode}`;
  }

  get checkoutTotal(): number {
    if (!this.checkoutState) {
      return 0;
    }

    return this.subtotal + this.shippingFee;
  }

  get subtotal(): number {
    return this.checkoutState?.cartTotal || 0;
  }

  get shippingFee(): number {
    if (!this.isHomeDeliveryMode) {
      return 0;
    }

    const basePrice = this.fiscalStampFee + (this.costPerKm * this.sanitizedDistanceKm);
    const chargeableWeight = this.chargeableWeight;
    const coefficient = this.deliveryTypeCoefficients[this.normalizedDeliveryType] ?? 1;
    const finalPrice = (basePrice + (chargeableWeight * this.weightRate)) * coefficient;
    return Math.max(finalPrice, this.minimumPrice);
  }

  get deliveryDistanceKm(): number {
    return this.sanitizedDistanceKm;
  }

  private get isHomeDeliveryMode(): boolean {
    return this.normalizedDeliveryMode === 'LIVRAISON_A_DOMICILE';
  }

  get normalizedDeliveryMode(): string {
    const deliveryMode = (this.checkoutState?.deliveryMode || '').trim();

    if (deliveryMode.toLowerCase() === 'livraison a domicile') {
      return 'LIVRAISON_A_DOMICILE';
    }

    if (deliveryMode.toLowerCase() === 'retrait-a-esprit') {
      return 'RETRAIT_A_ESPRIT';
    }

    return deliveryMode || 'LIVRAISON_A_DOMICILE';
  }

  get deliveryModeLabel(): string {
    return this.isHomeDeliveryMode ? 'Livraison à domicile' : 'Retrait-a-esprit';
  }

  get deliveryTypeCoefficient(): number {
    return this.deliveryTypeCoefficients[this.normalizedDeliveryType] ?? 1;
  }

  get normalizedDeliveryType(): 'STANDARD' | 'EXPRESS' | 'SAME_DAY' {
    const deliveryType = (this.checkoutState?.deliveryType || 'STANDARD').toUpperCase();
    if (deliveryType === 'EXPRESS' || deliveryType === 'SAME_DAY') {
      return deliveryType;
    }
    return 'STANDARD';
  }

  private get sanitizedDistanceKm(): number {
    const distanceKm = this.checkoutState?.distanceKm;
    return typeof distanceKm === 'number' && Number.isFinite(distanceKm) && distanceKm > 0
      ? distanceKm
      : 0;
  }

  private get sanitizedRealWeight(): number {
    return this.sanitizeNumber(this.checkoutState?.realWeight);
  }

  private get sanitizedLength(): number {
    return this.sanitizeNumber(this.checkoutState?.length);
  }

  private get sanitizedWidth(): number {
    return this.sanitizeNumber(this.checkoutState?.width);
  }

  private get sanitizedHeight(): number {
    return this.sanitizeNumber(this.checkoutState?.height);
  }

  private get volume(): number {
    return this.sanitizedLength * this.sanitizedWidth * this.sanitizedHeight;
  }

  private get volumetricWeight(): number {
    return this.volume > 0 ? this.volume / 5000 : 0;
  }

  get chargeableWeight(): number {
    return Math.max(this.sanitizedRealWeight, this.volumetricWeight);
  }

  private sanitizeNumber(value: number | undefined): number {
    return typeof value === 'number' && Number.isFinite(value) && value > 0 ? value : 0;
  }

  private toBackendPaymentMode(method: string): string {
    switch (method) {
      case 'check':
        return 'Cheque';
      case 'cash':
        return 'Paiement en espece a la livraison';
      case 'card':
        return 'Paiement par carte bancaire';
      default:
        return 'Cheque';
    }
  }
}
