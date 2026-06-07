import { ChangeDetectorRef, Component, NgZone, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HeaderComponent } from '../../../shared/layout/header.component';
import { FooterComponent } from '../../../shared/layout/footer.component';
import { DeliveryApiService, MarketplaceApiService, OrderResource } from '../../../services';

interface DeliveryOption {
  id: string;
  backendValue: string;
  title: string;
  description: string;
  location?: string;
}

interface DeliveryTypeOption {
  id: string;
  value: 'STANDARD' | 'EXPRESS' | 'SAME_DAY';
  title: string;
  description: string;
}

interface Address {
  id: number;
  deliveryAddress: string;
  city: string;
  postalCode: string;
  phoneNumber: string;
}

interface OrderItem {
  name: string;
  price: number;
}

type OrderLineResource = NonNullable<OrderResource['orderLines']>[number];

interface PackageMetrics {
  realWeight: number;
  length: number;
  width: number;
  height: number;
}

@Component({
  selector: 'app-confirmation-delivery',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, HeaderComponent, FooterComponent],
  templateUrl: './confirmation_de_delivery.html',
  styleUrl: './confirmation_de_delivery.css'
})
export class ConfirmationDeDeliveryComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly marketplaceApi = inject(MarketplaceApiService);
  private readonly deliveryApi = inject(DeliveryApiService);
  private readonly ngZone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly maxAddressesPerUser = 2;
  private readonly postalCodePattern = /^[0-9]{4}$/;
  private readonly phoneNumberPattern = /^(\+216)?[0-9]{8}$/;
  private readonly fiscalStampFee = 1;
  private readonly costPerKm = 0.5;
  private readonly weightRate = 0.4;
  private readonly minimumPrice = 1;
  private readonly deliveryTypeCoefficients: Record<DeliveryTypeOption['value'], number> = {
    STANDARD: 1,
    EXPRESS: 1.3,
    SAME_DAY: 1.6
  };
  private readonly minDistanceRefreshMeters = 35;
  private readonly earthRadiusMeters = 6371000;
  private geoWatchId: number | null = null;
  private lastDistanceRefreshFrom: google.maps.LatLngLiteral | null = null;

  orderId: number | null = null;
  selectedDeliveryOption: string = 'delivery1';
  selectedDeliveryType: DeliveryTypeOption['value'] = 'STANDARD';
  expandedOrderSummary: boolean = false;
  showNewAddressForm: boolean = false;
  addressesLoading = false;
  selectedAddressId: number | null = null;
  editingAddressId: number | null = null;
  addressFormError = '';
  checkoutError = '';
  isNewAddressSubmitted = false;
  realWeight = 0;
  length = 0;
  width = 0;
  height = 0;

  addresses: Address[] = [];

  newAddressForm: FormGroup = this.fb.group({
    deliveryAddress: ['', [Validators.required]],
    city: ['', [Validators.required]],
    postalCode: ['', [Validators.required, Validators.pattern(/^[0-9]{4}$/)]],
    phoneNumber: ['', [Validators.required, Validators.pattern(/^(\+216)?[0-9]{8}$/)]]
  });

  editingAddress: Omit<Address, 'id'> = {
    deliveryAddress: '',
    city: '',
    postalCode: '',
    phoneNumber: ''
  };

  deliveryOptions: DeliveryOption[] = [
    {
      id: 'delivery1',
      backendValue: 'LIVRAISON_A_DOMICILE',
      title: 'Livraison à domicile',
      description: 'Coût = (1.000 + 0.500 × distanceKm + poids facturable × 0.4) × coefficient',
      location: ''
    },
    {
      id: 'delivery2',
      backendValue: 'RETRAIT_A_ESPRIT',
      title: 'Retrait-a-esprit',
      description: 'Retrait au point Esprit',
      location: ''
    }
  ];

  deliveryTypes: DeliveryTypeOption[] = [
    {
      id: 'type-standard',
      value: 'STANDARD',
      title: 'STANDARD',
      description: 'Standard delivery (2-5 days)'
    },
    {
      id: 'type-express',
      value: 'EXPRESS',
      title: 'EXPRESS',
      description: 'Express delivery (24-48 hours)'
    },
    {
      id: 'type-same-day',
      value: 'SAME_DAY',
      title: 'SAME_DAY',
      description: 'Same-day delivery'
    }
  ];

  cartItems: OrderItem[] = [];
  cartTotal = 0;
  selectedDistanceKm = 0;
  shippingAddressFallback = '';
  packageMetrics: PackageMetrics = {
    realWeight: 0,
    length: 0,
    width: 0,
    height: 0
  };
  mapCenter: google.maps.LatLngLiteral = { lat: 36.8065, lng: 10.1815 };
  mapZoom = 13;
  userMarkerPosition: google.maps.LatLngLiteral | null = null;
  destinationMarkerPosition: google.maps.LatLngLiteral | null = null;
  isDistanceLoading = false;
  distanceInfoMessage = 'En attente de geolocalisation...';

  ngOnInit(): void {
    this.orderId = Number(this.route.snapshot.queryParamMap.get('orderId')) || null;
    this.loadAddresses();
    this.startRealtimeDistanceTracking();

    if (this.orderId) {
      this.loadOrder(this.orderId);
    }
  }

  ngOnDestroy(): void {
    if (this.geoWatchId !== null && typeof navigator !== 'undefined' && navigator.geolocation) {
      navigator.geolocation.clearWatch(this.geoWatchId);
      this.geoWatchId = null;
    }
  }

  toggleOrderSummary(): void {
    this.expandedOrderSummary = !this.expandedOrderSummary;
  }

  selectDeliveryOption(optionId: string): void {
    this.selectedDeliveryOption = optionId;
    if (!this.isHomeDeliverySelected) {
      this.selectedDistanceKm = 0;
      this.distanceInfoMessage = 'Retrait selectionne: distance facturable = 0 km.';
      return;
    }

    this.refreshDistanceFromMap();
  }

  selectDeliveryType(type: DeliveryTypeOption['value']): void {
    this.selectedDeliveryType = type;
  }

  selectAddress(addressId: number): void {
    this.selectedAddressId = addressId;
    this.refreshDistanceFromMap();
  }

  get nf() {
    return this.newAddressForm.controls;
  }

  toggleNewAddressForm(): void {
    this.addressFormError = '';
    if (this.hasReachedAddressLimit()) {
      this.addressFormError = `Maximum ${this.maxAddressesPerUser} adresses autorisees par compte.`;
      return;
    }

    if (!this.showNewAddressForm) {
      this.isNewAddressSubmitted = false;
      this.newAddressForm.reset({
        deliveryAddress: '',
        city: '',
        postalCode: '',
        phoneNumber: ''
      });
    }

    this.showNewAddressForm = !this.showNewAddressForm;
  }

  startEditAddress(address: Address): void {
    this.addressFormError = '';
    this.editingAddressId = address.id;
    this.editingAddress = {
      deliveryAddress: address.deliveryAddress,
      city: address.city,
      postalCode: address.postalCode,
      phoneNumber: address.phoneNumber
    };
    this.showNewAddressForm = false;
  }

  cancelEditAddress(): void {
    this.addressFormError = '';
    this.editingAddressId = null;
    this.editingAddress = {
      deliveryAddress: '',
      city: '',
      postalCode: '',
      phoneNumber: ''
    };
  }

  saveEditedAddress(): void {
    this.addressFormError = '';
    if (this.editingAddressId == null) {
      return;
    }

    const validationError = this.validateAddress(this.editingAddress);
    if (validationError) {
      this.addressFormError = validationError;
      return;
    }

    this.deliveryApi.addressDetails.update(this.editingAddressId, {
      deliveryAddress: this.editingAddress.deliveryAddress.trim(),
      city: this.editingAddress.city.trim(),
      postalCode: this.editingAddress.postalCode.trim(),
      phoneNumber: this.editingAddress.phoneNumber.trim()
    }).subscribe({
      next: (updatedAddress) => {
        this.addressFormError = '';
        this.editingAddressId = null;
        this.editingAddress = {
          deliveryAddress: '',
          city: '',
          postalCode: '',
          phoneNumber: ''
        };
        this.selectedAddressId = updatedAddress.id;
        this.loadAddresses(updatedAddress.id);
      },
      error: () => {
        this.addressFormError = 'Impossible de mettre à jour cette adresse. Veuillez réessayer.';
      }
    });
  }

  addNewAddress(): void {
    this.addressFormError = '';
    if (this.hasReachedAddressLimit()) {
      this.showNewAddressForm = false;
      this.addressFormError = `Maximum ${this.maxAddressesPerUser} adresses autorisees par compte.`;
      return;
    }

    this.isNewAddressSubmitted = true;
    if (this.newAddressForm.invalid) {
      this.newAddressForm.markAllAsTouched();
      return;
    }

    const formValue = this.newAddressForm.value;
    const payload = {
      deliveryAddress: String(formValue.deliveryAddress ?? '').trim(),
      city: String(formValue.city ?? '').trim(),
      postalCode: String(formValue.postalCode ?? '').trim(),
      phoneNumber: String(formValue.phoneNumber ?? '').trim()
    };

    this.deliveryApi.addressDetails.create(payload).subscribe({
      next: (createdAddress) => {
        this.addressFormError = '';
        this.isNewAddressSubmitted = false;
        this.newAddressForm.reset({
          deliveryAddress: '',
          city: '',
          postalCode: '',
          phoneNumber: ''
        });
        this.showNewAddressForm = false;
        this.loadAddresses(createdAddress.id);
      },
      error: () => {
        this.addressFormError = 'Impossible d’enregistrer cette adresse. Veuillez réessayer.';
      }
    });
  }

  hasReachedAddressLimit(): boolean {
    return this.addresses.length >= this.maxAddressesPerUser;
  }

  private validateAddress(address: Omit<Address, 'id'>): string | null {
    const deliveryAddress = address.deliveryAddress.trim();
    const city = address.city.trim();
    const postalCode = address.postalCode.trim();
    const phoneNumber = address.phoneNumber.trim();

    if (!deliveryAddress) {
      return 'Delivery address is required.';
    }

    if (!city) {
      return 'City is required.';
    }

    if (!postalCode) {
      return 'Postal code is required.';
    }

    if (!phoneNumber) {
      return 'Phone number is required.';
    }

    if (!this.postalCodePattern.test(postalCode)) {
      return 'Le code postal doit contenir exactement 4 chiffres.';
    }

    if (!this.phoneNumberPattern.test(phoneNumber)) {
      return 'Le numéro doit contenir 8 chiffres (optionnellement +216).';
    }

    return null;
  }

  proceedToPayment(): void {
    this.checkoutError = '';
    if (!this.orderId) {
      this.checkoutError = 'Commande introuvable. Retournez vers My Packages et cliquez sur Oui.';
      return;
    }

    const selectedAddress = this.addresses.find((address) => address.id === this.selectedAddressId);
    if (!selectedAddress) {
      this.checkoutError = 'Veuillez sélectionner ou ajouter une adresse de livraison.';
      return;
    }

    const selectedOption = this.deliveryOptions.find((option) => option.id === this.selectedDeliveryOption);
    if (!selectedOption) {
      this.checkoutError = 'Delivery type is required.';
      return;
    }

    this.router.navigate(['/verification-paiement'], {
      state: {
        orderId: this.orderId,
        addressDetailsId: selectedAddress.id,
        deliveryAddress: selectedAddress.deliveryAddress,
        city: selectedAddress.city,
        postalCode: selectedAddress.postalCode,
        phoneNumber: selectedAddress.phoneNumber,
        deliveryType: this.selectedDeliveryType,
        deliveryMode: selectedOption.backendValue,
        distanceKm: this.sanitizedDistanceKm,
        shippingPrice: this.deliveryFee,
        realWeight: this.realWeight,
        length: this.length,
        width: this.width,
        height: this.height,
        cartItems: this.cartItems,
        cartTotal: this.cartTotal
      }
    });
  }

  getTotalPrice(): number {
    return this.cartTotal + this.deliveryFee;
  }

  get deliveryFee(): number {
    const selectedOption = this.deliveryOptions.find((opt) => opt.id === this.selectedDeliveryOption);
    return this.getOptionPrice(selectedOption?.backendValue);
  }

  getOptionPrice(mode?: string): number {
    if (mode !== 'LIVRAISON_A_DOMICILE') {
      return 0;
    }

    const basePrice = this.fiscalStampFee + (this.costPerKm * this.sanitizedDistanceKm);
    const chargeableWeight = this.chargeableWeight;
    const deliveryTypeCoefficient = this.deliveryTypeCoefficients[this.selectedDeliveryType] ?? 1;
    const finalPrice = (basePrice + (chargeableWeight * this.weightRate)) * deliveryTypeCoefficient;

    return Math.max(finalPrice, this.minimumPrice);
  }

  get isHomeDeliverySelected(): boolean {
    const selectedOption = this.deliveryOptions.find((option) => option.id === this.selectedDeliveryOption);
    return selectedOption?.backendValue === 'LIVRAISON_A_DOMICILE';
  }

  get volume(): number {
    return this.sanitizedPositiveNumber(this.length) * this.sanitizedPositiveNumber(this.width) * this.sanitizedPositiveNumber(this.height);
  }

  get volumetricWeight(): number {
    return this.volume > 0 ? this.volume / 5000 : 0;
  }

  get chargeableWeight(): number {
    return Math.max(this.sanitizedPositiveNumber(this.realWeight), this.volumetricWeight);
  }

  private sanitizedPositiveNumber(value: number): number {
    return Number.isFinite(value) && value > 0 ? value : 0;
  }

  get sanitizedDistanceKm(): number {
    return Number.isFinite(this.selectedDistanceKm) && this.selectedDistanceKm > 0
      ? this.selectedDistanceKm
      : 0;
  }

  get canShowMap(): boolean {
    return this.hasGoogleMapsApi() && this.isHomeDeliverySelected;
  }

  private startRealtimeDistanceTracking(): void {
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      this.distanceInfoMessage = 'Geolocalisation non disponible sur cet appareil.';
      return;
    }

    this.geoWatchId = navigator.geolocation.watchPosition(
      (position) => {
        const nextPosition: google.maps.LatLngLiteral = {
          lat: position.coords.latitude,
          lng: position.coords.longitude
        };

        this.userMarkerPosition = nextPosition;
        this.mapCenter = nextPosition;

        if (!this.lastDistanceRefreshFrom) {
          this.lastDistanceRefreshFrom = nextPosition;
          this.refreshDistanceFromMap();
          return;
        }

        const movedMeters = this.getDistanceInMeters(this.lastDistanceRefreshFrom, nextPosition);
        if (movedMeters >= this.minDistanceRefreshMeters) {
          this.lastDistanceRefreshFrom = nextPosition;
          this.refreshDistanceFromMap();
        }
      },
      () => {
        this.distanceInfoMessage = 'Activez la localisation pour calculer la distance en temps reel.';
        this.cdr.detectChanges();
      },
      {
        enableHighAccuracy: true,
        maximumAge: 5000,
        timeout: 12000
      }
    );
  }

  private refreshDistanceFromMap(): void {
    if (!this.isHomeDeliverySelected) {
      this.selectedDistanceKm = 0;
      return;
    }

    const selectedAddress = this.addresses.find((address) => address.id === this.selectedAddressId);
    if (!selectedAddress) {
      this.distanceInfoMessage = 'Selectionnez une adresse pour calculer la distance.';
      return;
    }

    if (!this.userMarkerPosition) {
      this.distanceInfoMessage = 'En attente de votre position actuelle...';
      return;
    }

    if (!this.hasGoogleMapsApi()) {
      this.distanceInfoMessage = 'Google Maps non disponible pour le calcul de distance.';
      return;
    }

    this.isDistanceLoading = true;
    const destinationAddress = `${selectedAddress.deliveryAddress}, ${selectedAddress.city}, ${selectedAddress.postalCode}, Tunisia`;
    const geocoder = new google.maps.Geocoder();
    geocoder.geocode({ address: destinationAddress }, (results, status) => {
      this.ngZone.run(() => {
        const firstResult = results?.[0];
        const destinationLocation = firstResult?.geometry?.location;
        if (status !== google.maps.GeocoderStatus.OK || !destinationLocation) {
          this.isDistanceLoading = false;
          this.distanceInfoMessage = 'Adresse introuvable sur la carte. Verifiez les champs de l\'adresse.';
          this.destinationMarkerPosition = null;
          this.cdr.detectChanges();
          return;
        }

        const destinationPoint: google.maps.LatLngLiteral = {
          lat: destinationLocation.lat(),
          lng: destinationLocation.lng()
        };
        this.destinationMarkerPosition = destinationPoint;
        this.calculateDrivingDistance(this.userMarkerPosition as google.maps.LatLngLiteral, destinationPoint);
      });
    });
  }

  private calculateDrivingDistance(origin: google.maps.LatLngLiteral, destination: google.maps.LatLngLiteral): void {
    const directionsService = new google.maps.DirectionsService();
    directionsService.route(
      {
        origin,
        destination,
        travelMode: google.maps.TravelMode.DRIVING,
        provideRouteAlternatives: false,
        drivingOptions: {
          departureTime: new Date(),
          trafficModel: google.maps.TrafficModel.BEST_GUESS
        }
      },
      (result: google.maps.DirectionsResult | null, status: string) => {
        this.ngZone.run(() => {
          this.isDistanceLoading = false;
          if (status !== 'OK' || !result?.routes?.[0]?.legs?.length) {
            const fallbackKm = this.getDistanceInMeters(origin, destination) / 1000;
            this.selectedDistanceKm = Number(fallbackKm.toFixed(3));
            this.distanceInfoMessage = `Distance approximative: ${this.selectedDistanceKm.toFixed(3)} km (fallback).`;
            this.cdr.detectChanges();
            return;
          }

          const distanceMeters = result.routes[0].legs.reduce(
            (sum, leg) => sum + (leg.distance?.value ?? 0),
            0
          );
          const km = distanceMeters / 1000;
          this.selectedDistanceKm = Number(km.toFixed(3));
          this.distanceInfoMessage = `Distance route en direct: ${this.selectedDistanceKm.toFixed(3)} km.`;
          this.cdr.detectChanges();
        });
      }
    );
  }

  private hasGoogleMapsApi(): boolean {
    return typeof google !== 'undefined' && !!google.maps;
  }

  private getDistanceInMeters(from: google.maps.LatLngLiteral, to: google.maps.LatLngLiteral): number {
    const latDelta = this.toRadians(to.lat - from.lat);
    const lngDelta = this.toRadians(to.lng - from.lng);
    const fromLatRad = this.toRadians(from.lat);
    const toLatRad = this.toRadians(to.lat);

    const haversine =
      Math.sin(latDelta / 2) * Math.sin(latDelta / 2) +
      Math.cos(fromLatRad) * Math.cos(toLatRad) *
      Math.sin(lngDelta / 2) * Math.sin(lngDelta / 2);

    const arc = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    return this.earthRadiusMeters * arc;
  }

  private toRadians(value: number): number {
    return value * (Math.PI / 180);
  }

  private loadOrder(orderId: number): void {
    this.marketplaceApi.orders.getById(orderId).subscribe({
      next: (order) => this.applyOrderData(order),
      error: () => {
        this.checkoutError = 'Commande introuvable. Le lien de confirmation est invalide ou la commande a ete supprimee.';
        this.cartItems = [];
        this.cartTotal = 0;
        this.packageMetrics = {
          realWeight: 0,
          length: 0,
          width: 0,
          height: 0
        };
      }
    });
  }

  private applyOrderData(order: OrderResource): void {
    const orderLines = order.orderLines || [];
    this.cartItems = orderLines.map((line, index) => ({
      name: line.product?.name || `Article ${index + 1}`,
      price: line.subtotal ?? line.price ?? 0
    }));
    this.cartTotal = order.totalAmount || 0;
    this.shippingAddressFallback = (order.shippingAddress || '').trim();
    this.packageMetrics = this.derivePackageMetrics(orderLines);
    this.realWeight = this.packageMetrics.realWeight;
    this.length = this.packageMetrics.length;
    this.width = this.packageMetrics.width;
    this.height = this.packageMetrics.height;
  }

  get packageDimensionsLabel(): string {
    const { length, width, height } = this.packageMetrics;
    if (length <= 0 || width <= 0 || height <= 0) {
      return 'Dimensions non disponibles';
    }

    return `${length} x ${width} x ${height} cm`;
  }

  private derivePackageMetrics(orderLines: OrderLineResource[]): PackageMetrics {
    return orderLines.reduce<PackageMetrics>((metrics, line) => {
      const quantity = this.toPositiveNumber(line.quantity) || 1;
      const lineWeight = this.toPositiveNumber(line.weight ?? line.product?.weight);
      const dimensions = this.parseDimensionsLabel(line.dimensionsLabel ?? line.product?.dimensionsLabel ?? '');

      return {
        realWeight: metrics.realWeight + (lineWeight * quantity),
        length: Math.max(metrics.length, dimensions.length),
        width: Math.max(metrics.width, dimensions.width),
        height: Math.max(metrics.height, dimensions.height)
      };
    }, {
      realWeight: 0,
      length: 0,
      width: 0,
      height: 0
    });
  }

  private parseDimensionsLabel(label: string): { length: number; width: number; height: number } {
    const values = String(label || '')
      .match(/\d+(?:[.,]\d+)?/g)
      ?.map((value) => Number(value.replace(',', '.')))
      .filter((value) => Number.isFinite(value) && value > 0) ?? [];

    if (values.length >= 3) {
      return {
        length: values[0],
        width: values[1],
        height: values[2]
      };
    }

    return { length: 0, width: 0, height: 0 };
  }

  private toPositiveNumber(value: number | string | null | undefined): number {
    const numericValue = Number(value ?? 0);
    return Number.isFinite(numericValue) && numericValue > 0 ? numericValue : 0;
  }

  private loadAddresses(preferredSelectedId?: number): void {
    this.addressesLoading = true;

    this.deliveryApi.addressDetails.list().subscribe({
      next: (addressDetails) => {
        this.addresses = addressDetails.map((address) => ({
          id: address.id,
          deliveryAddress: address.deliveryAddress,
          city: address.city,
          postalCode: address.postalCode,
          phoneNumber: address.phoneNumber
        }));

        if (this.addresses.length === 0) {
          this.selectedAddressId = null;
          this.addressesLoading = false;
          return;
        }

        const preferredAddressExists = preferredSelectedId != null
          && this.addresses.some((address) => address.id === preferredSelectedId);

        this.selectedAddressId = preferredAddressExists
          ? preferredSelectedId ?? null
          : this.addresses[0].id;

        this.refreshDistanceFromMap();

        this.addressesLoading = false;
      },
      error: () => {
        this.addresses = [];
        this.selectedAddressId = null;
        this.addressesLoading = false;
      }
    });
  }
}
