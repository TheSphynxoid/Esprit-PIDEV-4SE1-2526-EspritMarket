import { ChangeDetectorRef, Component, DestroyRef, ViewChild, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';

import { FormsModule } from '@angular/forms';
import { MatPaginatorModule } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { catchError, forkJoin, interval, of } from 'rxjs';
import { FooterComponent } from '../../../shared/layout';
import { ModalComponent } from '../../../shared/components';
import { DeliveryFormComponent, DeliveryFormData, DeliveryOrderOption } from '../delivery-form';
import { AuthService, DeliveryApiService, MarketplaceApiService, QuizResultResponse, QuizService } from '../../../services';
import { AdminDeliveryOverviewResponse } from '../../../services/api/models/api-resource.model';
import { AdminCourierInfoResponse, CourierStatus, VehicleInfoDto, DeliveryWritePayload } from '../../../services/api';

interface OrderItem {
  id: number;
  customer: string;
  amount: number;
  status: string;
  date: string;
  paymentMethod: string;
  shipping_address: string;
}

interface DuplicateShippingAddressGroup {
  address: string;
  orders: OrderItem[];
}

interface DeliveryItem {
  id: number;
  code: string;
  deliverytype: string;
  status: string;
  cancellationReason?: string;
  deliverydate: string;
  address: string;
  city?: string;
  postalCode?: string;
  phoneNumber?: string;
  deliveryMode?: string;
  paymentMode?: string;
  distanceKm?: number | null;
  realWeightKg?: number | null;
  volumetricWeightKg?: number | null;
  billableWeightKg?: number | null;
  orderId?: number;
  vehiculeId?: number | null;
}

interface VehicleItem {
  id: number;
  licensePlate: string;
  type: string;
  status: string;
  capacity: number;
  vehiclePhotoFileName?: string;
  registrationCardFrontFileName?: string;
  registrationCardBackFileName?: string;
}

interface VehicleOwnerInfo {
  nom: string;
  prenom: string;
}

interface DeliveryAssignmentItem {
  deliveryId: number;
  deliveryCode: string;
  deliveryType: string;
  deliveryDate: string;
  deliveryStatus: string;
  cancellationReason?: string;
  deliveryAddress: string;
  orderId?: number;
  vehicleId: number;
  vehicleLicensePlate: string;
  vehicleType: string;
  vehicleStatus: string;
  vehicleCapacity: number;
}
//ajouter courier pour afficher les livreurs dans le dashboard admin
interface AdminCourierItem {
  courierId: number;
  userId: number;
  nom: string;
  prenom: string;
  email: string;
  phoneNumber: string;
  status: CourierStatus;
  quizResult: string;
  quizScore: number | null;
  meetingLink: string;
  interviewDate: string;
  interviewDateInput: string;
  voitures: VehicleInfoDto[];
  permitImage: string;
}

interface InterviewDayData {
  date: string;
  dayLabel: string;
  dayOfWeek: string;
  interviews: AdminCourierItem[];
}

type InterviewStatusTab = 'PENDING' | 'ACCEPTED' | 'REFUSED';
type DeliveryManagementTab = 'SAME_DAY' | 'EXPRESS' | 'STANDARD' | 'DELIVERED' | 'CANCELLED';
type OverviewPeriod = 7 | 30 | 90;

interface AdminOverviewKpis {
  totalOrders: number;
  totalDeliveries: number;
  deliveredDeliveries: number;
  pendingDeliveries: number;
  cancelledDeliveries: number;
  acceptedDeliveries: number;
  refusedDeliveries: number;
  completionRate: number;
  totalVehicles: number;
  totalCouriers: number;
  acceptedCouriers: number;
  pendingCouriers: number;
  refusedCouriers: number;
}

interface OverviewTrendPoint {
  date: string;
  count: number;
  label: string;
}

interface OverviewUpcomingItem {
  id: number;
  to: string;
  date: string;
  status: string;
}

interface StatusDistributionItem {
  label: string;
  count: number;
  percent: number;
}

@Component({
  selector: 'app-admin-delivery-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, MatPaginatorModule, FooterComponent, ModalComponent, DeliveryFormComponent],
  templateUrl: './admin-delivery-dashboard.component.html',
  styleUrls: ['./admin-delivery-dashboard.component.css']
})
export class AdminDeliveryDashboardComponent {
  @ViewChild(DeliveryFormComponent) deliveryFormComponent?: DeliveryFormComponent;

  private readonly deliveryApi = inject(DeliveryApiService);
  private readonly marketplaceApi = inject(MarketplaceApiService);
  private readonly quizService = inject(QuizService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  activeTab = 'overview';
  vehicleStatusFilter: 'ALL' | 'AVAILABLE' | 'UNAVAILABLE' = 'AVAILABLE';
  vehicleTypeFilter: 'ALL' | 'CAR' | 'MOTOR' | 'TRUCK' = 'ALL';
  vehicleRegistrationFilterTerm = '';
  courierEmailFilterTerm = '';
  assignedDeliveryFilterTerm = '';
  deliveryManagementFilterTerm = '';
  searchOrderTerm = '';
  interviewPhoneFilterTerm = '';
  interviewStatusTab: InterviewStatusTab = 'PENDING';
  deliveryManagementTab: DeliveryManagementTab = 'SAME_DAY';
  readonly overviewPeriod: OverviewPeriod = 7;
  readonly pageSize = 10;
  pendingCouriersPage = 0;
  acceptedCouriersPage = 0;
  refusedCouriersPage = 0;
  pendingInterviewsPage = 0;
  unassignedOrdersPage = 0;
  assignedOrdersPage = 0;
  activeAssignmentsPage = 0;
  sameDayDeliveriesPage = 0;
  expressDeliveriesPage = 0;
  standardDeliveriesPage = 0;
  deliveredAssignmentsPage = 0;
  cancelledAssignmentsPage = 0;
  availableVehiclesPage = 0;
  unavailableVehiclesPage = 0;
  couriersPage = 0;
  private readonly shippingAddressColorPalette: readonly string[] = [
    'bg-amber-50 border-amber-200 text-amber-800',
    'bg-emerald-50 border-emerald-200 text-emerald-800',
    'bg-sky-50 border-sky-200 text-sky-800',
    'bg-rose-50 border-rose-200 text-rose-800',
    'bg-indigo-50 border-indigo-200 text-indigo-800',
    'bg-orange-50 border-orange-200 text-orange-800',
    'bg-teal-50 border-teal-200 text-teal-800',
    'bg-fuchsia-50 border-fuchsia-200 text-fuchsia-800'
  ];
  private readonly shippingAddressDuplicateBadgePalette: readonly string[] = [
    'bg-amber-200 text-amber-900',
    'bg-emerald-200 text-emerald-900',
    'bg-sky-200 text-sky-900',
    'bg-rose-200 text-rose-900',
    'bg-indigo-200 text-indigo-900',
    'bg-orange-200 text-orange-900',
    'bg-teal-200 text-teal-900',
    'bg-fuchsia-200 text-fuchsia-900'
  ];

  isModalOpen = signal(false);
  isDetailsModalOpen = signal(false);
  isEditing = signal(false);
  isSubmitting = signal(false);
  editingDeliveryId = signal<number | null>(null);
  errorMessage = signal('');
  modalOrderId = signal<number | null>(null);
  selectedDetailsDelivery = signal<DeliveryItem | null>(null);

  // Dashboard statistics
  dashboardStatistics = signal<AdminDeliveryOverviewResponse | null>(null);
  statisticsLoading = signal(false);
  statisticsError = signal('');
  selectedOrderDetails = signal<any | null>(null);

  orders: OrderItem[] = [];
  deliveries: DeliveryItem[] = [];
  vehicles: VehicleItem[] = [];
  orderOptions: DeliveryOrderOption[] = [];
  qrCodeUrls: Record<number, string> = {};
  vehiclePhotoUrls: Record<number, string | null> = {};
  vehicleRegistrationFrontUrls: Record<number, string | null> = {};
  vehicleRegistrationBackUrls: Record<number, string | null> = {};
  vehicleInlinePhotoUrls: Record<number, string | null> = {};
  vehicleInlineRegistrationFrontUrls: Record<number, string | null> = {};
  vehicleInlineRegistrationBackUrls: Record<number, string | null> = {};

  assignments: DeliveryAssignmentItem[] = [];
  couriers: AdminCourierItem[] = [];
  interviewWeekData: InterviewDayData[] = [];
  selectedInterviewDayIndex = signal(0);
  interviewCalendarLoading = signal(false);
  selectedPreviewImageUrl = signal<string | null>(null);
  selectedPreviewImageLabel = signal('');
  selectedVehicleByDelivery: Record<number, number | null> = {};
  selectedVehicleByAssignment: Record<number, number | null> = {};
  readonly courierStatuses: CourierStatus[] = ['PENDING', 'ACCEPTED', 'REFUSED'];
  private readonly vehicleDocumentsLoadingIds = new Set<number>();
  private readonly vehicleOwnerBySeries = new Map<string, VehicleOwnerInfo>();
  private readonly updatingCourierStatusIds = new Set<number>();
  private readonly updatingInterviewDateIds = new Set<number>();
  private readonly interviewDateNotificationMessage = 'voici votre date entretien';
  assignmentError = signal('');
  assignmentMessage = signal('');
  couriersError = signal('');
  courierStatusMessage = signal('');

  constructor() {
    this.refreshData();
    interval(300000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refreshData());
  }

  getPageSlice<T>(items: T[], pageIndex: number): T[] {
    const start = pageIndex * this.pageSize;
    return items.slice(start, start + this.pageSize);
  }

  get overviewKpis(): AdminOverviewKpis {
    const stats = this.dashboardStatistics();
    const totalOrders = stats?.totalOrders ?? this.orders.length;
    const totalDeliveries = stats?.totalDeliveries ?? this.deliveries.length;
    const deliveredDeliveries = stats?.deliveredDeliveries ?? this.deliveredDeliveriesCount;
    const pendingDeliveries = stats?.pendingDeliveries ?? this.pendingDeliveriesCount;
    const cancelledDeliveries = stats?.cancelledDeliveries ?? this.countDeliveriesByStatus(['CANCELLED', 'CANCELLED '.trim(), 'ANNULE', 'ANNULEE']);
    const acceptedDeliveries = stats?.acceptedDeliveries ?? this.countDeliveriesByStatus(['ACCEPTED']);
    const refusedDeliveries = stats?.refusedDeliveries ?? this.countDeliveriesByStatus(['REFUSED', 'REJECTED', 'DECLINED']);
    const totalVehicles = stats?.totalVehicles ?? this.vehicles.length;
    const totalCouriers = stats?.totalCouriers ?? this.couriers.length;
    const acceptedCouriers = stats?.acceptedCouriers ?? this.couriers.filter((courier) => courier.status === 'ACCEPTED').length;
    const pendingCouriers = stats?.pendingCouriers ?? this.couriers.filter((courier) => courier.status === 'PENDING').length;
    const refusedCouriers = stats?.refusedCouriers ?? this.couriers.filter((courier) => courier.status === 'REFUSED').length;

    return {
      totalOrders,
      totalDeliveries,
      deliveredDeliveries,
      pendingDeliveries,
      cancelledDeliveries,
      acceptedDeliveries,
      refusedDeliveries,
      completionRate: totalDeliveries > 0 ? (deliveredDeliveries / totalDeliveries) * 100 : 0,
      totalVehicles,
      totalCouriers,
      acceptedCouriers,
      pendingCouriers,
      refusedCouriers
    };
  }

  get overviewTrendData(): OverviewTrendPoint[] {
    const stats = this.dashboardStatistics();
    const source = (stats?.deliveriesPerDay ?? stats?.ordersPerDay ?? []).map((entry) => ({
      date: entry.date,
      count: Number(entry.count ?? 0),
      label: this.formatTrendLabel(entry.date)
    }));

    if (source.length > 0) {
      return source.slice(-this.overviewPeriod);
    }

    return this.buildLocalTrendFallback();
  }

  get deliveredTrendPoints(): string {
    const points = this.overviewTrendData;
    if (points.length === 0) {
      return '0,40 100,40';
    }

    const max = Math.max(...points.map((point) => point.count), 1);
    const step = points.length === 1 ? 0 : 100 / (points.length - 1);

    return points
      .map((point, index) => {
        const x = index * step;
        const y = 36 - (point.count / max) * 30;
        return `${x.toFixed(1)},${y.toFixed(1)}`;
      })
      .join(' ');
  }

  get deliveredLast7DayTicks(): string[] {
    return this.overviewTrendData.map((point) => point.label);
  }

  get trendDeltaLabel(): string {
    const points = this.overviewTrendData;
    if (points.length < 2) {
      return 'Stable';
    }

    const first = points[0]?.count ?? 0;
    const last = points[points.length - 1]?.count ?? 0;
    const delta = last - first;

    if (delta === 0) {
      return 'Stable';
    }

    return delta > 0 ? `+${delta} vs start` : `${delta} vs start`;
  }

  get completionRingBackground(): string {
    const completionRate = this.overviewKpis.completionRate;
    return `conic-gradient(#22c55e 0% ${completionRate}%, #e2e8f0 ${completionRate}% 100%)`;
  }

  get statusDistribution(): StatusDistributionItem[] {
    const kpis = this.overviewKpis;
    const total = Math.max(kpis.totalDeliveries, 1);

    return [
      { label: 'Delivered', count: kpis.deliveredDeliveries, percent: (kpis.deliveredDeliveries / total) * 100 },
      { label: 'Pending', count: kpis.pendingDeliveries, percent: (kpis.pendingDeliveries / total) * 100 },
      { label: 'Accepted', count: kpis.acceptedDeliveries, percent: (kpis.acceptedDeliveries / total) * 100 },
      { label: 'Cancelled', count: kpis.cancelledDeliveries + kpis.refusedDeliveries, percent: ((kpis.cancelledDeliveries + kpis.refusedDeliveries) / total) * 100 }
    ];
  }

  get overviewUpcomingDeliveries(): OverviewUpcomingItem[] {
    return this.deliveries
      .filter((delivery) => this.isPendingOrOpenDelivery(delivery.status))
      .slice()
      .sort((left, right) => this.deliveryDateValue(right) - this.deliveryDateValue(left))
      .slice(0, 4)
      .map((delivery) => ({
        id: delivery.id,
        to: delivery.address || delivery.city || 'Unknown destination',
        date: delivery.deliverydate,
        status: delivery.status
      }));
  }

  get overviewCouriersPerMonth(): Array<{ month: string; count: number }> {
    return (this.dashboardStatistics()?.couriersPerMonth ?? []).map((entry) => ({
      month: entry.month,
      count: Number(entry.count ?? 0)
    }));
  }

  get overviewCourierMonthTicks(): string[] {
    return this.overviewCouriersPerMonth.map((entry) => this.formatMonthLabel(entry.month));
  }

  get overviewCourierMonthPoints(): string {
    const points = this.overviewCouriersPerMonth;
    if (points.length === 0) {
      return '0,40 100,40';
    }

    const max = Math.max(...points.map((point) => point.count), 1);
    const step = points.length === 1 ? 0 : 100 / (points.length - 1);

    return points
      .map((point, index) => {
        const x = index * step;
        const y = 36 - (point.count / max) * 30;
        return `${x.toFixed(1)},${y.toFixed(1)}`;
      })
      .join(' ');
  }

  get averageDistancePerDeliveryKm(): number {
    const deliveries = this.deliveries.filter((delivery) => delivery.distanceKm != null);
    if (deliveries.length === 0) {
      return 0;
    }

    const total = deliveries.reduce((sum, delivery) => sum + Number(delivery.distanceKm ?? 0), 0);
    return total / deliveries.length;
  }

  private countDeliveriesByStatus(statuses: string[]): number {
    const allowed = new Set(statuses.map((status) => status.trim().toUpperCase()));
    return this.deliveries.filter((delivery) => allowed.has((delivery.status ?? '').trim().toUpperCase())).length;
  }

  private buildLocalTrendFallback(): OverviewTrendPoint[] {
    const today = new Date();
    const fallback: OverviewTrendPoint[] = [];

    for (let index = this.overviewPeriod - 1; index >= 0; index -= 1) {
      const date = new Date(today);
      date.setDate(today.getDate() - index);
      const dateKey = date.toISOString().slice(0, 10);
      const count = this.deliveries.filter((delivery) => this.normalizeTrendDate(delivery.deliverydate) === dateKey).length;

      fallback.push({
        date: dateKey,
        count,
        label: this.formatTrendLabel(dateKey)
      });
    }

    return fallback;
  }

  private formatTrendLabel(dateValue: string): string {
    const date = new Date(dateValue);
    if (Number.isNaN(date.getTime())) {
      return dateValue.slice(5, 10);
    }

    return date.toLocaleDateString('en-US', { weekday: 'short' });
  }

  private formatMonthLabel(monthValue: string): string {
    const [year, month] = monthValue.split('-').map((part) => Number(part));
    if (!year || !month) {
      return monthValue;
    }

    const date = new Date(year, month - 1, 1);
    return date.toLocaleDateString('en-US', { month: 'short', year: '2-digit' });
  }

  private normalizeTrendDate(value: string | Date | null | undefined): string {
    if (!value) {
      return '';
    }

    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) {
      return '';
    }

    return date.toISOString().slice(0, 10);
  }

  private isPendingOrOpenDelivery(status: string | null | undefined): boolean {
    const normalized = (status ?? '').trim().toUpperCase();
    return normalized !== 'DELIVERED' && normalized !== 'CANCELLED';
  }

  private deliveryDateValue(delivery: DeliveryItem): number {
    const rawDate = delivery.deliverydate;
    if (!rawDate) {
      return 0;
    }

    const date = new Date(rawDate);
    return Number.isNaN(date.getTime()) ? 0 : date.getTime();
  }

  get pendingDeliveriesCount(): number {
    return this.deliveries.filter((delivery) => delivery.status === 'PENDING').length;
  }

  get deliveredDeliveriesCount(): number {
    return this.deliveries.filter((delivery) => delivery.status === 'DELIVERED').length;
  }

  get pendingCouriers(): AdminCourierItem[] {
    return this.couriers.filter((courier) =>
      courier.status === 'PENDING' && this.matchesCourierPhoneFilter(courier)
    );
  }

  get pendingCouriersNotificationCount(): number {
    return this.couriers.filter((courier) => courier.status === 'PENDING').length;
  }

  get hasPendingInterviewAlert(): boolean {
    return this.pendingCouriersNotificationCount > 0;
  }

  get acceptedCouriers(): AdminCourierItem[] {
    return this.couriers.filter((courier) =>
      courier.status === 'ACCEPTED' && this.matchesCourierPhoneFilter(courier)
    );
  }

  get refusedCouriers(): AdminCourierItem[] {
    return this.couriers.filter((courier) =>
      courier.status === 'REFUSED' && this.matchesCourierPhoneFilter(courier)
    );
  }

  hasCouriersForStatus(status: CourierStatus): boolean {
    if (status === 'PENDING') {
      return this.pendingCouriers.length > 0;
    }

    if (status === 'ACCEPTED') {
      return this.acceptedCouriers.length > 0;
    }

    return this.refusedCouriers.length > 0;
  }

  get availableVehicles(): VehicleItem[] {
    return this.vehicles.filter((vehicle) => this.isVehicleAvailable(vehicle));
  }

  get unavailableVehicles(): VehicleItem[] {
    return this.vehicles.filter((vehicle) => (vehicle.status ?? '').toUpperCase() === 'UNAVAILABLE');
  }

  get showAvailableVehiclesTable(): boolean {
    return this.vehicleStatusFilter === 'ALL' || this.vehicleStatusFilter === 'AVAILABLE';
  }

  get showUnavailableVehiclesTable(): boolean {
    return this.vehicleStatusFilter === 'ALL' || this.vehicleStatusFilter === 'UNAVAILABLE';
  }

  get filteredAvailableVehicles(): VehicleItem[] {
    return this.availableVehicles
      .filter((vehicle) => this.matchesVehicleTypeFilter(vehicle))
      .filter((vehicle) => this.matchesVehicleRegistrationFilter(vehicle));
  }

  get filteredUnavailableVehicles(): VehicleItem[] {
    return this.unavailableVehicles
      .filter((vehicle) => this.matchesVehicleTypeFilter(vehicle))
      .filter((vehicle) => this.matchesVehicleRegistrationFilter(vehicle));
  }

  get filteredCouriers(): AdminCourierItem[] {
    return this.couriers.filter((courier) => this.matchesCourierEmailFilter(courier));
  }

  get filteredSameDayDeliveries(): DeliveryItem[] {
    const deliveriesByType = this.deliveries
      .filter((delivery) => !this.isClosedStatus(delivery.status))
      .filter((delivery) => this.isUnassignedDelivery(delivery))
      .filter((delivery) => this.matchesDeliveryType(delivery, 'same_day'));

    return this.filterDeliveriesByTerm(deliveriesByType, this.deliveryManagementFilterTerm);
  }

  get filteredExpressDeliveries(): DeliveryItem[] {
    const deliveriesByType = this.deliveries
      .filter((delivery) => !this.isClosedStatus(delivery.status))
      .filter((delivery) => this.isUnassignedDelivery(delivery))
      .filter((delivery) => this.matchesDeliveryType(delivery, 'express'));

    return this.filterDeliveriesByTerm(deliveriesByType, this.deliveryManagementFilterTerm);
  }

  get filteredStandardDeliveries(): DeliveryItem[] {
    const deliveriesByType = this.deliveries
      .filter((delivery) => !this.isClosedStatus(delivery.status))
      .filter((delivery) => this.isUnassignedDelivery(delivery))
      .filter((delivery) => this.matchesDeliveryType(delivery, 'standard'));

    return this.filterDeliveriesByTerm(deliveriesByType, this.deliveryManagementFilterTerm);
  }

  get sameDayDeliveriesNotificationCount(): number {
    return this.deliveries
      .filter((delivery) => !this.isClosedStatus(delivery.status))
      .filter((delivery) => this.isUnassignedDelivery(delivery))
      .filter((delivery) => this.matchesDeliveryType(delivery, 'same_day')).length;
  }

  get expressDeliveriesNotificationCount(): number {
    return this.deliveries
      .filter((delivery) => !this.isClosedStatus(delivery.status))
      .filter((delivery) => this.isUnassignedDelivery(delivery))
      .filter((delivery) => this.matchesDeliveryType(delivery, 'express')).length;
  }

  get standardDeliveriesNotificationCount(): number {
    return this.deliveries
      .filter((delivery) => !this.isClosedStatus(delivery.status))
      .filter((delivery) => this.isUnassignedDelivery(delivery))
      .filter((delivery) => this.matchesDeliveryType(delivery, 'standard')).length;
  }

  get deliveryManagementNotificationCount(): number {
    return this.sameDayDeliveriesNotificationCount
      + this.expressDeliveriesNotificationCount
      + this.standardDeliveriesNotificationCount;
  }

  get hasDeliveryManagementAlert(): boolean {
    return this.deliveryManagementNotificationCount > 0;
  }




get searchedOrders(): OrderItem[] {
    const term = this.searchOrderTerm.trim().toLowerCase();
    if (!term) {
      return this.orders;
    }


    return this.orders.filter((order) => {
      const orderCode = String(order.id).toLowerCase();
      return orderCode.includes(term);
    });
  }
//orders Assigned et unassigned
  get unassignedOrders(): OrderItem[] {
    const assignedOrderIds = this.assignedOrderIds;
    return this.searchedOrders.filter((order) => !assignedOrderIds.has(order.id));
  }

  get assignedOrders(): OrderItem[] {
    const assignedOrderIds = this.assignedOrderIds;
    return this.searchedOrders.filter((order) => assignedOrderIds.has(order.id));
  }

  getAssignedDeliveryCode(orderId: number): string {
    const delivery = this.deliveries.find((item) => item.orderId === orderId);
    return delivery?.code ?? '-';
  }

  private get assignedOrderIds(): Set<number> {
    return new Set(
      this.deliveries
        .map((delivery) => delivery.orderId)
        .filter((orderId): orderId is number => typeof orderId === 'number')
    );
  }
//Orders with same address

  get duplicateShippingAddressGroups(): DuplicateShippingAddressGroup[] {
    const groupsByAddress = new Map<string, DuplicateShippingAddressGroup>();

    for (const order of this.unassignedOrders) {
      const normalizedAddress = this.normalizeShippingAddress(order.shipping_address);
      if (!normalizedAddress) {
        continue;
      }

      const existing = groupsByAddress.get(normalizedAddress);
      if (existing) {
        existing.orders.push(order);
      } else {
        groupsByAddress.set(normalizedAddress, {
          address: order.shipping_address,
          orders: [order]
        });
      }
    }

    return Array.from(groupsByAddress.values())
      .filter((group) => group.orders.length > 1)
      .map((group) => ({
        ...group,
        orders: [...group.orders].sort((left, right) => left.id - right.id)
      }))
      .sort((left, right) => left.address.localeCompare(right.address));
  }

  hasDuplicateShippingAddress(address: string): boolean {
    const normalizedAddress = this.normalizeShippingAddress(address);
    if (!normalizedAddress) {
      return false;
    }

    let addressCount = 0;
    for (const order of this.unassignedOrders) {
      if (this.normalizeShippingAddress(order.shipping_address) === normalizedAddress) {
        addressCount += 1;
      }
      if (addressCount > 1) {
        return true;
      }
    }

    return false;
  }

  getShippingAddressBoxClass(address: string): string {
    const normalizedAddress = this.normalizeShippingAddress(address);
    if (!normalizedAddress) {
      return 'rounded-md border border-neutral-200 bg-neutral-50 px-3 py-2 text-neutral-700';
    }

    const colorClass = this.shippingAddressColorPalette[this.getAddressColorIndex(normalizedAddress)];
    return `rounded-md border px-3 py-2 ${colorClass}`;
  }

  getShippingAddressDuplicateBadgeClass(address: string): string {
    const normalizedAddress = this.normalizeShippingAddress(address);
    if (!normalizedAddress) {
      return 'bg-neutral-200 text-neutral-700';
    }

    return this.shippingAddressDuplicateBadgePalette[this.getAddressColorIndex(normalizedAddress)];
  }














  openCreateDeliveryModal(orderId?: number): void {
    this.isEditing.set(false);
    this.editingDeliveryId.set(null);
    this.errorMessage.set('');
    this.modalOrderId.set(orderId ?? null);
    this.isModalOpen.set(true);
    setTimeout(() => {
      this.deliveryFormComponent?.resetForm();
    }, 100);
  }

  openEditDeliveryModal(delivery: DeliveryItem): void {
    this.isEditing.set(true);
    this.editingDeliveryId.set(delivery.id);
    this.errorMessage.set('');
    this.isModalOpen.set(true);
    setTimeout(() => {
      this.deliveryFormComponent?.setFormData({
        deliverytype: delivery.deliverytype,
        status: delivery.status,
        cancellationReason: delivery.cancellationReason ?? '',
        deliverydate: delivery.deliverydate,
        address: delivery.address,
        orderId: delivery.orderId ?? null
      });
    }, 100);
  }

  openDeliveryDetails(delivery: DeliveryItem): void {
    this.selectedDetailsDelivery.set(delivery);
    this.isDetailsModalOpen.set(true);
    // load order details if available
    this.selectedOrderDetails.set(null);
    const orderId = delivery.orderId;
    if (typeof orderId === 'number') {
      // try to find in already loaded orders
      const existing = this.orders.find((o) => o.id === orderId);
      if (existing) {
        this.selectedOrderDetails.set(existing);
      }

      // if the API exposes a get method, prefer the fresh copy
      const maybeGet = (this.marketplaceApi.orders as any)?.get;
      if (typeof maybeGet === 'function') {
        maybeGet.call(this.marketplaceApi.orders, orderId).pipe(catchError(() => of(null))).subscribe((fullOrder: any) => {
          if (fullOrder) {
            this.selectedOrderDetails.set(fullOrder);
            this.forceViewUpdate();
          }
        });
      }

    }

    // ensure QR code is loaded
    if (!this.qrCodeUrls[delivery.id]) {
      this.deliveryApi.getDeliveryQrCode(delivery.id).subscribe({
        next: (blob) => {
          this.qrCodeUrls[delivery.id] = URL.createObjectURL(blob);
          this.forceViewUpdate();
        },
        error: () => {
          this.qrCodeUrls[delivery.id] = '';
        }
      });
    }
  }

  closeDeliveryDetails(): void {
    this.selectedDetailsDelivery.set(null);
    this.isDetailsModalOpen.set(false);
  }

  getDeliveryDetailsCandidate(assignment: DeliveryAssignmentItem): DeliveryItem {
    const existing = this.deliveries.find((delivery) => delivery.id === assignment.deliveryId);
    if (existing) {
      return existing;
    }

    return {
      id: assignment.deliveryId,
      code: assignment.deliveryCode,
      deliverytype: assignment.deliveryType,
      status: assignment.deliveryStatus,
      deliverydate: assignment.deliveryDate,
      address: assignment.deliveryAddress,
      orderId: assignment.orderId,
      vehiculeId: assignment.vehicleId
    };
  }

  formatWeightKg(value: number | null | undefined): string {
    if (typeof value !== 'number' || !Number.isFinite(value)) {
      return '-';
    }
    return `${value.toFixed(3)} kg`;
  }

  formatDistanceKm(value: number | null | undefined): string {
    if (typeof value !== 'number' || !Number.isFinite(value)) {
      return '-';
    }
    return `${value.toFixed(3)} km`;
  }

  closeDeliveryModal(): void {
    this.isModalOpen.set(false);
    this.isEditing.set(false);
    this.editingDeliveryId.set(null);
    this.modalOrderId.set(null);
    this.errorMessage.set('');
  }

  submitDeliveryForm(): void {
    if (!this.deliveryFormComponent?.isFormValid()) {
      return;
    }

    const formData = this.deliveryFormComponent.getFormData();
    const payload: DeliveryWritePayload = this.toPayload(formData);
    const deliveryId = this.editingDeliveryId();

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const request = deliveryId
      ? this.deliveryApi.deliveries.update(deliveryId, payload)
      : this.deliveryApi.deliveries.create(payload);

    request.subscribe({
      next: (delivery) => {
        // Synchronize order status with delivery status
        this.syncOrderStatus(delivery);
        this.closeDeliveryModal();
        this.refreshData();
      },
      error: (error) => {
        const message = error?.error?.message ?? error?.message ?? 'Failed to save delivery';
        this.errorMessage.set(message);
        this.isSubmitting.set(false);
      },
      complete: () => {
        this.isSubmitting.set(false);
      }
    });
  }

  confirmDeleteDelivery(deliveryId: number, code: string): void {
    const confirmed = window.confirm(`Are you sure you want to delete delivery "${code}"?`);
    if (confirmed) {
      this.deleteDelivery(deliveryId);
    }
  }

  private refreshData(): void {
    this.loadStatistics();
    this.loadOrders();
    this.loadDeliveries();
    this.loadVehicles();
    this.loadCouriers();
  }

  private loadStatistics(): void {
    this.statisticsError.set('');
    this.statisticsLoading.set(true);
    this.deliveryApi.getDashboardStatistics().subscribe({
      next: (stats) => {
        this.dashboardStatistics.set(stats);
        this.statisticsLoading.set(false);
        this.forceViewUpdate();
      },
      error: (err) => {
        const message = err?.error?.message ?? err?.message ?? 'Impossible de charger les statistiques.';
        this.statisticsError.set(message);
        this.statisticsLoading.set(false);
        this.forceViewUpdate();
      }
    });
  }

  private forceViewUpdate(): void {
    this.cdr.detectChanges();
  }
//ajouter courier pour afficher les livreurs dans le dashboard admin

  private loadCouriers(): void {
    this.couriersError.set('');
    this.courierStatusMessage.set('');

    this.deliveryApi.listAdminCouriers().subscribe({
      next: (items: AdminCourierInfoResponse[]) => {
        const baseCouriers = (items ?? []).map((item) => ({
          courierId: item.courierId,
          userId: item.userId,
          nom: item.nom,
          prenom: item.prenom,
          email: item.email,
          phoneNumber: this.readOptionalString(item, ['phoneNumber', 'phone_number', 'phone', 'telephone', 'tel', 'mobile']),
          status: this.normalizeCourierStatus(item.status),
          quizResult: this.readOptionalString(item, ['quizResult', 'quizStatus', 'resultatQuiz', 'quiz_result']),
          quizScore: this.readOptionalNumber(item, ['quizScore', 'score', 'quiz_score']),
          meetingLink: this.readOptionalString(item, ['meetingLink', 'meetingUrl', 'interviewLink', 'meeting_link']),
          interviewDate: this.readOptionalString(item, ['interviewDate', 'interview_date', 'dateEntretien']),
          interviewDateInput: this.toDatetimeLocalValue(this.readOptionalString(item, ['interviewDate', 'interview_date', 'dateEntretien'])),
          voitures: item.voitures ?? [],
          permitImage: item.permitImage
        }));

        this.rebuildVehicleOwnerMap(baseCouriers);

        if (baseCouriers.length === 0) {
          this.couriers = [];
          this.forceViewUpdate();
          return;
        }

        const recentQuizRequests = baseCouriers.map((courier) =>
          this.quizService.getMostRecentQuizResult(courier.userId).pipe(catchError(() => of(null)))
        );

        forkJoin(recentQuizRequests).subscribe({
          next: (quizResults: Array<QuizResultResponse | null>) => {
            this.couriers = baseCouriers.map((courier, index) => {
              const quiz = quizResults[index];
              if (!quiz) {
                return courier;
              }

              return {
                ...courier,
                quizResult: this.normalizeQuizResult(quiz.status) || courier.quizResult,
                quizScore: typeof quiz.score === 'number' ? quiz.score : courier.quizScore,
                meetingLink: quiz.meetingLink?.trim() || courier.meetingLink,
                interviewDate: quiz.interviewScheduledAt?.trim() || courier.interviewDate,
                interviewDateInput: this.toDatetimeLocalValue(quiz.interviewScheduledAt?.trim() || courier.interviewDate)
              };
            });
            this.rebuildVehicleOwnerMap(this.couriers);
            this.forceViewUpdate();
            this.loadInterviewWeek();
          },
          error: () => {
            this.couriers = baseCouriers;
            this.rebuildVehicleOwnerMap(this.couriers);
            this.forceViewUpdate();
            this.loadInterviewWeek();
          }
        });
      },
      error: (error) => {
        this.couriers = [];
        this.rebuildVehicleOwnerMap([]);
        const message = error?.error?.message ?? error?.message ?? 'Impossible de charger les livreurs.';
        this.couriersError.set(message);
        this.forceViewUpdate();
        this.loadInterviewWeek();
      }
    });
  }

  private loadInterviewWeek(): void {
    this.interviewCalendarLoading.set(true);
    this.deliveryApi.getAdminInterviewWeek().subscribe({
      next: (weekData: any) => {
        const weekdayOrder = ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi'];
        const days = Array.isArray(weekData?.days) ? weekData.days : [];

        this.interviewWeekData = days
          .map((dayData: any) => {
            const date = dayData?.date || '';
            const dayOfWeek = this.getDayOfWeekFromDate(date);
            return {
              date,
              dayLabel: dayData?.dayLabel || date,
              dayOfWeek,
              interviews: (dayData?.interviews ?? []).map((interview: any) => this.convertInterviewToCourier(interview))
            };
          })
          .filter((dayData: InterviewDayData) => weekdayOrder.includes(dayData.dayOfWeek))
          .sort((left: InterviewDayData, right: InterviewDayData) => {
            const leftIndex = weekdayOrder.indexOf(left.dayOfWeek);
            const rightIndex = weekdayOrder.indexOf(right.dayOfWeek);

            if (leftIndex !== rightIndex) {
              return leftIndex - rightIndex;
            }

            return new Date(left.date).getTime() - new Date(right.date).getTime();
          });

        this.selectedInterviewDayIndex.set(this.interviewWeekData.length > 0 ? 0 : -1);
        this.interviewCalendarLoading.set(false);
        this.forceViewUpdate();
      },
      error: (error) => {
        console.error('Erreur lors du chargement des entretiens:', error);
        this.interviewWeekData = [];
        this.interviewCalendarLoading.set(false);
        this.forceViewUpdate();
      }
    });
  }

  private getDayOfWeekFromDate(dateStr: string): string {
    if (!dateStr) return '';
    try {
      const date = new Date(dateStr);
      const days = ['Dimanche', 'Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi'];
      return days[date.getDay()] || '';
    } catch {
      return '';
    }
  }

  selectInterviewDay(index: number): void {
    this.selectedInterviewDayIndex.set(index);
  }

  get selectedInterviewDay(): InterviewDayData | null {
    return this.interviewWeekData[this.selectedInterviewDayIndex()] ?? null;
  }

  get selectedInterviewDayCount(): number {
    return this.selectedInterviewDayPendingInterviews().length;
  }

  getInterviewDayPendingCount(dayData: InterviewDayData): number {
    return dayData.interviews.filter((interview) => interview.status === 'PENDING').length;
  }

  selectedInterviewDayPendingInterviews(): AdminCourierItem[] {
    return this.selectedInterviewDay?.interviews.filter((interview) => interview.status === 'PENDING') ?? [];
  }

  private isWeekdayLabel(dayLabel: string): boolean {
    return ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi'].includes(dayLabel);
  }

  private convertInterviewToCourier(interview: any): AdminCourierItem {
    const matchedCourier = this.findCourierByUserId(interview?.userId);
    return {
      courierId: matchedCourier?.courierId ?? interview.userId ?? 0,
      userId: matchedCourier?.userId ?? interview.userId ?? 0,
      nom: matchedCourier?.nom ?? interview.nom ?? '',
      prenom: matchedCourier?.prenom ?? interview.prenom ?? '',
      email: matchedCourier?.email ?? interview.email ?? '',
      phoneNumber: matchedCourier?.phoneNumber ?? interview.phoneNumber ?? '',
      status: this.normalizeCourierStatus(matchedCourier?.status ?? interview.status),
      quizResult: this.normalizeQuizResult(interview.status),
      quizScore: interview.score || null,
      meetingLink: interview.meetingLink || '',
      interviewDate: interview.interviewScheduledAt || '',
      interviewDateInput: this.toDatetimeLocalValue(interview.interviewScheduledAt || ''),
      voitures: [],
      permitImage: ''
    };
  }

  private findCourierByUserId(userId: number | null | undefined): AdminCourierItem | null {
    if (typeof userId !== 'number') {
      return null;
    }

    return this.couriers.find((courier) => courier.userId === userId) ?? null;
  }

// ajouter une méthode pour mettre à jour le statut du livreur

  onCourierStatusSelectionChange(courier: AdminCourierItem, nextStatus: string): void {
    if (!this.isCourierStatus(nextStatus)) {
      this.couriersError.set('Statut courier invalide.');
      return;
    }

    if (courier.status === nextStatus) {
      return;
    }

    this.courierStatusMessage.set('');
    this.couriersError.set('');
    this.updatingCourierStatusIds.add(courier.courierId);

    this.deliveryApi.updateAdminCourierStatus(courier.courierId, nextStatus).subscribe({
      next: (updatedCourier) => {
        const resolvedStatus = this.normalizeCourierStatus(updatedCourier.status ?? nextStatus);
        this.syncCourierStatus(courier.userId, courier.courierId, resolvedStatus);
        this.syncInterviewCalendarCourierStatus(courier.userId, resolvedStatus);
        this.courierStatusMessage.set(`Statut mis a jour pour ${this.getCourierDisplayName(courier)}.`);
        this.forceViewUpdate();
      },
      error: (error) => {
        const message = error?.error?.message ?? error?.message ?? 'Impossible de mettre a jour le statut du livreur.';
        this.couriersError.set(message);
      },
      complete: () => {
        this.updatingCourierStatusIds.delete(courier.courierId);
      }
    });
  }

  private syncInterviewCalendarCourierStatus(userId: number, status: CourierStatus): void {
    for (const dayData of this.interviewWeekData) {
      for (const interview of dayData.interviews) {
        if (interview.userId === userId) {
          interview.status = status;
        }
      }
    }

    if (this.selectedInterviewDayIndex() >= this.interviewWeekData.length) {
      this.selectedInterviewDayIndex.set(this.interviewWeekData.length > 0 ? 0 : -1);
    }
  }

  private syncCourierStatus(userId: number, courierId: number, status: CourierStatus): void {
    for (const item of this.couriers) {
      if (item.userId === userId || item.courierId === courierId) {
        item.status = status;
      }
    }
  }

  isCourierStatusUpdating(courierId: number): boolean {
    return this.updatingCourierStatusIds.has(courierId);
  }

  isInterviewDateUpdating(courierId: number): boolean {
    return this.updatingInterviewDateIds.has(courierId);
  }

  addInterviewDate(courier: AdminCourierItem): void {
    const interviewDate = (courier.interviewDateInput ?? '').trim();
    if (!interviewDate) {
      this.couriersError.set('Choisissez une date et une heure pour l entretien.');
      return;
    }

    this.courierStatusMessage.set('');
    this.couriersError.set('');
    this.updatingInterviewDateIds.add(courier.courierId);

    this.deliveryApi.updateAdminCourierInterviewDate(
      courier.courierId,
      interviewDate,
      true,
      this.interviewDateNotificationMessage
    ).subscribe({
      next: (updatedCourier) => {
        const savedInterviewDate = this.readOptionalString(updatedCourier, ['interviewDate', 'interview_date', 'dateEntretien']) || interviewDate;
        courier.interviewDate = savedInterviewDate;
        courier.interviewDateInput = this.toDatetimeLocalValue(savedInterviewDate);
        this.courierStatusMessage.set(`Date d entretien ajoutee et mail envoye pour ${this.getCourierDisplayName(courier)}.`);
      },
      error: (error) => {
        const message = error?.error?.message ?? error?.message ?? 'Impossible d ajouter la date d entretien.';
        this.couriersError.set(message);
      },
      complete: () => {
        this.updatingInterviewDateIds.delete(courier.courierId);
      }
    });
  }

  getCourierStatusLabel(status: CourierStatus): string {
    switch (status) {
      case 'ACCEPTED':
        return 'ACCEPTED';
      case 'REFUSED':
        return 'REFUSED';
      case 'PENDING':
      default:
        return 'PENDING';
    }
  }

  private isCourierStatus(value: string): value is CourierStatus {
    return value === 'PENDING' || value === 'ACCEPTED' || value === 'REFUSED';
  }

  private normalizeCourierStatus(status: string | null | undefined): CourierStatus {
    if (status === 'ACCEPTED' || status === 'REFUSED' || status === 'PENDING') {
      return status;
    }

    return 'PENDING';
  }

  getCourierDisplayName(courier: AdminCourierItem): string {
    const fullName = `${courier.nom ?? ''} ${courier.prenom ?? ''}`.trim();
    return fullName || courier.email || `Courier #${courier.courierId}`;
  }

  getCourierQuizResultLabel(value: string): string {
    const normalized = (value ?? '').trim();
    if (normalized === 'ACCEPTED_QUIZ') {
      return 'ACCEPTED';
    }
    if (normalized === 'REJECTED') {
      return 'REFUSED';
    }
    if (normalized === 'PENDING') {
      return 'PENDING';
    }
    return normalized || '-';
  }

  formatInterviewDate(value: string): string {
    const raw = (value ?? '').trim();
    if (!raw) {
      return '-';
    }

    const parsed = new Date(raw);
    if (Number.isNaN(parsed.getTime())) {
      return raw;
    }

    return parsed.toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  private toDatetimeLocalValue(value: string): string {
    const raw = (value ?? '').trim();
    if (!raw) {
      return '';
    }

    const parsed = new Date(raw);
    if (Number.isNaN(parsed.getTime())) {
      return '';
    }

    const pad = (num: number): string => String(num).padStart(2, '0');
    const year = parsed.getFullYear();
    const month = pad(parsed.getMonth() + 1);
    const day = pad(parsed.getDate());
    const hours = pad(parsed.getHours());
    const minutes = pad(parsed.getMinutes());

    return `${year}-${month}-${day}T${hours}:${minutes}`;
  }

  private normalizeQuizResult(status: string | null | undefined): string {
    const normalized = (status ?? '').trim().toUpperCase();
    if (normalized === 'ACCEPTED_QUIZ' || normalized === 'REJECTED' || normalized === 'PENDING') {
      return normalized;
    }

    return '';
  }

  private matchesCourierPhoneFilter(courier: AdminCourierItem): boolean {
    const term = this.interviewPhoneFilterTerm.trim().toLowerCase();
    if (!term) {
      return true;
    }

    return (courier.phoneNumber ?? '').toLowerCase().includes(term);
  }

  private readOptionalString(source: object, keys: string[]): string {
    const record = source as Record<string, unknown>;
    for (const key of keys) {
      const value = record[key];
      if (typeof value === 'string' && value.trim().length > 0) {
        return value.trim();
      }
    }

    return '';
  }

  private readOptionalNumber(source: object, keys: string[]): number | null {
    const record = source as Record<string, unknown>;
    for (const key of keys) {
      const value = record[key];
      if (typeof value === 'number' && Number.isFinite(value)) {
        return value;
      }
      if (typeof value === 'string' && value.trim().length > 0) {
        const parsed = Number(value);
        if (Number.isFinite(parsed)) {
          return parsed;
        }
      }
    }

    return null;
  }

  resolvePermitImageSrc(permitImage: string | null | undefined): string | null {
    if (!permitImage) {
      return null;
    }

    const trimmed = permitImage.trim();
    if (!trimmed) {
      return null;
    }

    if (trimmed.startsWith('data:') || trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
      return trimmed;
    }

    return `data:image/jpeg;base64,${trimmed}`;
  }

  getVehicleOwnerNom(vehicle: VehicleItem): string {
    return this.getVehicleOwnerInfo(vehicle).nom;
  }

  getVehicleOwnerPrenom(vehicle: VehicleItem): string {
    return this.getVehicleOwnerInfo(vehicle).prenom;
  }

  getVehiclePhotoUrl(vehicleId: number): string | null {
    return this.vehiclePhotoUrls[vehicleId] ?? this.vehicleInlinePhotoUrls[vehicleId] ?? null;
  }

  getVehicleRegistrationFrontUrl(vehicleId: number): string | null {
    return this.vehicleRegistrationFrontUrls[vehicleId] ?? this.vehicleInlineRegistrationFrontUrls[vehicleId] ?? null;
  }

  getVehicleRegistrationBackUrl(vehicleId: number): string | null {
    return this.vehicleRegistrationBackUrls[vehicleId] ?? this.vehicleInlineRegistrationBackUrls[vehicleId] ?? null;
  }

  isVehicleDocumentLoading(vehicleId: number): boolean {
    return this.vehicleDocumentsLoadingIds.has(vehicleId);
  }

  openImagePreview(imageUrl: string | null, label: string): void {
    if (!imageUrl) {
      return;
    }

    this.selectedPreviewImageUrl.set(imageUrl);
    this.selectedPreviewImageLabel.set(label);
  }

  closeImagePreview(): void {
    this.selectedPreviewImageUrl.set(null);
    this.selectedPreviewImageLabel.set('');
  }

  private loadOrders(): void {
    this.marketplaceApi.orders.list().subscribe({
      next: (orders) => {
        this.orders = orders.map((order) => ({
          ...(() => {
            const orderRecord = order as unknown as Record<string, unknown>;
            const paymentMethodFromApi =
              (typeof orderRecord['paymentMethod'] === 'string' ? orderRecord['paymentMethod'] : null) ??
              (typeof orderRecord['payment_method'] === 'string' ? orderRecord['payment_method'] : null) ??
              'N/A';

            const shippingAddressFromApi =
              (typeof orderRecord['shipping_address'] === 'string' ? orderRecord['shipping_address'] : null) ??
              (typeof orderRecord['shippingAddress'] === 'string' ? orderRecord['shippingAddress'] : null) ??
              (typeof orderRecord['address'] === 'string' ? orderRecord['address'] : null) ??
              'N/A';

            return {
              paymentMethod: paymentMethodFromApi,
              shipping_address: shippingAddressFromApi
            };
          })(),
          id: order.id,
          customer: order.user?.name ?? 'Customer',
          amount: order.totalAmount,
          status: order.status,
          date: order.date
        }));
        this.orderOptions = this.orders.map((order) => ({
          id: order.id,
          label: `#${order.id} - ${order.customer} (${order.amount} DT)`,
          shippingAddress: order.shipping_address
        }));
        this.forceViewUpdate();
      },
      error: () => {
        this.orders = [];
        this.orderOptions = [];
        this.forceViewUpdate();
      }
    });
  }

  private loadDeliveries(): void {
    this.deliveryApi.deliveries.list().subscribe({
      next: (deliveries) => {
        this.deliveries = deliveries.map((delivery) => ({
          ...(() => {
            const record = delivery as unknown as Record<string, unknown>;
            const readOptionalNumber = (...keys: string[]): number | null => {
              for (const key of keys) {
                const raw = record[key];
                if (typeof raw === 'number' && Number.isFinite(raw)) {
                  return raw;
                }
                if (typeof raw === 'string' && raw.trim()) {
                  const parsed = Number(raw);
                  if (Number.isFinite(parsed)) {
                    return parsed;
                  }
                }
              }
              return null;
            };

            return {
              city: typeof record['city'] === 'string' ? record['city'] : undefined,
              postalCode: typeof record['postalCode'] === 'string' ? record['postalCode'] : undefined,
              phoneNumber: typeof record['phoneNumber'] === 'string' ? record['phoneNumber'] : undefined,
              deliveryMode: typeof record['deliveryMode'] === 'string' ? record['deliveryMode'] : undefined,
              paymentMode: typeof record['paymentMode'] === 'string' ? record['paymentMode'] : undefined,
              distanceKm: readOptionalNumber('distanceKm', 'distance_km', 'distance'),
              realWeightKg: readOptionalNumber('realWeight', 'realWeightKg', 'poidsReel', 'poids_reel'),
              volumetricWeightKg: readOptionalNumber('volumetricWeight', 'volumetricWeightKg', 'poidsVolumetrique', 'poids_volumetrique'),
              billableWeightKg: readOptionalNumber('billableWeight', 'billableWeightKg', 'poidsFacturable', 'poids_facturable')
            };
          })(),
          id: delivery.id,
          code: `DEL${delivery.id}`,
          deliverytype: delivery.deliverytype,
          status: delivery.status,
          cancellationReason: delivery.cancellationReason,
          deliverydate: delivery.deliverydate ? delivery.deliverydate.split('T')[0] : '',
          address: delivery.address,
          orderId: delivery.order?.id,
          vehiculeId: delivery.vehicule?.id ?? null
        }));
        this.loadQrCodesForDeliveries();
        this.syncAssignments();
        this.forceViewUpdate();
      },
      error: () => {
        this.deliveries = [];
        this.forceViewUpdate();
      }
    });
  }

  private loadQrCodesForDeliveries(): void {
    this.qrCodeUrls = {};
    this.deliveries.forEach((delivery) => {
      this.deliveryApi.getDeliveryQrCode(delivery.id).subscribe({
        next: (blob) => {
          const objectUrl = URL.createObjectURL(blob);
          this.qrCodeUrls[delivery.id] = objectUrl;
        },
        error: (error) => {
          console.error(`Failed to load QR code for delivery ${delivery.id}:`, error);
          this.qrCodeUrls[delivery.id] = '';
        }
      });
    });
  }

  private deleteDelivery(deliveryId: number): void {
    const deliveryToDelete = this.deliveries.find((delivery) => delivery.id === deliveryId);
    const orderId = deliveryToDelete?.orderId;

    this.deliveryApi.deliveries.delete(deliveryId).subscribe({
      next: () => {
        if (typeof orderId === 'number') {
          this.marketplaceApi.orders.delete(orderId).subscribe({
            next: () => {
              this.refreshData();
            },
            error: () => {
              this.refreshData();
            }
          });
          return;
        }

        this.refreshData();
      },
      error: () => {
        this.assignmentError.set('Failed to delete delivery.');
      }
    });
  }

  private toPayload(formData: DeliveryFormData): DeliveryWritePayload {
    return {
      deliverytype: formData.deliverytype,
      status: formData.status,
      cancellationReason: formData.cancellationReason?.trim() || undefined,
      deliverydate: formData.deliverydate,
      address: formData.address,
      orderId: formData.orderId ?? null
    };
  }

  private loadVehicles(): void {
    this.deliveryApi.listAllVehicules().subscribe({
      next: (vehicules) => {
        this.vehicles = vehicules.map((vehicule) => ({
          ...(() => {
            const vehiculeRecord = vehicule as unknown as Record<string, unknown>;
            const vehiclePhotoFileName = this.readOptionalString(vehiculeRecord, [
              'vehiclePhotoFileName',
              'vehicle_photo_file_name',
              'vehiclePhotoFilename'
            ]);
            const registrationCardFrontFileName = this.readOptionalString(vehiculeRecord, [
              'registrationCardFrontFileName',
              'registration_card_front_file_name',
              'registrationCardFrontFilename'
            ]);
            const registrationCardBackFileName = this.readOptionalString(vehiculeRecord, [
              'registrationCardBackFileName',
              'registration_card_back_file_name',
              'registrationCardBackFilename'
            ]);

            this.vehicleInlinePhotoUrls[vehicule.id] = this.resolveVehicleInlineImageSrc(this.readOptionalString(vehiculeRecord, [
              'vehiclePhoto',
              'vehicle_photo',
              'vehiclePhotoBase64',
              'vehicle_photo_base64',
              'vehiclePhotoUrl',
              'vehicle_photo_url'
            ]));

            this.vehicleInlineRegistrationFrontUrls[vehicule.id] = this.resolveVehicleInlineImageSrc(this.readOptionalString(vehiculeRecord, [
              'registrationCardFront',
              'registration_card_front',
              'registrationCardFrontBase64',
              'registration_card_front_base64',
              'registrationCardFrontUrl',
              'registration_card_front_url'
            ]));

            this.vehicleInlineRegistrationBackUrls[vehicule.id] = this.resolveVehicleInlineImageSrc(this.readOptionalString(vehiculeRecord, [
              'registrationCardBack',
              'registration_card_back',
              'registrationCardBackBase64',
              'registration_card_back_base64',
              'registrationCardBackUrl',
              'registration_card_back_url'
            ]));

            return {
              vehiclePhotoFileName: vehiclePhotoFileName || undefined,
              registrationCardFrontFileName: registrationCardFrontFileName || undefined,
              registrationCardBackFileName: registrationCardBackFileName || undefined
            };
          })(),
          id: vehicule.id,
          licensePlate: vehicule.registrationnumbers,
          type: vehicule.type,
          status: vehicule.status,
          capacity: vehicule.capacity
        }));
        this.preloadVehicleDocuments(this.vehicles);
        this.syncAssignments();
        this.forceViewUpdate();
      },
      error: () => {
        this.vehicles = [];
        this.forceViewUpdate();
      }
    });
  }

  private preloadVehicleDocuments(vehicles: VehicleItem[]): void {
    vehicles.forEach((vehicle) => {
      if (this.vehicleDocumentsLoadingIds.has(vehicle.id)) {
        return;
      }

      const shouldLoadPhoto = !this.getVehiclePhotoUrl(vehicle.id)
        && (Boolean(vehicle.vehiclePhotoFileName) || this.vehiclePhotoUrls[vehicle.id] === undefined);
      const shouldLoadFront = !this.getVehicleRegistrationFrontUrl(vehicle.id)
        && (Boolean(vehicle.registrationCardFrontFileName) || this.vehicleRegistrationFrontUrls[vehicle.id] === undefined);
      const shouldLoadBack = !this.getVehicleRegistrationBackUrl(vehicle.id)
        && (Boolean(vehicle.registrationCardBackFileName) || this.vehicleRegistrationBackUrls[vehicle.id] === undefined);

      if (!shouldLoadPhoto && !shouldLoadFront && !shouldLoadBack) {
        return;
      }

      this.vehicleDocumentsLoadingIds.add(vehicle.id);

      forkJoin({
        photo: this.deliveryApi.getVehiclePhoto(vehicle.id).pipe(catchError(() => of(null))),
        registrationFront: this.deliveryApi.getRegistrationCardFront(vehicle.id).pipe(catchError(() => of(null))),
        registrationBack: this.deliveryApi.getRegistrationCardBack(vehicle.id).pipe(catchError(() => of(null)))
      }).subscribe({
        next: (documents) => {
          if (shouldLoadPhoto) {
            this.vehiclePhotoUrls[vehicle.id] = documents.photo ? URL.createObjectURL(documents.photo) : null;
          }
          if (shouldLoadFront) {
            this.vehicleRegistrationFrontUrls[vehicle.id] = documents.registrationFront ? URL.createObjectURL(documents.registrationFront) : null;
          }
          if (shouldLoadBack) {
            this.vehicleRegistrationBackUrls[vehicle.id] = documents.registrationBack ? URL.createObjectURL(documents.registrationBack) : null;
          }
          this.forceViewUpdate();
        },
        complete: () => {
          this.vehicleDocumentsLoadingIds.delete(vehicle.id);
        }
      });
    });
  }

  private getVehicleOwnerInfo(vehicle: VehicleItem): VehicleOwnerInfo {
    const normalizedSeries = this.normalizeVehicleSeries(vehicle.licensePlate);
    if (!normalizedSeries) {
      return { nom: '-', prenom: '-' };
    }

    const owner = this.vehicleOwnerBySeries.get(normalizedSeries);
    if (!owner) {
      return { nom: '-', prenom: '-' };
    }

    return owner;
  }

  private rebuildVehicleOwnerMap(couriers: AdminCourierItem[]): void {
    this.vehicleOwnerBySeries.clear();

    couriers.forEach((courier) => {
      (courier.voitures ?? []).forEach((voiture) => {
        const series = this.getCourierVehicleSeries(voiture);
        const normalizedSeries = this.normalizeVehicleSeries(series);
        if (!normalizedSeries) {
          return;
        }

        this.vehicleOwnerBySeries.set(normalizedSeries, {
          nom: (courier.nom ?? '').trim() || '-',
          prenom: (courier.prenom ?? '').trim() || '-'
        });
      });
    });
  }

  private getCourierVehicleSeries(voiture: VehicleInfoDto): string {
    const record = voiture as unknown as Record<string, unknown>;
    const series =
      (typeof record['serie'] === 'string' ? record['serie'] : null)
      ?? (typeof record['registrationnumbers'] === 'string' ? record['registrationnumbers'] : null)
      ?? (typeof record['registrationNumber'] === 'string' ? record['registrationNumber'] : null)
      ?? (typeof record['licensePlate'] === 'string' ? record['licensePlate'] : null)
      ?? '';

    return series.trim();
  }

  private normalizeVehicleSeries(value: string | null | undefined): string {
    return (value ?? '').trim().toLowerCase().replace(/\s+/g, '');
  }

  private resolveVehicleInlineImageSrc(imageValue: string | null | undefined): string | null {
    const value = (imageValue ?? '').trim();
    if (!value) {
      return null;
    }

    if (value.startsWith('data:') || value.startsWith('http://') || value.startsWith('https://') || value.startsWith('blob:')) {
      return value;
    }

    return `data:image/jpeg;base64,${value}`;
  }

  assignDeliveryToVehicle(delivery: DeliveryItem): void {
    this.assignmentError.set('');
    this.assignmentMessage.set('');

    const vehicleId = this.selectedVehicleByDelivery[delivery.id];
    if (!vehicleId) {
      this.assignmentError.set('Select a vehicle before assigning.');
      return;
    }

    const vehicle = this.availableVehicles.find((item) => item.id === vehicleId);
    if (!vehicle) {
      this.assignmentError.set('Selected vehicle is not available.');
      return;
    }

    const payload = this.buildAssignmentPayload(delivery, vehicle.id);

    this.deliveryApi.deliveries.update(delivery.id, payload).subscribe({
      next: (updatedDelivery) => {
        // Synchronize order status with delivery status
        this.syncOrderStatus(updatedDelivery);
        delivery.vehiculeId = vehicle.id;
        this.syncAssignments();
        this.forceViewUpdate();
        this.selectedVehicleByDelivery[delivery.id] = null;
        this.assignmentMessage.set('Delivery assigned successfully.');
        this.loadDeliveries();
      },
      error: (error) => {
        const message = error?.error?.message ?? error?.message ?? 'Failed to assign delivery';
        this.assignmentError.set(message);
      }
    });
  }

  removeAssignment(deliveryId: number): void {
    this.assignmentError.set('');
    this.assignmentMessage.set('');
    const delivery = this.deliveries.find((item) => item.id === deliveryId);
    if (!delivery) {
      this.assignmentError.set('Delivery not found.');
      return;
    }

    const payload = this.buildAssignmentPayload(delivery, null);
    this.deliveryApi.deliveries.update(delivery.id, payload).subscribe({
      next: (updatedDelivery) => {
        // Synchronize order status with delivery status
        this.syncOrderStatus(updatedDelivery);
        this.assignmentMessage.set('Assignment removed.');
        this.loadDeliveries();
      },
      error: (error) => {
        const message = error?.error?.message ?? error?.message ?? 'Failed to remove assignment';
        this.assignmentError.set(message);
      }
    });
  }

  updateAssignmentVehicle(assignment: DeliveryAssignmentItem): void {
    this.assignmentError.set('');
    this.assignmentMessage.set('');

    const delivery = this.deliveries.find((item) => item.id === assignment.deliveryId);
    if (!delivery) {
      this.assignmentError.set('Delivery not found.');
      return;
    }

    const newVehicleId = this.selectedVehicleByAssignment[assignment.deliveryId];
    if (!newVehicleId) {
      this.assignmentError.set('Select a vehicle before updating.');
      return;
    }

    const vehicle = this.availableVehicles.find((item) => item.id === newVehicleId);
    if (!vehicle) {
      this.assignmentError.set('Selected vehicle is not available.');
      return;
    }

    const payload = this.buildAssignmentPayload(delivery, vehicle.id);
    this.deliveryApi.deliveries.update(delivery.id, payload).subscribe({
      next: (updatedDelivery) => {
        // Synchronize order status with delivery status
        this.syncOrderStatus(updatedDelivery);
        this.assignmentMessage.set('Assignment updated.');
        this.loadDeliveries();
      },
      error: (error) => {
        const message = error?.error?.message ?? error?.message ?? 'Failed to update assignment';
        this.assignmentError.set(message);
      }
    });
  }

  isDeliveryAssigned(deliveryId: number): boolean {
    return this.assignments.some((assignment) => assignment.deliveryId === deliveryId);
  }

  getAssignedVehicleLabel(deliveryId: number): string {
    const assignment = this.assignments.find((item) => item.deliveryId === deliveryId);
    if (!assignment) {
      return '-';
    }
    return `${assignment.vehicleType} • ${assignment.vehicleLicensePlate}`;
  }

  openEditDeliveredAssignment(assignment: DeliveryAssignmentItem): void {
    const delivery = this.deliveries.find((item) => item.id === assignment.deliveryId);
    if (!delivery) {
      this.assignmentError.set('Delivery not found.');
      return;
    }

    this.openEditDeliveryModal(delivery);
  }

  get deliveredAssignments(): DeliveryAssignmentItem[] {
    const vehicleMap = new Map(this.vehicles.map((vehicle) => [vehicle.id, vehicle]));

    return this.deliveries
      .filter((delivery) => this.isDeliveredStatus(delivery.status))
      .map((delivery) => {
        const vehicle = delivery.vehiculeId ? vehicleMap.get(delivery.vehiculeId) : undefined;

        return {
          deliveryId: delivery.id,
          deliveryCode: delivery.code,
          deliveryType: delivery.deliverytype,
          deliveryDate: delivery.deliverydate,
          deliveryStatus: delivery.status,
          cancellationReason: delivery.cancellationReason,
          deliveryAddress: delivery.address,
          orderId: delivery.orderId,
          vehicleId: delivery.vehiculeId ?? 0,
          vehicleLicensePlate: vehicle?.licensePlate ?? '-',
          vehicleType: vehicle?.type ?? '-',
          vehicleStatus: vehicle?.status ?? '-',
          vehicleCapacity: vehicle?.capacity ?? 0
        };
      });
  }

  get filteredDeliveredAssignments(): DeliveryAssignmentItem[] {
    return this.filterAssignmentsByTerm(this.deliveredAssignments, this.deliveryManagementFilterTerm);
  }

  get cancelledAssignments(): DeliveryAssignmentItem[] {
    const vehicleMap = new Map(this.vehicles.map((vehicle) => [vehicle.id, vehicle]));

    return this.deliveries
      .filter((delivery) => this.isCancelledStatus(delivery.status))
      .map((delivery) => {
        const vehicle = delivery.vehiculeId ? vehicleMap.get(delivery.vehiculeId) : undefined;

        return {
          deliveryId: delivery.id,
          deliveryCode: delivery.code,
          deliveryType: delivery.deliverytype,
          deliveryDate: delivery.deliverydate,
          deliveryStatus: delivery.status,
          cancellationReason: delivery.cancellationReason,
          deliveryAddress: delivery.address,
          orderId: delivery.orderId,
          vehicleId: delivery.vehiculeId ?? 0,
          vehicleLicensePlate: vehicle?.licensePlate ?? '-',
          vehicleType: vehicle?.type ?? '-',
          vehicleStatus: vehicle?.status ?? '-',
          vehicleCapacity: vehicle?.capacity ?? 0
        };
      });
  }

  get filteredCancelledAssignments(): DeliveryAssignmentItem[] {
    return this.filterAssignmentsByTerm(this.cancelledAssignments, this.deliveryManagementFilterTerm);
  }

  get activeAssignments(): DeliveryAssignmentItem[] {
    return this.assignments.filter((assignment) => !this.isClosedStatus(assignment.deliveryStatus));
  }

  get filteredActiveAssignments(): DeliveryAssignmentItem[] {
    return this.filterAssignmentsByDeliveryId(this.activeAssignments, this.assignedDeliveryFilterTerm);
  }

  private syncAssignments(): void {
    const vehicleMap = new Map(this.vehicles.map((vehicle) => [vehicle.id, vehicle]));

    this.assignments = this.deliveries
      .filter((delivery) => delivery.vehiculeId)
      .map((delivery) => {
        const vehicle = delivery.vehiculeId ? vehicleMap.get(delivery.vehiculeId) : undefined;
        return {
          deliveryId: delivery.id,
          deliveryCode: delivery.code,
          deliveryType: delivery.deliverytype,
          deliveryDate: delivery.deliverydate,
          deliveryStatus: delivery.status,
          cancellationReason: delivery.cancellationReason,
          deliveryAddress: delivery.address,
          orderId: delivery.orderId,
          vehicleId: delivery.vehiculeId ?? 0,
          vehicleLicensePlate: vehicle?.licensePlate ?? 'Unknown',
          vehicleType: vehicle?.type ?? 'Unknown',
          vehicleStatus: vehicle?.status ?? 'Unknown',
          vehicleCapacity: vehicle?.capacity ?? 0
        };
      });

    this.assignments.forEach((assignment) => {
      if (this.selectedVehicleByAssignment[assignment.deliveryId] === undefined) {
        this.selectedVehicleByAssignment[assignment.deliveryId] = assignment.vehicleId;
      }
    });
  }

  private buildAssignmentPayload(delivery: DeliveryItem, vehicleId: number | null): DeliveryWritePayload {
    return {
      deliverytype: delivery.deliverytype,
      status: delivery.status,
      cancellationReason: delivery.cancellationReason,
      deliverydate: delivery.deliverydate,
      address: delivery.address,
      orderId: delivery.orderId ?? null,
      vehiculeId: vehicleId ?? null
    };
  }

  private isVehicleAvailable(vehicle: VehicleItem): boolean {
    return (vehicle.status ?? '').toUpperCase() === 'AVAILABLE';
  }

  private matchesVehicleTypeFilter(vehicle: VehicleItem): boolean {
    if (this.vehicleTypeFilter === 'ALL') {
      return true;
    }

    const normalizedType = (vehicle.type ?? '').trim().toUpperCase();
    if (this.vehicleTypeFilter === 'CAR') {
      return normalizedType.includes('CAR') || normalizedType.includes('VOITURE');
    }

    if (this.vehicleTypeFilter === 'MOTOR') {
      return normalizedType.includes('MOTOR') || normalizedType.includes('MOTO') || normalizedType.includes('BIKE');
    }

    return normalizedType.includes('TRUCK') || normalizedType.includes('CAMION');
  }

  private matchesVehicleRegistrationFilter(vehicle: VehicleItem): boolean {
    const term = this.vehicleRegistrationFilterTerm.trim().toLowerCase();
    if (!term) {
      return true;
    }

    const licensePlate = (vehicle.licensePlate ?? '').toLowerCase();
    return licensePlate.includes(term);
  }

  private matchesCourierEmailFilter(courier: AdminCourierItem): boolean {
    const term = this.courierEmailFilterTerm.trim().toLowerCase();
    if (!term) {
      return true;
    }

    const email = (courier.email ?? '').toLowerCase();
    return email.includes(term);
  }

  private matchesDeliveryType(delivery: DeliveryItem, expectedType: string): boolean {
    const normalizedType = (delivery.deliverytype ?? '').trim().toLowerCase().replace(/[-\s]+/g, '_');
    return normalizedType === expectedType;
  }

  private filterDeliveriesByTerm(deliveries: DeliveryItem[], rawTerm: string): DeliveryItem[] {
    const term = rawTerm.trim().toLowerCase();
    if (!term) {
      return deliveries;
    }

    return deliveries.filter((delivery) => {
      const deliveryCode = delivery.code ?? `DEL${delivery.id}`;
      const deliveryId = String(delivery.id);
      const orderId = delivery.orderId != null ? String(delivery.orderId) : '';

      const searchableText = [deliveryCode, deliveryId, orderId].join(' ').toLowerCase();
      return searchableText.includes(term);
    });
  }

  private filterAssignmentsByTerm(assignments: DeliveryAssignmentItem[], rawTerm: string): DeliveryAssignmentItem[] {
    const term = rawTerm.trim().toLowerCase();
    if (!term) {
      return assignments;
    }

    return assignments.filter((assignment) => {
      const deliveryCode = assignment.deliveryCode ?? `DEL${assignment.deliveryId}`;
      const deliveryId = String(assignment.deliveryId);
      const orderId = assignment.orderId != null ? String(assignment.orderId) : '';

      const searchableText = [deliveryCode, deliveryId, orderId].join(' ').toLowerCase();
      return searchableText.includes(term);
    });
  }

  private filterAssignmentsByDeliveryId(assignments: DeliveryAssignmentItem[], rawTerm: string): DeliveryAssignmentItem[] {
    const term = rawTerm.trim().toLowerCase();
    if (!term) {
      return assignments;
    }

    return assignments.filter((assignment) => {
      const deliveryCode = assignment.deliveryCode ?? `DEL${assignment.deliveryId}`;
      const deliveryId = String(assignment.deliveryId);
      const searchableText = [deliveryCode, deliveryId].join(' ').toLowerCase();
      return searchableText.includes(term);
    });
  }

  private isDeliveredStatus(status: string | null | undefined): boolean {
    return (status ?? '').trim().toUpperCase() === 'DELIVERED';
  }

  private isCancelledStatus(status: string | null | undefined): boolean {
    return (status ?? '').trim().toUpperCase() === 'CANCELLED';
  }

  private isClosedStatus(status: string | null | undefined): boolean {
    return this.isDeliveredStatus(status) || this.isCancelledStatus(status);
  }

  private isUnassignedDelivery(delivery: DeliveryItem): boolean {
    return delivery.vehiculeId == null;
  }

  private normalizeShippingAddress(address: string | null | undefined): string {
    return (address ?? '').trim().toLowerCase();
  }

  private getAddressColorIndex(normalizedAddress: string): number {
    let hash = 0;
    for (let index = 0; index < normalizedAddress.length; index += 1) {
      hash = (hash * 31 + normalizedAddress.charCodeAt(index)) >>> 0;
    }

    return hash % this.shippingAddressColorPalette.length;
  }

  /**
   * Synchronizes the order status with the delivery status.
   * When delivery status changes, the corresponding order's status is updated to match.
   * For example:
   * - Delivery status "In progress" → Order status "In progress"
   * - Delivery status "Delivered" → Order status "Delivered"
   * - Delivery status "Pending" → Order status "Pending"
   */
  private syncOrderStatus(delivery: any): void {
    // Check if the delivery has an associated order
    if (!delivery || !delivery.order || !delivery.order.id) {
      return;
    }

    const orderId = delivery.order.id;
    const deliveryStatus = delivery.status;

    // Prepare the order update payload with the delivery status
    const orderPayload: any = {
      status: deliveryStatus
    };

    // Update the order status via the marketplace API
    this.marketplaceApi.orders.update(orderId, orderPayload).subscribe({
      next: () => {
        // Update successful - order status is now synchronized with delivery status
        console.log(`Order #${orderId} status synchronized to: ${deliveryStatus}`);
      },
      error: (error) => {
        // Log error but don't block the delivery operation
        console.error(`Failed to synchronize order #${orderId} status:`, error);
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}

