import { Component, inject, signal, OnDestroy, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import { DeliveryApiService, DeliveryResource, MarketplaceApiService, OrderResource } from '../../../services';

declare const google: any;

interface PackageViewModel {
  orderId: number;
  id: string;
  status: 'pending' | 'confirmed' | 'shipped' | 'delivered';
  items: string;
  amount: number;
  address: string;
  estimatedDate: string;
  step: number;
  courierLat: number | null;
  courierLng: number | null;
  destinationAddress: string;
  routeDistanceText: string;
  routeEtaText: string;
}

@Component({
  selector: 'app-my-packages',
  standalone: true,
  imports: [CommonModule, HeaderComponent, FooterComponent],
  templateUrl: './my-packages.component.html',
  styleUrls: []
})
export class MyPackagesComponent implements OnDestroy {
  private readonly marketplaceApi = inject(MarketplaceApiService);
  private readonly deliveryApi = inject(DeliveryApiService);
  private readonly router = inject(Router);
  private readonly ngZone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);

  private liveRefreshIntervalId: number | null = null;
  private mapsLoaded = false;
  private mapPollTimer: number | null = null;

  readonly packages = signal<PackageViewModel[]>([]);
  activePackage: PackageViewModel | null = null;

  private mapInstance: google.maps.Map | null = null;
  private courierMarker: google.maps.Marker | null = null;
  private destinationMarker: google.maps.Marker | null = null;
  private routePolyline: google.maps.Polyline | null = null;

  constructor() {
    this.loadPackages();
    this.initLiveRefresh();
  }

  private loadPackages(): void {
    forkJoin({
      orders: this.marketplaceApi.orders.list(),
      deliveries: this.deliveryApi.getMyDeliveries().pipe(catchError(() => of([] as DeliveryResource[])))
    }).subscribe({
      next: ({ orders, deliveries }) => {
        const baseDeliveries = deliveries ?? [];

        const trackingRequests = baseDeliveries
          .filter((delivery) => typeof delivery.id === 'number')
          .map((delivery) =>
            this.deliveryApi.getDeliveryTracking(delivery.id).pipe(
              map((tracking) => ({ deliveryId: delivery.id, tracking })),
              catchError(() => of({ deliveryId: delivery.id, tracking: delivery.tracking }))
            )
          );

        if (trackingRequests.length === 0) {
          const deliveryMap = new Map<number, DeliveryResource>();
          for (const delivery of baseDeliveries) {
            const orderId = delivery.order?.id;
            if (orderId) deliveryMap.set(orderId, delivery);
          }
          this.packages.set(orders.map((order) => this.mapPackage(order, deliveryMap.get(order.id))));
          this.tryRenderMap();
          return;
        }

        forkJoin(trackingRequests).subscribe({
          next: (trackingRows) => {
            const enrichedById = new Map<number, DeliveryResource>();
            for (const delivery of baseDeliveries) enrichedById.set(delivery.id, { ...delivery });
            for (const row of trackingRows) {
              const current = enrichedById.get(row.deliveryId);
              if (current) current.tracking = row.tracking;
            }

            const deliveryMap = new Map<number, DeliveryResource>();
            for (const delivery of enrichedById.values()) {
              const orderId = delivery.order?.id;
              if (orderId) deliveryMap.set(orderId, delivery);
            }

            this.packages.set(orders.map((order) => this.mapPackage(order, deliveryMap.get(order.id))));
            this.tryRenderMap();
          },
          error: () => {
            const deliveryMap = new Map<number, DeliveryResource>();
            for (const delivery of baseDeliveries) {
              const orderId = delivery.order?.id;
              if (orderId) deliveryMap.set(orderId, delivery);
            }
            this.packages.set(orders.map((order) => this.mapPackage(order, deliveryMap.get(order.id))));
          }
        });
      },
      error: () => this.packages.set([])
    });
  }

  get shippedPackages(): PackageViewModel[] {
    return this.packages().filter(p => p.status === 'shipped');
  }

  selectPackage(pkg: PackageViewModel): void {
    this.activePackage = pkg;
    this.tryRenderMap();
  }

  private mapPackage(order: OrderResource, delivery?: DeliveryResource): PackageViewModel {
    const rawStatus = delivery?.status ?? order.status;
    const status = this.toTrackingStatus(rawStatus);
    const detailedAddress = [delivery?.deliveryAddress, delivery?.city, delivery?.postalCode]
      .filter((value) => !!value)
      .join(', ');

    let courierLat: number | null = null;
    let courierLng: number | null = null;

    if (delivery?.tracking?.currentLocation) {
      const parts = delivery.tracking.currentLocation.split(',');
      if (parts.length === 2) {
        courierLat = parseFloat(parts[0].trim());
        courierLng = parseFloat(parts[1].trim());
        if (!Number.isFinite(courierLat) || !Number.isFinite(courierLng)) {
          courierLat = null;
          courierLng = null;
        }
      }
    }

    return {
      orderId: order.id,
      id: `ORD-${order.id}`,
      status,
      items: `${order.orderLines?.length ?? 0} item(s)`,
      amount: order.totalAmount,
      address: detailedAddress || delivery?.address || order.shippingAddress || 'Delivery address',
      estimatedDate: delivery?.deliverydate
        ? new Date(delivery.deliverydate).toISOString().slice(0, 10)
        : new Date(order.date).toISOString().slice(0, 10),
      step: this.toStep(status),
      courierLat,
      courierLng,
      destinationAddress: detailedAddress || delivery?.address || order.shippingAddress || '',
      routeDistanceText: '',
      routeEtaText: ''
    };
  }

  private tryRenderMap(): void {
    const shipped = this.shippedPackages;
    if (shipped.length === 0) return;

    if (!this.activePackage || !shipped.includes(this.activePackage)) {
      this.activePackage = shipped[0];
    }

    if (this.mapsLoaded) {
      this.destroyMap();
      this.drawRoute();
      return;
    }

    if (typeof google !== 'undefined' && google.maps) {
      this.mapsLoaded = true;
      this.ngZone.runOutsideAngular(() => {
        setTimeout(() => this.drawRoute(), 100);
      });
      return;
    }

    if (!this.mapPollTimer) {
      this.mapPollTimer = window.setInterval(() => {
        if (typeof google !== 'undefined' && google.maps) {
          window.clearInterval(this.mapPollTimer!);
          this.mapPollTimer = null;
          this.mapsLoaded = true;
          this.ngZone.runOutsideAngular(() => {
            setTimeout(() => this.drawRoute(), 100);
          });
        }
      }, 500);
    }
  }

  private destroyMap(): void {
    this.clearMapOverlays();
    if (this.mapInstance) {
      const mapEl = this.mapInstance.getDiv();
      google.maps.event.clearInstanceListeners(mapEl);
      this.mapInstance = null;
    }
  }

  private drawRoute(): void {
    const pkg = this.activePackage;
    if (!pkg || pkg.courierLat === null || pkg.courierLng === null || !pkg.destinationAddress) return;

    const mapEl = document.getElementById('tracking-map-customer');
    if (!mapEl) return;

    const courierPos: google.maps.LatLngLiteral = { lat: pkg.courierLat, lng: pkg.courierLng };

    if (!this.mapInstance) {
      this.mapInstance = new google.maps.Map(mapEl, {
        center: courierPos,
        zoom: 13,
        disableDefaultUI: false,
        zoomControl: true,
        mapTypeControl: false,
        streetViewControl: false,
        fullscreenControl: false
      });
    }

    const map = this.mapInstance!;

    this.clearMapOverlays();

    this.courierMarker = new google.maps.Marker({
      position: courierPos,
      map,
      title: 'Courier position',
      icon: 'https://maps.google.com/mapfiles/ms/icons/blue-dot.png'
    });

    const directionsService = new google.maps.DirectionsService();

    directionsService.route(
      {
        origin: courierPos,
        destination: pkg.destinationAddress,
        travelMode: google.maps.TravelMode.DRIVING
      },
      (result: google.maps.DirectionsResult | null, status: string) => {
        this.ngZone.run(() => {
          if (status === 'OK' && result) {
            const leg = result.routes?.[0]?.legs?.[0];
            const distanceKm = leg?.distance?.value ? leg.distance.value / 1000 : null;
            const distText = leg?.distance?.text ?? (distanceKm !== null ? `${distanceKm.toFixed(1)} km` : '');
            const etaText = leg?.duration?.text ?? '';
            this.updatePackageInfo(pkg.orderId, distText, etaText);

            const endLocation = result.routes?.[0]?.legs?.[0]?.end_location;
            if (endLocation) {
              this.destinationMarker = new google.maps.Marker({
                position: { lat: endLocation.lat(), lng: endLocation.lng() },
                map,
                title: 'Delivery destination',
                icon: 'https://maps.google.com/mapfiles/ms/icons/red-dot.png'
              });
            }

            const path = result.routes?.[0]?.overview_path ?? [];
            if (path.length >= 2) {
              this.routePolyline = new google.maps.Polyline({
                path,
                strokeColor: '#2563eb',
                strokeOpacity: 0.9,
                strokeWeight: 5,
                map
              });
            }

            const bounds = new google.maps.LatLngBounds();
            bounds.extend(courierPos);
            if (endLocation) bounds.extend(endLocation);
            map.fitBounds(bounds, 60);
          } else {
            const geocoder = new google.maps.Geocoder();
            geocoder.geocode({ address: pkg.destinationAddress }, (geoResults: google.maps.GeocoderResult[] | null, geoStatus: string) => {
              if (geoStatus === 'OK' && geoResults?.[0]?.geometry?.location) {
                const destPos = { lat: geoResults[0].geometry.location.lat(), lng: geoResults[0].geometry.location.lng() };

                this.destinationMarker = new google.maps.Marker({
                  position: destPos,
                  map,
                  title: 'Delivery destination',
                  icon: 'https://maps.google.com/mapfiles/ms/icons/red-dot.png'
                });

                this.routePolyline = new google.maps.Polyline({
                  path: [courierPos, destPos],
                  strokeColor: '#2563eb',
                  strokeOpacity: 0.7,
                  strokeWeight: 4,
                  map,
                  icons: [{ icon: { path: 'M 0,-1 0,1', strokeOpacity: 1, scale: 3 }, offset: '0', repeat: '12px' }]
                });

                const bounds = new google.maps.LatLngBounds();
                bounds.extend(courierPos);
                bounds.extend(destPos);
                map.fitBounds(bounds, 60);

                const earthR = 6371000;
                const dLat = (destPos.lat - courierPos.lat) * Math.PI / 180;
                const dLng = (destPos.lng - courierPos.lng) * Math.PI / 180;
                const a = Math.sin(dLat / 2) ** 2 + Math.cos(courierPos.lat * Math.PI / 180) * Math.cos(destPos.lat * Math.PI / 180) * Math.sin(dLng / 2) ** 2;
                const km = (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * earthR) / 1000;
                this.updatePackageInfo(pkg.orderId, `${km.toFixed(1)} km (approx)`, `~${Math.max(1, Math.round((km / 35) * 60))} min`);
              }
              this.cdr.detectChanges();
            });
          }
          this.cdr.detectChanges();
        });
      }
    );
  }

  private updatePackageInfo(orderId: number, distanceText: string, etaText: string): void {
    this.packages.update(pkgs =>
      pkgs.map(p => p.orderId === orderId ? { ...p, routeDistanceText: distanceText, routeEtaText: etaText } : p)
    );
  }

  private clearMapOverlays(): void {
    if (this.courierMarker) { this.courierMarker.setMap(null); this.courierMarker = null; }
    if (this.destinationMarker) { this.destinationMarker.setMap(null); this.destinationMarker = null; }
    if (this.routePolyline) { this.routePolyline.setMap(null); this.routePolyline = null; }
  }

  private toTrackingStatus(rawStatus: string): 'pending' | 'confirmed' | 'shipped' | 'delivered' {
    const normalizedStatus = (rawStatus || '').trim().toLowerCase();
    if (normalizedStatus === 'processing') return 'confirmed';
    if (normalizedStatus === 'in progress' || normalizedStatus === 'in_progress' || normalizedStatus === 'shipped') return 'shipped';
    if (normalizedStatus === 'delivered') return 'delivered';
    if (normalizedStatus === 'pending') return 'pending';
    return 'pending';
  }

  private initLiveRefresh(): void {
    this.liveRefreshIntervalId = window.setInterval(() => {
      this.loadPackages();
    }, 5000);
  }

  ngOnDestroy(): void {
    if (this.liveRefreshIntervalId !== null) { window.clearInterval(this.liveRefreshIntervalId); this.liveRefreshIntervalId = null; }
    if (this.mapPollTimer !== null) { window.clearInterval(this.mapPollTimer); this.mapPollTimer = null; }
    this.destroyMap();
  }

  goToConfirmationDelivery(orderId: number): void {
    this.router.navigate(['/confirmation-de-delivery'], { queryParams: { orderId } });
  }

  private toStep(status: 'pending' | 'confirmed' | 'shipped' | 'delivered'): number {
    switch (status) {
      case 'confirmed': return 1;
      case 'shipped': return 2;
      case 'delivered': return 3;
      case 'pending':
      default: return 0;
    }
  }
}
