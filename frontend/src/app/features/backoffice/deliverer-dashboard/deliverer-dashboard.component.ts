import { ChangeDetectorRef, Component, DestroyRef, ElementRef, NgZone, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { catchError, forkJoin, map, Observable, of } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FooterComponent } from '../../../shared/layout';
import { ModalComponent } from '../../../shared/components';
import { GoogleMap, GoogleMapsModule } from '@angular/google-maps';
import { DelivererQuizComponent } from './deliverer-quiz.component';
import {
  AuthService,
  ChatMessage,
  ChatService,
  CommonApiService,
  CourierProfileStatus,
  CourierStatisticsResponse,
  CourierStatus,
  CourierProfileResponse,
  DeliveryResource,
  DeliveryApiService,
  MarketplaceApiService,
  TopCourierResponse,
  UserResource,
  VehiculeMultipartPayload,
  VehiculeResource,
  VehiculeWritePayload,
  VerificationResponse
} from '../../../services';
import { UserRequest } from '../../../generated/model/userRequest';

interface DelivererDeliveryItem {
  id: string;
  orderLabel: string;
  from: string;
  to: string;
  distance: number;
  distanceKm?: number | null;
  price: number;
  priority: string;
  date?: string;
  vehicule?: string;
  deliverytype?: string;
  city?: string;
  postalCode?: string;
  phoneNumber?: string;
  deliveryMode?: string;
  paymentMode?: string;
  realWeightKg?: number | null;
  volumetricWeightKg?: number | null;
  billableWeightKg?: number | null;
  orderStatus?: string;
  status?: 'Accepted' | 'In progress' | 'Delivered' | 'Cancelled';
  receiverId?: string;
  userName?: string;
  cancellationReason?: string;
}

interface DuplicateDelivererAddressGroup {
  address: string;
  deliveries: DelivererDeliveryItem[];
}

interface VehicleDocumentPreview {
  vehiclePhotoUrl: string | null;
  registrationCardFrontUrl: string | null;
  registrationCardBackUrl: string | null;
}

interface CourierOverviewStats {
  totalDeliveries: number;
  deliveredDeliveries: number;
  completionRate: number;
  totalDistanceKm: number;
  pendingDeliveries: number;
  cancelledDeliveries: number;
  totalVehicles: number;
}

interface StatusDistributionItem {
  label: string;
  count: number;
  percent: number;
}

@Component({
  selector: 'app-delivery-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, FooterComponent, ModalComponent, GoogleMapsModule, DelivererQuizComponent],
  templateUrl: './deliverer-dashboard.component.html',
  styleUrls: ['./deliverer-dashboard.component.css']
})
export class DeliveryDashboardComponent implements OnInit, OnDestroy {
  private readonly deliveryApi = inject(DeliveryApiService);
  private readonly marketplaceApi = inject(MarketplaceApiService);
  private readonly commonApi = inject(CommonApiService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly ngZone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  readonly authService = inject(AuthService);
  showQuiz: boolean = false;

  activeTab = 'overview';
  deliveryView: 'pending' | 'delivered' | 'cancelled' = 'pending';
  vehicleStatusFilter: 'ALL' | 'AVAILABLE' | 'UNAVAILABLE' = 'ALL';
  vehicleTypeFilter: 'ALL' | 'CAR' | 'MOTOR' | 'TRUCK' = 'ALL';
  vehicleRegistrationFilter = '';
  currentVehiclePage = 1;
  readonly vehiclesPageSize = 5;
  deliveryIdFilter = '';
  currentPendingDeliveryPage = 1;
  currentDeliveredDeliveryPage = 1;
  currentCancelledDeliveryPage = 1;
  readonly deliveryTablePageSize = 5;

  pendingDeliveries: DelivererDeliveryItem[] = [];
  deliveredDeliveries: DelivererDeliveryItem[] = [];
  cancelledDeliveries: DelivererDeliveryItem[] = [];
  cancellingDeliveryId: string | null = null;
  cancellationReason = '';
  private allDeliveriesSource: DeliveryResource[] = [];
  private pendingDeliveriesSource: DeliveryResource[] = [];
  private readonly chatService = inject(ChatService);

  chatOpenDeliveryId: string | null = null;
  actionsPanelOpenDeliveryId: string | null = null;
  chatMessages: ChatMessage[] = [];
  chatDraft = '';
  chatLoading = false;
  chatErrorMessage = '';
  courierStatistics: CourierStatisticsResponse | null = null;
  isCourierStatisticsLoading = false;
  courierStatisticsError = '';
  topCouriers: TopCourierResponse[] = [];
  isTopCouriersLoading = false;
  topCouriersError = '';
  deliveryQrCodeUrls: Record<string, string> = {};
  isDeliveryDetailsModalOpen = false;
  selectedDeliveryDetails: DelivererDeliveryItem | null = null;
  qrScanInput = '';
  qrScanSuccess = '';
  qrScanError = '';
  isQrScanSubmitting = false;
  private delivererVehicleIds = new Set<number>();

  profileUser: UserResource | null = null;
  courierProfile: CourierProfileResponse | null = null;
  profileForm = {
    name: '',
    email: '',
    phoneNumber: '',
    password: ''
  };
  isProfileModalOpen = false;
  isProfileSubmitting = false;
  isPhoneNumberSaving = false;
  phoneNumberSaveError = '';
  phoneNumberSaveSuccess = '';
  permitUploadError = '';
  permitUploadSuccess = '';
  isPermitUploading = false;
  selectedPermitFile: File | null = null;
  permitPreviewUrl: string | null = null;
  readonly maxPermitSizeBytes = 5 * 1024 * 1024;

  verificationError = '';
  isPermitVerificationSubmitting = false;
  selectedSelfieFile: File | null = null;
  verificationResult: VerificationResponse | null = null;

  // Stepper properties
  verifySteps = ['Permit', 'Selfie', 'Verification'];
  verifyStep = 0;
  selfiePreviewUrl: string | null = null;
  isCameraOpen = false;
  cameraStream: MediaStream | null = null;
  cameraError = '';

  vehicles: VehiculeResource[] = [];
  vehicleForm!: FormGroup;
  vehicleTypes = ['MOTOR', 'CAR', 'TRUCK'];
  isVehicleModalOpen = false;
  isVehicleSubmitting = false;
  vehicleErrorMessage = '';
  vehicleSuccessMessage = '';
  private readonly duplicateVehicleSeriesMessage = 'A vehicle with this serial number already exists.';
  private readonly maxVehicleFileSizeBytes = 5 * 1024 * 1024;
  selectedVehiclePhotoFile: File | null = null;
  selectedRegistrationCardFrontFile: File | null = null;
  selectedRegistrationCardBackFile: File | null = null;
  vehiclePhotoError = '';
  registrationCardFrontError = '';
  registrationCardBackError = '';
  private vehicleDocumentPreviews = new Map<number, VehicleDocumentPreview>();
  selectedVehicleImageUrl: string | null = null;
  selectedVehicleImageTitle = '';
  editingVehicleId: number | null = null;
  private geoWatchId: number | null = null;
  @ViewChild(GoogleMap) mapComponent?: GoogleMap;
  @ViewChild('cameraVideo') cameraVideo?: ElementRef<HTMLVideoElement>;
  private nativeRoutePolyline: google.maps.Polyline | null = null;
  private currentPositionMarker: google.maps.Marker | null = null;
  private destinationMarker: google.maps.Marker | null = null;

  ngOnInit(): void {
    this.initVehicleForm();
    this.loadPendingDeliveries();
    this.loadProfile();
    this.loadCourierProfile();
    this.loadTopCouriers();
    this.loadVehicles();
    this.requestCurrentLocation();
  }

  ngOnDestroy(): void {
    if (this.geoWatchId !== null && typeof navigator !== 'undefined' && navigator.geolocation) {
      navigator.geolocation.clearWatch(this.geoWatchId);
      this.geoWatchId = null;
    }

    this.clearPermitPreview();
    this.clearSelfiePreview();
    this.clearAllVehicleDocumentPreviews();
    this.clearDeliveryQrCodeUrls();
  }

  private initVehicleForm(): void {
    this.vehicleForm = this.fb.group({
      type: ['', [
        Validators.required,
        Validators.pattern(/^(MOTOR|CAR|TRUCK|motor|car|truck)$/)
      ]],
      registrationnumbers: ['', [
        Validators.required,
        Validators.pattern(/^(\d{5}|\d{3}\sTU\s\d{4})$/)
      ]],
      capacity: [null, [
        Validators.required,
        Validators.min(0.0001)
      ]],
      status: ['', [
        Validators.required,
        Validators.pattern(/^(AVAILABLE|UNAVAILABLE|available|unavailable)$/)
      ]]
    });
  }

  get f() {
    return this.vehicleForm.controls;
  }

  get isVehicleSubmitDisabled(): boolean {
    if (this.vehicleForm.invalid) {
      return true;
    }

    if (this.editingVehicleId) {
      return false;
    }

    return !this.selectedVehiclePhotoFile
      || !this.selectedRegistrationCardFrontFile
      || !this.selectedRegistrationCardBackFile;
  }

  get filteredVehicles(): VehiculeResource[] {
    const registrationFilter = this.vehicleRegistrationFilter.trim().toLowerCase();

    return this.vehicles.filter((vehicle) => {
      const matchesStatus = this.vehicleStatusFilter === 'ALL' || vehicle.status === this.vehicleStatusFilter;
      const matchesType = this.vehicleTypeFilter === 'ALL' || vehicle.type === this.vehicleTypeFilter;
      const registrationNumber = String(vehicle.registrationnumbers ?? '').trim().toLowerCase();
      const matchesRegistration = !registrationFilter || registrationNumber.includes(registrationFilter);

      return matchesStatus && matchesType && matchesRegistration;
    });
  }

  get paginatedVehicles(): VehiculeResource[] {
    const startIndex = (this.currentVehiclePage - 1) * this.vehiclesPageSize;
    return this.filteredVehicles.slice(startIndex, startIndex + this.vehiclesPageSize);
  }

  get totalVehiclePages(): number {
    return Math.max(1, Math.ceil(this.filteredVehicles.length / this.vehiclesPageSize));
  }

  get hasVehiclePagination(): boolean {
    return this.filteredVehicles.length > this.vehiclesPageSize;
  }

  get vehiclePaginationStart(): number {
    if (this.filteredVehicles.length === 0) {
      return 0;
    }

    return (this.currentVehiclePage - 1) * this.vehiclesPageSize + 1;
  }

  get vehiclePaginationEnd(): number {
    return Math.min(this.currentVehiclePage * this.vehiclesPageSize, this.filteredVehicles.length);
  }

  onVehicleFiltersChanged(): void {
    this.currentVehiclePage = 1;
  }

  setVehiclePage(page: number): void {
    const nextPage = Math.min(Math.max(page, 1), this.totalVehiclePages);
    this.currentVehiclePage = nextPage;
  }

  isChatOpenForDelivery(delivery: DelivererDeliveryItem): boolean {
    return this.chatOpenDeliveryId === delivery.id;
  }

  async toggleDeliveryChat(delivery: DelivererDeliveryItem): Promise<void> {
    if (this.isChatOpenForDelivery(delivery)) {
      this.closeDeliveryChat();
      return;
    }

    await this.openDeliveryChat(delivery);
  }

  closeDeliveryChat(): void {
    this.chatOpenDeliveryId = null;
    this.chatMessages = [];
    this.chatDraft = '';
    this.chatLoading = false;
    this.chatErrorMessage = '';
  }

  isDeliveryActionsPanelOpen(delivery: DelivererDeliveryItem): boolean {
    return this.actionsPanelOpenDeliveryId === delivery.id;
  }

  toggleDeliveryActionsPanel(delivery: DelivererDeliveryItem): void {
    this.actionsPanelOpenDeliveryId = this.actionsPanelOpenDeliveryId === delivery.id ? null : delivery.id;
  }

  async sendChatMessageForDelivery(delivery: DelivererDeliveryItem): Promise<void> {
    if (!delivery.receiverId) {
      this.chatErrorMessage = 'Impossible d\'envoyer: destinataire introuvable.';
      return;
    }

    const content = this.chatDraft.trim();
    if (!content) {
      return;
    }

    this.chatDraft = '';
    this.chatMessages = [...this.chatMessages, this.buildLocalChatMessage(delivery.receiverId, content)];

    try {
      await this.chatService.sendMessage(delivery.receiverId, content);
    } catch (error) {
      this.chatErrorMessage = this.readChatErrorMessage(error, 'Impossible d\'envoyer le message.');
    }
  }

  private async openDeliveryChat(delivery: DelivererDeliveryItem): Promise<void> {
    this.chatErrorMessage = '';
    this.chatMessages = [];
    this.chatOpenDeliveryId = delivery.id;

    if (!delivery.receiverId) {
      this.chatLoading = false;
      this.chatErrorMessage = 'Impossible de déterminer l\'identifiant du destinataire.';
      return;
    }

    this.chatLoading = true;

    try {
      await this.chatService.ensureConnected();

      this.chatService.incomingMessages$
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((message) => {
          if (this.chatOpenDeliveryId !== delivery.id) {
            return;
          }

          if (message.senderId === delivery.receiverId || message.receiverId === delivery.receiverId) {
            this.chatMessages = [...this.chatMessages, message];
          }
        });

      this.chatService.getConversation(delivery.receiverId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (messages) => {
            if (this.chatOpenDeliveryId !== delivery.id) {
              return;
            }
            this.chatMessages = messages;
            this.chatLoading = false;
          },
          error: () => {
            if (this.chatOpenDeliveryId !== delivery.id) {
              return;
            }
            this.chatErrorMessage = 'Impossible de charger l\'historique du chat.';
            this.chatLoading = false;
          }
        });
    } catch (error) {
      this.chatLoading = false;
      this.chatErrorMessage = this.readChatErrorMessage(error, 'Impossible de se connecter au chat.');
    }
  }

  private buildLocalChatMessage(receiverId: string, content: string): ChatMessage {
    const senderId = this.authService.currentUser()?.email?.trim() || 'me';

    return {
      clientId: `local-${Date.now()}`,
      senderId,
      receiverId,
      content,
      sentAt: new Date().toISOString(),
      read: false
    };
  }

  private extractReceiverId(delivery: DeliveryResource): string | undefined {
    const userEmail = delivery.order?.user?.email?.trim();
    if (userEmail) {
      return userEmail;
    }

    if (delivery.connectedUserId) {
      return String(delivery.connectedUserId);
    }

    const userId = delivery.order?.user?.id;
    if (typeof userId === 'number' && userId > 0) {
      return String(userId);
    }

    const orderUserId = (delivery.order as any)?.userId;
    if (typeof orderUserId === 'number' && orderUserId > 0) {
      return String(orderUserId);
    }

    const orderUserId2 = (delivery.order as any)?.user_id;
    if (typeof orderUserId2 === 'number' && orderUserId2 > 0) {
      return String(orderUserId2);
    }

    // Try tracking phone number as a fallback for identifier
    if (delivery.phoneNumber) {
      return delivery.phoneNumber;
    }
    
    // Ultimate fallback for testing locally if no user is assigned
    return '1';
  }

  private readChatErrorMessage(error: unknown, fallback: string): string {
    if (error instanceof Error) {
      return error.message || fallback;
    }

    if (typeof error === 'string') {
      return error;
    }

    return fallback;
  }

  private matchesDeliveryIdFilter(deliveryId: string): boolean {
    const filter = this.deliveryIdFilter.trim().toLowerCase();
    if (!filter) {
      return true;
    }

    return deliveryId.toLowerCase().includes(filter);
  }

  // Orders with same address
  get duplicatePendingDeliveryAddressGroups(): DuplicateDelivererAddressGroup[] {
    const groupsByAddress = new Map<string, DuplicateDelivererAddressGroup>();

    for (const delivery of this.pendingDeliveries) {
      const normalizedAddress = this.normalizeDeliveryAddress(delivery.to);
      if (!normalizedAddress) {
        continue;
      }

      const existingGroup = groupsByAddress.get(normalizedAddress);
      if (existingGroup) {
        existingGroup.deliveries.push(delivery);
      } else {
        groupsByAddress.set(normalizedAddress, {
          address: delivery.to,
          deliveries: [delivery]
        });
      }
    }

    return Array.from(groupsByAddress.values())
      .filter((group) => group.deliveries.length > 1)
      .map((group) => ({
        ...group,
        deliveries: [...group.deliveries].sort((left, right) => left.id.localeCompare(right.id))
      }))
      .sort((left, right) => left.address.localeCompare(right.address));
  }

  get filteredDuplicatePendingDeliveryAddressGroups(): DuplicateDelivererAddressGroup[] {
    return this.duplicatePendingDeliveryAddressGroups
      .map((group) => ({
        ...group,
        deliveries: group.deliveries.filter((delivery) => this.matchesDeliveryIdFilter(delivery.id))
      }))
      .filter((group) => group.deliveries.length > 0);
  }

  get nonDuplicatePendingDeliveries(): DelivererDeliveryItem[] {
    const duplicateIds = new Set(
      this.duplicatePendingDeliveryAddressGroups.flatMap((group) => group.deliveries.map((delivery) => delivery.id))
    );

    return this.pendingDeliveries.filter((delivery) => !duplicateIds.has(delivery.id));
  }

  get filteredNonDuplicatePendingDeliveries(): DelivererDeliveryItem[] {
    return this.nonDuplicatePendingDeliveries.filter((delivery) => this.matchesDeliveryIdFilter(delivery.id));
  }

  get paginatedPendingDeliveries(): DelivererDeliveryItem[] {
    const startIndex = (this.currentPendingDeliveryPage - 1) * this.deliveryTablePageSize;
    return this.filteredNonDuplicatePendingDeliveries.slice(startIndex, startIndex + this.deliveryTablePageSize);
  }

  get pendingDeliveryTotalPages(): number {
    return Math.max(1, Math.ceil(this.filteredNonDuplicatePendingDeliveries.length / this.deliveryTablePageSize));
  }

  get filteredDeliveredDeliveries(): DelivererDeliveryItem[] {
    return this.deliveredDeliveries.filter((delivery) => this.matchesDeliveryIdFilter(delivery.id));
  }

  get paginatedDeliveredDeliveries(): DelivererDeliveryItem[] {
    const startIndex = (this.currentDeliveredDeliveryPage - 1) * this.deliveryTablePageSize;
    return this.filteredDeliveredDeliveries.slice(startIndex, startIndex + this.deliveryTablePageSize);
  }

  get deliveredDeliveryTotalPages(): number {
    return Math.max(1, Math.ceil(this.filteredDeliveredDeliveries.length / this.deliveryTablePageSize));
  }

  get filteredCancelledDeliveries(): DelivererDeliveryItem[] {
    return this.cancelledDeliveries.filter((delivery) => this.matchesDeliveryIdFilter(delivery.id));
  }

  get paginatedCancelledDeliveries(): DelivererDeliveryItem[] {
    const startIndex = (this.currentCancelledDeliveryPage - 1) * this.deliveryTablePageSize;
    return this.filteredCancelledDeliveries.slice(startIndex, startIndex + this.deliveryTablePageSize);
  }

  get cancelledDeliveryTotalPages(): number {
    return Math.max(1, Math.ceil(this.filteredCancelledDeliveries.length / this.deliveryTablePageSize));
  }

  get deliveryNotificationCount(): number {
    return this.pendingDeliveries.length;
  }

  get deliveredPackageCount(): number {
    return this.deliveredDeliveries.length;
  }

  get cancelledPackageCount(): number {
    return this.cancelledDeliveries.length;
  }

  get hasDeliveryAlert(): boolean {
    return this.deliveryNotificationCount > 0;
  }

  setDeliveryView(view: 'pending' | 'delivered' | 'cancelled'): void {
    this.deliveryView = view;
  }

  onDeliveryIdFilterChanged(): void {
    this.currentPendingDeliveryPage = 1;
    this.currentDeliveredDeliveryPage = 1;
    this.currentCancelledDeliveryPage = 1;
  }

  setPendingDeliveryPage(page: number): void {
    const nextPage = Math.min(Math.max(page, 1), this.pendingDeliveryTotalPages);
    this.currentPendingDeliveryPage = nextPage;
  }

  setDeliveredDeliveryPage(page: number): void {
    const nextPage = Math.min(Math.max(page, 1), this.deliveredDeliveryTotalPages);
    this.currentDeliveredDeliveryPage = nextPage;
  }

  setCancelledDeliveryPage(page: number): void {
    const nextPage = Math.min(Math.max(page, 1), this.cancelledDeliveryTotalPages);
    this.currentCancelledDeliveryPage = nextPage;
  }

  openProfileModal(): void {
    if (!this.profileUser) return;
    this.profileForm = {
      name: this.profileUser.name,
      email: this.profileUser.email,
      phoneNumber: this.courierProfile?.phoneNumber ?? '',
      password: ''
    };
    this.isProfileModalOpen = true;
  }

  closeProfileModal(): void {
    this.isProfileModalOpen = false;
    this.isProfileSubmitting = false;
    this.profileForm.password = '';
  }

  submitProfileUpdate(): void {
    if (!this.profileUser) return;
    const trimmedName = this.profileForm.name.trim();
    const trimmedEmail = this.profileForm.email.trim();
    const trimmedPhoneNumber = this.profileForm.phoneNumber.trim();

    if (!trimmedName || !trimmedEmail || !this.profileForm.password) return;

    this.isProfileSubmitting = true;

    forkJoin({
      user: this.commonApi.users.update(this.profileUser.id, {
        name: trimmedName,
        email: trimmedEmail,
        password: this.profileForm.password,
        role: this.profileUser.role ?? UserRequest.RoleEnum.Courier
      } as UserRequest),
      courierProfile: this.deliveryApi.updateMyCourierProfile({
        phoneNumber: trimmedPhoneNumber
      })
    })
      .subscribe({
        next: ({ user, courierProfile }) => {
          this.profileUser = user;
          this.courierProfile = courierProfile;
          this.authService.updateUser({ name: user.name, email: user.email });
          this.closeProfileModal();
        },
        error: () => {
          this.isProfileSubmitting = false;
        },
        complete: () => {
          this.isProfileSubmitting = false;
        }
      });
  }

  savePhoneNumberDirectly(): void {
    if (!this.profileUser) return;

    const trimmedPhoneNumber = this.profileForm.phoneNumber.trim();
    this.phoneNumberSaveError = '';
    this.phoneNumberSaveSuccess = '';
    this.isPhoneNumberSaving = true;

    this.deliveryApi.updateMyCourierProfile({
      phoneNumber: trimmedPhoneNumber
    }).subscribe({
      next: (courierProfile) => {
        this.courierProfile = courierProfile;
        this.profileForm.phoneNumber = courierProfile.phoneNumber ?? trimmedPhoneNumber;
        this.phoneNumberSaveSuccess = 'Phone number updated.';
        this.isPhoneNumberSaving = false;
      },
      error: () => {
        this.phoneNumberSaveError = 'Unable to update phone number.';
        this.isPhoneNumberSaving = false;
      }
    });
  }

  confirmDeleteProfile(): void {
    if (!this.profileUser) return;
    const confirmed = window.confirm('Are you sure you want to delete your account?');
    if (!confirmed) return;

    this.commonApi.users.delete(this.profileUser.id).subscribe({
      next: () => {
        this.profileUser = null;
        this.authService.logout();
      }
    });
  }

  openCreateVehicleModal(): void {
    this.editingVehicleId = null;
    this.vehicleForm.reset({
      type: 'CAR',
      registrationnumbers: '',
      capacity: null,
      status: 'AVAILABLE'
    });
    this.vehicleErrorMessage = '';
    this.vehicleSuccessMessage = '';
    this.clearSelectedVehicleFiles();
    this.clearVehicleDocumentErrors();
    this.isVehicleModalOpen = true;
  }

  openEditVehicleModal(vehicle: VehiculeResource): void {
    this.editingVehicleId = vehicle.id;
    this.vehicleForm.patchValue({
      type: vehicle.type,
      registrationnumbers: vehicle.registrationnumbers,
      capacity: vehicle.capacity,
      status: vehicle.status
    });
    this.vehicleErrorMessage = '';
    this.vehicleSuccessMessage = '';
    this.clearSelectedVehicleFiles();
    this.clearVehicleDocumentErrors();
    this.isVehicleModalOpen = true;
  }

  closeVehicleModal(): void {
    this.isVehicleModalOpen = false;
    this.isVehicleSubmitting = false;
    this.editingVehicleId = null;
    this.vehicleErrorMessage = '';
    this.vehicleSuccessMessage = '';
    this.clearSelectedVehicleFiles();
    this.clearVehicleDocumentErrors();
  }

  onVehiclePhotoSelected(event: Event): void {
    const selectedFile = this.extractImageFile(event, 'Vehicle photo');
    this.selectedVehiclePhotoFile = selectedFile;
    this.vehiclePhotoError = selectedFile ? '' : this.vehiclePhotoError;
  }

  onRegistrationCardFrontSelected(event: Event): void {
    const selectedFile = this.extractImageFile(event, 'Registration card front image');
    this.selectedRegistrationCardFrontFile = selectedFile;
    this.registrationCardFrontError = selectedFile ? '' : this.registrationCardFrontError;
  }

  onRegistrationCardBackSelected(event: Event): void {
    const selectedFile = this.extractImageFile(event, 'Registration card back image');
    this.selectedRegistrationCardBackFile = selectedFile;
    this.registrationCardBackError = selectedFile ? '' : this.registrationCardBackError;
  }

  submitVehicleForm(): void {
    this.vehicleErrorMessage = '';
    this.vehicleSuccessMessage = '';
    this.clearVehicleDocumentErrors();

    if (this.vehicleForm.invalid) {
      this.vehicleForm.markAllAsTouched();
      this.vehicleErrorMessage = 'Please fix all validation errors';
      return;
    }

    if (!this.validateVehicleDocumentFiles()) {
      this.vehicleErrorMessage = 'Please upload all required vehicle documents.';
      return;
    }

    const payload = this.buildVehiclePayload();

    this.isVehicleSubmitting = true;
    this.buildVehicleMultipartPayload(payload).subscribe({
      next: (multipartPayload) => {
        if (!multipartPayload.vehiclePhoto || !multipartPayload.registrationCardFront || !multipartPayload.registrationCardBack) {
          this.vehicleErrorMessage = 'Unable to load existing vehicle documents. Please re-upload all images.';
          this.isVehicleSubmitting = false;
          return;
        }

        this.sendVehicleRequest(multipartPayload);
      },
      error: () => {
        this.vehicleErrorMessage = 'Unable to prepare vehicle documents for submission.';
        this.isVehicleSubmitting = false;
      }
    });
  }

  private buildVehiclePayload(): VehiculeWritePayload {
    const raw = this.vehicleForm.getRawValue();

    return {
      type: String(raw.type ?? '').trim().toUpperCase(),
      registrationnumbers: this.normalizeRegistrationSeries(String(raw.registrationnumbers ?? '')),
      capacity: Number(raw.capacity),
      status: String(raw.status ?? '').trim().toUpperCase()
    };
  }

  private normalizeRegistrationSeries(value: string): string {
    return value.trim().replace(/\s+/g, ' ').toUpperCase();
  }

  private getVehicleRequestErrorMessage(error: unknown, fallback: string): string {
    const httpError = error as HttpErrorResponse;
    const backendMessage = typeof httpError?.error?.message === 'string'
      ? httpError.error.message
      : (typeof httpError?.message === 'string' ? httpError.message : '');

    const normalizedMessage = backendMessage.trim().toLowerCase();
    const isDuplicateSeries =
      httpError?.status === 409 ||
      normalizedMessage.includes('serie existe') ||
      normalizedMessage.includes('already exists') ||
      normalizedMessage.includes('duplicate');

    if (isDuplicateSeries) {
      return this.duplicateVehicleSeriesMessage;
    }

    return backendMessage || fallback;
  }

  private sendVehicleRequest(multipartPayload: VehiculeMultipartPayload): void {
    console.log('Sending vehicle request with payload:', multipartPayload);

    if (this.editingVehicleId) {
      // Update
      this.deliveryApi.updateVehiculeWithDocuments(this.editingVehicleId, multipartPayload).subscribe({
        next: (response) => {
          console.log('Vehicle updated successfully:', response);
          this.setVehicleDocumentPreviewsFromSelectedFiles(response.id);
          this.vehicleSuccessMessage = 'Vehicle updated successfully!';
          this.isVehicleSubmitting = false;
          setTimeout(() => {
            this.closeVehicleModal();
            this.loadVehicles();
          }, 500);
        },
        error: (err) => {
          const errorMsg = this.getVehicleRequestErrorMessage(err, 'Failed to update vehicle');
          console.error('Failed to update vehicle:', err);
          this.vehicleErrorMessage = errorMsg;
          this.isVehicleSubmitting = false;
        }
      });
    } else {
      // Create
      this.deliveryApi.createVehiculeWithDocuments(multipartPayload).subscribe({
        next: (response) => {
          console.log('Vehicle created successfully:', response);
          this.setVehicleDocumentPreviewsFromSelectedFiles(response.id);
          this.vehicleSuccessMessage = 'Vehicle created successfully!';
          this.isVehicleSubmitting = false;
          setTimeout(() => {
            this.closeVehicleModal();
            this.loadVehicles();
          }, 500);
        },
        error: (err) => {
          const errorMsg = this.getVehicleRequestErrorMessage(err, 'Failed to create vehicle');
          console.error('Failed to create vehicle:', err);
          this.vehicleErrorMessage = errorMsg;
          if (errorMsg === this.duplicateVehicleSeriesMessage) {
            window.alert(this.duplicateVehicleSeriesMessage);
          }
          this.isVehicleSubmitting = false;
        }
      });
    }
  }

  private buildVehicleMultipartPayload(payload: VehiculeWritePayload): Observable<VehiculeMultipartPayload> {
    if (!this.editingVehicleId) {
      return of({
        ...payload,
        vehiclePhoto: this.selectedVehiclePhotoFile,
        registrationCardFront: this.selectedRegistrationCardFrontFile,
        registrationCardBack: this.selectedRegistrationCardBackFile
      });
    }

    const vehicleId = this.editingVehicleId;

    const vehiclePhoto$ = this.selectedVehiclePhotoFile
      ? of(this.selectedVehiclePhotoFile)
      : this.deliveryApi.getVehiclePhoto(vehicleId).pipe(
        map((blob) => this.createVehicleDocumentFile(blob, `vehicle-photo-${vehicleId}`)),
        catchError(() => of(null))
      );

    const registrationCardFront$ = this.selectedRegistrationCardFrontFile
      ? of(this.selectedRegistrationCardFrontFile)
      : this.deliveryApi.getRegistrationCardFront(vehicleId).pipe(
        map((blob) => this.createVehicleDocumentFile(blob, `registration-front-${vehicleId}`)),
        catchError(() => of(null))
      );

    const registrationCardBack$ = this.selectedRegistrationCardBackFile
      ? of(this.selectedRegistrationCardBackFile)
      : this.deliveryApi.getRegistrationCardBack(vehicleId).pipe(
        map((blob) => this.createVehicleDocumentFile(blob, `registration-back-${vehicleId}`)),
        catchError(() => of(null))
      );

    return forkJoin({
      vehiclePhoto: vehiclePhoto$,
      registrationCardFront: registrationCardFront$,
      registrationCardBack: registrationCardBack$
    }).pipe(
      map((documents) => ({
        ...payload,
        ...documents
      }))
    );
  }

  private createVehicleDocumentFile(blob: Blob, fileNamePrefix: string): File {
    const mimeType = blob.type || 'application/octet-stream';
    const extension = this.getFileExtensionFromMimeType(mimeType);
    return new File([blob], `${fileNamePrefix}.${extension}`, { type: mimeType });
  }

  private getFileExtensionFromMimeType(mimeType: string): string {
    const normalizedType = mimeType.toLowerCase();
    if (normalizedType === 'image/jpeg') {
      return 'jpg';
    }
    if (normalizedType === 'image/png') {
      return 'png';
    }
    if (normalizedType === 'image/webp') {
      return 'webp';
    }
    if (normalizedType === 'image/gif') {
      return 'gif';
    }
    if (normalizedType.startsWith('image/')) {
      return normalizedType.slice('image/'.length);
    }

    return 'bin';
  }

  confirmDeleteVehicle(vehicle: VehiculeResource): void {
    const confirmed = window.confirm(`Delete vehicle ${vehicle.registrationnumbers}?`);
    if (!confirmed) return;

    this.deliveryApi.vehicules.delete(vehicle.id).subscribe({
      next: () => {
        this.vehicles = this.vehicles.filter((item) => item.id !== vehicle.id);
      }
    });
  }
// affecter delivery seuelement avec leur voiture 
  private loadPendingDeliveries(): void {
    this.deliveryApi.deliveries.list().subscribe({
      next: (deliveries) => {
        this.allDeliveriesSource = deliveries ?? [];
        this.loadDeliveryQrCodes(deliveries);
        this.pendingDeliveriesSource = deliveries.filter(
          (delivery) => !this.isDeliveredStatus(delivery.status) && !this.isCancelledStatus(delivery.status)
        );
        // Charger aussi les livraisons Delivered depuis l'API
        const deliveredSource = deliveries.filter(
          (delivery) => this.isDeliveredStatus(delivery.status)
        );
        // Charger aussi les livraisons Cancelled depuis l'API
        const cancelledSource = deliveries.filter(
          (delivery) => this.isCancelledStatus(delivery.status)
        );
        this.refreshPendingDeliveriesView();
        this.refreshDeliveredDeliveriesView(deliveredSource);
        this.refreshCancelledDeliveriesView(cancelledSource);
      },
      error: () => {
        this.clearDeliveryQrCodeUrls();
        this.allDeliveriesSource = [];
        this.pendingDeliveriesSource = [];
        this.pendingDeliveries = [];
        this.deliveredDeliveries = [];
        this.cancelledDeliveries = [];
      }
    });
  }

  get overviewStats(): CourierOverviewStats {
    if (this.courierStatistics) {
      return {
        totalDeliveries: this.courierStatistics.totalDeliveries ?? 0,
        deliveredDeliveries: this.courierStatistics.deliveredDeliveries ?? 0,
        completionRate: this.courierStatistics.completionRate ?? 0,
        totalDistanceKm: Number(this.courierStatistics.totalDistanceKm ?? 0),
        pendingDeliveries: this.courierStatistics.pendingDeliveries ?? 0,
        cancelledDeliveries: this.courierStatistics.cancelledDeliveries ?? 0,
        totalVehicles: this.courierStatistics.totalVehicles ?? 0
      };
    }

    const deliveriesForCourier = this.getCourierScopedDeliveries();

    const deliveredDeliveries = deliveriesForCourier.filter((delivery) => this.isDeliveredStatus(delivery.status)).length;
    const cancelledDeliveries = deliveriesForCourier.filter((delivery) => this.isCancelledStatus(delivery.status)).length;
    const pendingDeliveries = deliveriesForCourier.filter(
      (delivery) => !this.isDeliveredStatus(delivery.status) && !this.isCancelledStatus(delivery.status)
    ).length;

    const totalDistanceKm = deliveriesForCourier.reduce((sum, delivery) => {
      const distance = Number(delivery.distanceKm ?? 0);
      return Number.isFinite(distance) ? sum + distance : sum;
    }, 0);

    const totalDeliveries = deliveriesForCourier.length;
    const completionRate = totalDeliveries > 0 ? (deliveredDeliveries * 100) / totalDeliveries : 0;

    return {
      totalDeliveries,
      deliveredDeliveries,
      completionRate,
      totalDistanceKm,
      pendingDeliveries,
      cancelledDeliveries,
      totalVehicles: this.vehicles.length
    };
  }

  get averageDistancePerDeliveryKm(): number {
    const total = this.overviewStats.totalDeliveries;
    if (total <= 0) {
      return 0;
    }

    return this.overviewStats.totalDistanceKm / total;
  }

  get completionRingBackground(): string {
    const clampedPercent = Math.max(0, Math.min(100, this.overviewStats.completionRate));
    return `conic-gradient(#4f46e5 0% ${clampedPercent}%, #e5e7eb ${clampedPercent}% 100%)`;
  }

  get overviewUpcomingDeliveries(): DelivererDeliveryItem[] {
    return this.pendingDeliveries.slice(0, 3);
  }

  get top5CouriersByDelivered(): TopCourierResponse[] {
    return this.topCouriers.slice(0, 5);
  }

  get trendDeltaLabel(): string {
    const values = this.deliveredLast7Days;
    if (values.length < 2) {
      return 'Stable';
    }

    const midpoint = Math.floor(values.length / 2);
    const firstPeriod = values.slice(0, midpoint).reduce((sum, value) => sum + value, 0);
    const secondPeriod = values.slice(midpoint).reduce((sum, value) => sum + value, 0);

    if (firstPeriod === 0 && secondPeriod === 0) {
      return 'Stable';
    }

    if (firstPeriod === 0) {
      return '+100% vs start of week';
    }

    const deltaPercent = ((secondPeriod - firstPeriod) / firstPeriod) * 100;
    const sign = deltaPercent >= 0 ? '+' : '';
    return `${sign}${deltaPercent.toFixed(0)}% vs start of week`;
  }

  get deliveredLast7Days(): number[] {
    const deliveriesForCourier = this.getCourierScopedDeliveries().filter((delivery) => this.isDeliveredStatus(delivery.status));
    const dayLabels = this.last7DayLabels;

    const countByDay = new Map<string, number>();
    dayLabels.forEach((label) => countByDay.set(label, 0));

    deliveriesForCourier.forEach((delivery) => {
      const isoDate = this.normalizeDateLabel(delivery.deliverydate);
      if (!isoDate || !countByDay.has(isoDate)) {
        return;
      }

      countByDay.set(isoDate, (countByDay.get(isoDate) ?? 0) + 1);
    });

    return dayLabels.map((label) => countByDay.get(label) ?? 0);
  }

  get deliveredLast7DaysMax(): number {
    const values = this.deliveredLast7Days;
    const maxValue = values.reduce((max, value) => Math.max(max, value), 0);
    return maxValue > 0 ? maxValue : 1;
  }

  get deliveredTrendPoints(): string {
    const values = this.deliveredLast7Days;
    const maxValue = this.deliveredLast7DaysMax;
    const width = 100;
    const height = 40;

    if (values.length === 1) {
      return `0,${height / 2}`;
    }

    return values
      .map((value, index) => {
        const x = (index / (values.length - 1)) * width;
        const y = height - (value / maxValue) * height;
        return `${x.toFixed(2)},${y.toFixed(2)}`;
      })
      .join(' ');
  }

  get deliveredLast7DayTicks(): string[] {
    return this.last7DayLabels.map((label) => this.formatDayLabel(label));
  }

  get statusDistribution(): StatusDistributionItem[] {
    const stats = this.overviewStats;
    const total = Math.max(stats.totalDeliveries, 1);

    return [
      {
        label: 'Delivered',
        count: stats.deliveredDeliveries,
        percent: (stats.deliveredDeliveries / total) * 100
      },
      {
        label: 'Pending',
        count: stats.pendingDeliveries,
        percent: (stats.pendingDeliveries / total) * 100
      },
      {
        label: 'Cancelled',
        count: stats.cancelledDeliveries,
        percent: (stats.cancelledDeliveries / total) * 100
      }
    ];
  }

  private get last7DayLabels(): string[] {
    const labels: string[] = [];
    const now = new Date();

    for (let offset = 6; offset >= 0; offset -= 1) {
      const day = new Date(now);
      day.setDate(now.getDate() - offset);
      labels.push(day.toISOString().slice(0, 10));
    }

    return labels;
  }

  private getCourierScopedDeliveries(): DeliveryResource[] {
    return this.allDeliveriesSource.filter((delivery) => {
      const deliveryVehiculeId = delivery.vehicule?.id;
      return typeof deliveryVehiculeId === 'number' && this.delivererVehicleIds.has(deliveryVehiculeId);
    });
  }

  private normalizeDateLabel(value: string | null | undefined): string | null {
    const raw = (value ?? '').trim();
    if (!raw) {
      return null;
    }

    const parsed = new Date(raw);
    if (Number.isNaN(parsed.getTime())) {
      return null;
    }

    return parsed.toISOString().slice(0, 10);
  }

  private formatDayLabel(isoLabel: string): string {
    const parsed = new Date(isoLabel);
    if (Number.isNaN(parsed.getTime())) {
      return isoLabel;
    }

    return parsed.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
  }

  private isDeliveredStatus(status: string | null | undefined): boolean {
    const normalized = (status ?? '').trim().toUpperCase();
    return normalized.includes('DELIVER') || normalized.includes('LIVR');
  }

  private isCancelledStatus(status: string | null | undefined): boolean {
    const normalized = (status ?? '').trim().toUpperCase();
    return normalized.includes('CANCEL') || normalized.includes('ANNUL');
  }

  submitDeliveryQrScan(): void {
    this.qrScanError = '';
    this.qrScanSuccess = '';

    const token = this.extractQrToken(this.qrScanInput);
    if (!token) {
      this.qrScanError = 'QR invalide. Collez le token ou le lien complet du QR.';
      return;
    }

    this.isQrScanSubmitting = true;
    this.deliveryApi.scanDeliveryQrToken(token).subscribe({
      next: (updatedDelivery) => {
        this.qrScanInput = '';
        this.qrScanSuccess = `Livraison DEL${updatedDelivery.id} marquee Delivered avec succes.`;
        this.loadPendingDeliveries();
        this.isQrScanSubmitting = false;
      },
      error: () => {
        this.qrScanError = 'Echec du scan QR. Verifiez le token ou vos droits Courier.';
        this.isQrScanSubmitting = false;
      }
    });
  }

  private loadDeliveryQrCodes(deliveries: DeliveryResource[]): void {
    this.clearDeliveryQrCodeUrls();

    deliveries.forEach((delivery) => {
      this.deliveryApi.getDeliveryQrCode(delivery.id).subscribe({
        next: (blob) => {
          const objectUrl = URL.createObjectURL(blob);
          this.deliveryQrCodeUrls[`DEL${delivery.id}`] = objectUrl;
        },
        error: () => {
          this.deliveryQrCodeUrls[`DEL${delivery.id}`] = '';
        }
      });
    });
  }

  private clearDeliveryQrCodeUrls(): void {
    Object.values(this.deliveryQrCodeUrls).forEach((objectUrl) => {
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    });
    this.deliveryQrCodeUrls = {};
  }

  private extractQrToken(rawValue: string): string | null {
    const trimmed = rawValue.trim();
    if (!trimmed) {
      return null;
    }

    const scanPathMarker = '/api/delivery/deliveries/scan/';
    const markerIndex = trimmed.indexOf(scanPathMarker);
    if (markerIndex >= 0) {
      const afterMarker = trimmed.slice(markerIndex + scanPathMarker.length);
      const tokenFromUrl = afterMarker.split(/[?#]/)[0]?.trim();
      return tokenFromUrl || null;
    }

    return trimmed;
  }

  openDeliveryDetails(delivery: DelivererDeliveryItem): void {
    this.selectedDeliveryDetails = delivery;
    this.isDeliveryDetailsModalOpen = true;
  }

  closeDeliveryDetailsModal(): void {
    this.isDeliveryDetailsModalOpen = false;
    this.selectedDeliveryDetails = null;
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

// Afficher les livraisons délivrées depuis l'API
  private refreshDeliveredDeliveriesView(deliveredSource: DeliveryResource[]): void {
    this.deliveredDeliveries = deliveredSource
      .filter((delivery) => {
        const deliveryVehiculeId = delivery.vehicule?.id;
        return typeof deliveryVehiculeId === 'number' && this.delivererVehicleIds.has(deliveryVehiculeId);
      })
      .map((delivery) => {
        const receiverId = this.extractReceiverId(delivery);
        return {
          id: `DEL${delivery.id}`,
          orderLabel: delivery.order?.id ? `#${delivery.order.id}` : '-',
          from: delivery.courier?.name ?? 'Starting point',
          to: delivery.address,
          distance: 0,
          distanceKm: delivery.distanceKm ?? null,
          price: delivery.order?.totalAmount ?? 0,
          priority: (delivery.status || '').toLowerCase().includes('urgent') ? 'urgent' : 'normal',
          date: delivery.deliverydate ? delivery.deliverydate.split('T')[0] : 'Date not available',
          vehicule: delivery.vehicule?.registrationnumbers || delivery.vehicule?.type || 'Vehicule non affecte',
          deliverytype: delivery.deliverytype,
          city: delivery.city,
          postalCode: delivery.postalCode,
          phoneNumber: delivery.phoneNumber,
          deliveryMode: delivery.deliveryMode,
          paymentMode: delivery.paymentMode,
          realWeightKg: (delivery as any).realWeight ?? (delivery as any).realWeightKg ?? null,
          volumetricWeightKg: (delivery as any).volumetricWeight ?? (delivery as any).volumetricWeightKg ?? null,
          billableWeightKg: (delivery as any).billableWeight ?? (delivery as any).billableWeightKg ?? null,
          orderStatus: delivery.order?.status,
          status: delivery.status as 'Accepted' | 'In progress' | 'Delivered' | 'Cancelled',
          receiverId,
          userName: delivery.order?.user?.name || `Client #${receiverId || 'Inconnu'}`,
          cancellationReason: delivery.cancellationReason
        };
      });
  }

  private refreshCancelledDeliveriesView(cancelledSource: DeliveryResource[]): void {
    this.cancelledDeliveries = cancelledSource
      .filter((delivery) => {
        const deliveryVehiculeId = delivery.vehicule?.id;
        return typeof deliveryVehiculeId === 'number' && this.delivererVehicleIds.has(deliveryVehiculeId);
      })
      .map((delivery) => {
        const receiverId = this.extractReceiverId(delivery);
        return {
          id: `DEL${delivery.id}`,
          orderLabel: delivery.order?.id ? `#${delivery.order.id}` : '-',
          from: delivery.courier?.name ?? 'Starting point',
          to: delivery.address,
          distance: 0,
          distanceKm: delivery.distanceKm ?? null,
          price: delivery.order?.totalAmount ?? 0,
          priority: (delivery.status || '').toLowerCase().includes('urgent') ? 'urgent' : 'normal',
          date: delivery.deliverydate ? delivery.deliverydate.split('T')[0] : 'Date not available',
          vehicule: delivery.vehicule?.registrationnumbers || delivery.vehicule?.type || 'Vehicule non affecte',
          deliverytype: delivery.deliverytype,
          city: delivery.city,
          postalCode: delivery.postalCode,
          phoneNumber: delivery.phoneNumber,
          deliveryMode: delivery.deliveryMode,
          paymentMode: delivery.paymentMode,
          realWeightKg: (delivery as any).realWeight ?? (delivery as any).realWeightKg ?? null,
          volumetricWeightKg: (delivery as any).volumetricWeight ?? (delivery as any).volumetricWeightKg ?? null,
          billableWeightKg: (delivery as any).billableWeight ?? (delivery as any).billableWeightKg ?? null,
          orderStatus: delivery.order?.status,
          status: delivery.status as 'Accepted' | 'In progress' | 'Delivered' | 'Cancelled',
          receiverId,
          userName: delivery.order?.user?.name || `Client #${receiverId || 'Inconnu'}`,
          cancellationReason: delivery.cancellationReason
        };
      });
  }
// serie de voiture et date 
  private refreshPendingDeliveriesView(): void {
    this.pendingDeliveries = this.pendingDeliveriesSource
      .filter((delivery) => {
        const deliveryVehiculeId = delivery.vehicule?.id;
        return typeof deliveryVehiculeId === 'number' && this.delivererVehicleIds.has(deliveryVehiculeId);
      })
      .map((delivery) => {
        const receiverId = this.extractReceiverId(delivery);
        return {
          id: `DEL${delivery.id}`,
          orderLabel: delivery.order?.id ? `#${delivery.order.id}` : '-',
          from: delivery.courier?.name ?? 'Starting point',
          to: delivery.address,
          distance: 0,
          distanceKm: delivery.distanceKm ?? null,
          price: delivery.order?.totalAmount ?? 0,
          priority: (delivery.status || '').toLowerCase().includes('urgent') ? 'urgent' : 'normal',
          date: delivery.deliverydate ? delivery.deliverydate.split('T')[0] : 'Date not available',
          vehicule: delivery.vehicule?.registrationnumbers || delivery.vehicule?.type || 'Vehicule non affecte',
          deliverytype: delivery.deliverytype,
          city: delivery.city,
          postalCode: delivery.postalCode,
          phoneNumber: delivery.phoneNumber,
          deliveryMode: delivery.deliveryMode,
          paymentMode: delivery.paymentMode,
          realWeightKg: (delivery as any).realWeight ?? (delivery as any).realWeightKg ?? null,
          volumetricWeightKg: (delivery as any).volumetricWeight ?? (delivery as any).volumetricWeightKg ?? null,
          billableWeightKg: (delivery as any).billableWeight ?? (delivery as any).billableWeightKg ?? null,
          orderStatus: delivery.order?.status,
          status: delivery.status as 'Accepted' | 'In progress' | 'Delivered',
          receiverId,
          userName: delivery.order?.user?.name || `Client #${receiverId || 'Inconnu'}`
        };
      });
  }

  private loadProfile(): void {
    const currentUser = this.authService.currentUser();
    if (!currentUser) {
      this.profileUser = null;
      return;
    }

    if (currentUser.id > 0) {
      this.commonApi.users.getById(currentUser.id).subscribe({
        next: (user) => {
          this.setProfileUser(user);
        },
        error: () => {
          this.loadProfileByEmail(currentUser.email);
        }
      });
      return;
    }

    this.loadProfileByEmail(currentUser.email);
  }

  private loadProfileByEmail(email: string): void {
    this.commonApi.users.list().subscribe({
      next: (users) => {
        const normalizedEmail = email.trim().toLowerCase();
        const matchedUser = users.find(
          (user) => user.email.trim().toLowerCase() === normalizedEmail
        );

        if (!matchedUser) {
          this.profileUser = null;
          return;
        }

        this.authService.updateUser({ id: matchedUser.id, email: matchedUser.email, name: matchedUser.name });
        this.setProfileUser(matchedUser);
      },
      error: () => {
        this.profileUser = null;
      }
    });
  }

  private setProfileUser(user: UserResource): void {
    this.profileUser = user;
    this.profileForm = {
      name: user.name,
      email: user.email,
      phoneNumber: this.courierProfile?.phoneNumber ?? '',
      password: ''
    };
  }
//ajouter image de permis de conduire pour les livreurs 
  private loadCourierProfile(): void {
    this.deliveryApi.getMyCourierProfile().subscribe({
      next: (profile) => {
        this.courierProfile = profile;
        this.profileForm.phoneNumber = profile.phoneNumber ?? '';

        this.restoreVerificationState(profile);

        this.loadPermitImage();
        this.loadCourierStatisticsForProfile(profile);
      },
      error: () => {
        this.courierProfile = null;
        this.clearPermitPreview();
        this.courierStatistics = null;
        this.courierStatisticsError = 'Unable to load courier statistics.';
        this.isCourierStatisticsLoading = false;
      }
    });
  }

  private loadCourierStatisticsForProfile(profile: CourierProfileResponse | null): void {
    const courierId = this.getCourierIdFromProfile(profile);
    if (!courierId) {
      this.courierStatistics = null;
      this.courierStatisticsError = 'Courier ID not found. Unable to load statistics.';
      this.isCourierStatisticsLoading = false;
      return;
    }

    this.isCourierStatisticsLoading = true;
    this.courierStatisticsError = '';

    this.deliveryApi.getCourierStatistics(courierId).subscribe({
      next: (stats) => {
        this.courierStatistics = stats;
        this.isCourierStatisticsLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.courierStatistics = null;
        this.isCourierStatisticsLoading = false;
        

      
      }
    });
  }

  private loadTopCouriers(): void {
    this.isTopCouriersLoading = true;
    this.topCouriersError = '';

    this.deliveryApi.getTopCouriersByDeliveredDeliveries(5).subscribe({
      next: (rows) => {
        const sortedRows = (rows ?? [])
          .filter((row) => Number(row.deliveredDeliveries ?? 0) > 0)
          .sort((left, right) => {
            const rightValue = Number(right.deliveredDeliveries ?? 0);
            const leftValue = Number(left.deliveredDeliveries ?? 0);
            if (rightValue !== leftValue) {
              return rightValue - leftValue;
            }
            return String(left.courierName ?? '').localeCompare(String(right.courierName ?? ''));
          });

        this.topCouriers = sortedRows.slice(0, 5);
        this.isTopCouriersLoading = false;
      },
      error: () => {
        this.topCouriers = [];
        this.topCouriersError = 'Unable to load top couriers.';
        this.isTopCouriersLoading = false;
      }
    });
  }

  private getCourierIdFromProfile(profile: CourierProfileResponse | null): number | null {
    if (!profile) {
      return null;
    }

    const dynamicProfile = profile as Record<string, unknown>;
    const idCandidates = [dynamicProfile['courierId'], dynamicProfile['courier_id'], profile.id];

    for (const candidate of idCandidates) {
      if (typeof candidate === 'number' && Number.isFinite(candidate) && candidate > 0) {
        return candidate;
      }
    }

    return null;
  }

  private loadPermitImage(): void {
    this.deliveryApi.getMyPermitImage().subscribe({
      next: (blob) => {
        if (!blob || blob.size === 0) {
          this.clearPermitPreview();
          return;
        }

        this.setPermitPreview(blob);
      },
      error: () => {
        this.clearPermitPreview();
      }
    });
  }

  private setPermitPreview(blob: Blob): void {
    this.clearPermitPreview();
    this.permitPreviewUrl = URL.createObjectURL(blob);
  }

  private clearPermitPreview(): void {
    if (this.permitPreviewUrl) {
      URL.revokeObjectURL(this.permitPreviewUrl);
      this.permitPreviewUrl = null;
    }
  }

  private clearSelfiePreview(): void {
    if (this.selfiePreviewUrl) {
      URL.revokeObjectURL(this.selfiePreviewUrl);
      this.selfiePreviewUrl = null;
    }
  }

  hasPermitImage(): boolean {
    return Boolean(
      this.permitPreviewUrl ||
      this.courierProfile?.permitImageUploaded ||
      this.courierProfile?.hasPermitImage ||
      this.courierProfile?.permitImageExists
    );
  }
// ajouter une méthode pour mettre à jour le statut du livreur et button disabled

  getCourierStatus(): CourierStatus {
    return this.normalizeCourierStatus(this.courierProfile?.status) ?? 'PENDING';
  }

  isCourierAccessRestricted(): boolean {
    const normalizedStatus = this.normalizeCourierStatus(this.courierProfile?.status);
    if (!normalizedStatus) {
      return false;
    }

    return normalizedStatus === 'PENDING' || normalizedStatus === 'REFUSED';
  }

  getCourierStatusLabel(): string {
    const status = this.getCourierStatus();
    if (status === 'ACCEPTED') {
      return 'ACCEPTED';
    }
    if (status === 'REFUSED') {
      return 'REFUSED';
    }
    return 'PENDING';
  }

  getProfileStatus(): CourierProfileStatus {
    const normalizedProfileStatus = this.normalizeCourierProfileStatus(this.courierProfile?.profileStatus);
    if (normalizedProfileStatus) {
      return normalizedProfileStatus;
    }

    return 'INCOMPLETE';
  }

  getProfileStatusLabel(): string {
    return this.getProfileStatus() === 'COMPLETED' ? 'COMPLETED' : 'INCOMPLETE';
  }

  isProfileIncomplete(): boolean {
    return this.getProfileStatus() === 'INCOMPLETE';
  }

  private normalizeCourierStatus(status: string | null | undefined): CourierStatus | null {
    const normalized = (status ?? '').trim().toUpperCase();
    if (normalized === 'ACCEPTED' || normalized === 'REFUSED' || normalized === 'PENDING') {
      return normalized;
    }

    return null;
  }

  private normalizeCourierProfileStatus(status: string | null | undefined): CourierProfileStatus | null {
    const normalized = (status ?? '').trim().toUpperCase();
    if (normalized === 'COMPLETED' || normalized === 'INCOMPLETE') {
      return normalized;
    }

    return null;
  }

  onPermitFileSelected(event: Event): void {
    this.permitUploadError = '';
    this.permitUploadSuccess = '';

    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0] ?? null;
    if (!file) {
      this.selectedPermitFile = null;
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.selectedPermitFile = null;
      this.permitUploadError = 'Permit file must be an image.';
      if (input) {
        input.value = '';
      }
      return;
    }

    if (file.size > this.maxPermitSizeBytes) {
      this.selectedPermitFile = null;
      this.permitUploadError = 'Permit image must be 5 MB or less.';
      if (input) {
        input.value = '';
      }
      return;
    }

    this.selectedPermitFile = file;
  }

  onSelfieFileSelected(event: Event): void {
    this.verificationError = '';
    this.verificationResult = null;

    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0] ?? null;

    if (!file) {
      this.selectedSelfieFile = null;
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.selectedSelfieFile = null;
      this.verificationError = 'Selfie file must be an image.';
      if (input) {
        input.value = '';
      }
      return;
    }

    if (file.size > this.maxPermitSizeBytes) {
      this.selectedSelfieFile = null;
      this.verificationError = 'Selfie image must be 5 MB or less.';
      if (input) {
        input.value = '';
      }
      return;
    }

    this.setSelfieFile(file);
  }

  private setSelfieFile(file: File): void {
    this.selectedSelfieFile = file;
    this.verificationError = '';
    if (this.selfiePreviewUrl) {
      URL.revokeObjectURL(this.selfiePreviewUrl);
    }
    this.selfiePreviewUrl = URL.createObjectURL(file);
  }

  async openCamera(): Promise<void> {
    this.cameraError = '';

    if (!navigator.mediaDevices?.getUserMedia) {
      this.cameraError = 'Votre appareil ne supporte pas l\'accès à la caméra.';
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' } });
      this.cameraStream = stream;
      this.isCameraOpen = true;
      this.cdr.detectChanges();

      const video = this.cameraVideo?.nativeElement;
      if (!video) {
        this.cameraError = 'Impossible de démarrer la caméra.';
        return;
      }

      video.srcObject = stream;
      try {
        await video.play();
      } catch {
        // ignore autoplay promise rejection when not required
      }
    } catch (error) {
      this.cameraError = 'Impossible d\'ouvrir la caméra.';
    }
  }

  closeCamera(): void {
    if (this.cameraStream) {
      this.cameraStream.getTracks().forEach((track) => track.stop());
      this.cameraStream = null;
    }
    this.isCameraOpen = false;
    this.cameraError = '';

    if (this.cameraVideo?.nativeElement) {
      this.cameraVideo.nativeElement.srcObject = null;
    }
  }

  captureSelfie(): void {
    const video = this.cameraVideo?.nativeElement;
    if (!video || video.videoWidth === 0 || video.videoHeight === 0) {
      this.cameraError = 'La caméra n\'est pas prête.';
      return;
    }

    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const context = canvas.getContext('2d');

    if (!context) {
      this.cameraError = 'Impossible de capturer le selfie.';
      return;
    }

    context.drawImage(video, 0, 0, canvas.width, canvas.height);
    canvas.toBlob((blob) => {
      if (!blob) {
        this.cameraError = 'Impossible de capturer le selfie.';
        return;
      }

      const file = new File([blob], 'selfie.jpg', { type: 'image/jpeg' });
      this.setSelfieFile(file);
      this.closeCamera();
    }, 'image/jpeg', 0.95);
  }

  verifyPermitIdentity(): void {
    this.verificationError = '';
    this.verificationResult = null;

    if (!this.hasPermitImage()) {
      this.verificationError = 'Please upload your permit image before verification.';
      return;
    }

    if (!this.selectedSelfieFile) {
      this.verificationError = 'Please select a selfie image first.';
      return;
    }

    this.isPermitVerificationSubmitting = true;

    this.deliveryApi.verifyMyPermitWithSelfie(this.selectedSelfieFile).subscribe({
      next: (result) => {
        this.verificationResult = result;
        this.selectedSelfieFile = null;
        this.isPermitVerificationSubmitting = false;
      },
      error: (error: HttpErrorResponse) => {
        const messageCandidate = (error.error as { message?: string } | null)?.message;
        this.verificationError = messageCandidate || 'Verification failed. Please try again.';
        this.isPermitVerificationSubmitting = false;
      }
    });
  }

  uploadPermitImage(): void {
    this.permitUploadError = '';
    this.permitUploadSuccess = '';

    if (!this.selectedPermitFile) {
      this.permitUploadError = 'Please select a permit image first.';
      return;
    }

    this.isPermitUploading = true;

    this.deliveryApi.uploadMyPermitImage(this.selectedPermitFile).subscribe({
      next: (profile) => {
        this.courierProfile = profile;
        this.permitUploadSuccess = 'Permit image uploaded successfully.';
        this.selectedPermitFile = null;
        this.clearPersistedVerificationState();
        this.loadPermitImage();
        this.isPermitUploading = false;
        this.verifyStep = 1;
      },
      error: (error: { error?: { message?: string } }) => {
        this.permitUploadError = error?.error?.message || 'Failed to upload permit image.';
        this.isPermitUploading = false;
      }
    });
  }

  // Stepper methods
  resetVerifyFlow(): void {
    this.verifyStep = 0;
    this.verificationResult = null;
    this.verificationError = '';
    this.selectedPermitFile = null;
    this.selectedSelfieFile = null;
    this.selfiePreviewUrl = null;
    this.clearPersistedVerificationState();
    this.closeCamera();
  }

  uploadAndContinue(): void {
    if (!this.selectedPermitFile) return;
    this.isPermitUploading = true;
    this.deliveryApi.uploadMyPermitImage(this.selectedPermitFile).subscribe({
      next: (profile) => {
        this.courierProfile = profile;
        this.loadPermitImage();
        this.isPermitUploading = false;
        this.verifyStep = 1;
      },
      error: (error) => {
        this.permitUploadError = error?.error?.message || 'Échec du téléchargement.';
        this.isPermitUploading = false;
      }
    });
  }

  verifyAndContinue(): void {
    if (!this.selectedSelfieFile || !this.hasPermitImage()) return;
    this.verifyStep = 2;
    this.isPermitVerificationSubmitting = true;
    this.deliveryApi.verifyMyPermitWithSelfie(this.selectedSelfieFile).subscribe({
      next: (result) => {
        this.verificationResult = result;
        this.persistVerificationState(result);
        this.isPermitVerificationSubmitting = false;
        this.verifyStep = 3;
      },
      error: (error) => {
        this.verificationError = error?.error?.message || 'Vérification échouée.';
        this.isPermitVerificationSubmitting = false;
        this.verifyStep = 3;
      }
    });
  }
// affecter delivery seuelement avec leur voiture 

  private restoreVerificationState(profile: CourierProfileResponse | null): void {
    this.verificationError = '';
    this.verificationResult = null;

    const storageKey = this.getVerificationStorageKey(profile);
    const storedState = storageKey ? this.readVerificationState(storageKey) : null;

    if (storedState?.verdict === 'APPROVED') {
      this.verificationResult = storedState;
      this.verifyStep = 3;
      return;
    }

    const hasStoredPermitImage = Boolean(
      profile?.permitImageUploaded ||
      profile?.hasPermitImage ||
      profile?.permitImageExists
    );
    const courierStatus = this.normalizeCourierStatus(profile?.status);

    if (courierStatus === 'ACCEPTED') {
      this.verificationResult = { verdict: 'APPROVED' };
      this.verifyStep = 3;
      return;
    }

    this.verifyStep = hasStoredPermitImage ? 1 : 0;
  }

  private persistVerificationState(result: VerificationResponse): void {
    const storageKey = this.getVerificationStorageKey();
    if (!storageKey) {
      return;
    }

    localStorage.setItem(storageKey, JSON.stringify(result));
  }

  private clearPersistedVerificationState(): void {
    const storageKey = this.getVerificationStorageKey();
    if (!storageKey) {
      return;
    }

    localStorage.removeItem(storageKey);
  }

  private readVerificationState(storageKey: string): VerificationResponse | null {
    const rawValue = localStorage.getItem(storageKey);
    if (!rawValue) {
      return null;
    }

    try {
      const parsed = JSON.parse(rawValue) as VerificationResponse | null;
      if (parsed?.verdict === 'APPROVED' || parsed?.verdict === 'REFUSED') {
        return parsed;
      }
    } catch {
      return null;
    }

    return null;
  }

  private getVerificationStorageKey(profile?: CourierProfileResponse | null): string | null {
    const currentUser = this.authService.currentUser();
    const identity = profile?.userId ?? currentUser?.id ?? currentUser?.email ?? null;

    if (identity === null || identity === undefined || identity === '') {
      return null;
    }

    return `deliverer-verification-state-${identity}`;
  }

  private loadVehicles(): void {
    this.deliveryApi.vehicules.list().subscribe({
      next: (vehicles: VehiculeResource[]) => {
        const currentUserId = this.authService.currentUser()?.id;
        const loadedVehicles = vehicles || [];

        this.vehicles = loadedVehicles.filter((vehicle) => {
          const vehicleCourierId = vehicle.courier?.id;
          if (typeof currentUserId !== 'number' || currentUserId <= 0) {
            return true;
          }

          if (typeof vehicleCourierId !== 'number') {
            return true;
          }

          return vehicleCourierId === currentUserId;
        });

        this.delivererVehicleIds = new Set(this.vehicles.map((vehicle) => vehicle.id));
        this.loadVehicleDocumentPreviews(this.vehicles);
        this.refreshPendingDeliveriesView();
        // Recharger aussi les delivered après mise à jour des véhicules
        this.loadPendingDeliveries();
      },
      error: () => {
        this.delivererVehicleIds.clear();
        this.vehicles = [];
        this.clearAllVehicleDocumentPreviews();
        this.refreshPendingDeliveriesView();
      }
    });
  }

  private loadVehicleDocumentPreviews(vehicles: VehiculeResource[]): void {
    this.clearAllVehicleDocumentPreviews();

    vehicles.forEach((vehicle) => {
      const preview: VehicleDocumentPreview = {
        vehiclePhotoUrl: null,
        registrationCardFrontUrl: null,
        registrationCardBackUrl: null
      };

      this.vehicleDocumentPreviews.set(vehicle.id, preview);

      if (vehicle.vehiclePhotoFileName) {
        this.deliveryApi.getVehiclePhoto(vehicle.id).subscribe({
          next: (blob) => {
            preview.vehiclePhotoUrl = URL.createObjectURL(blob);
          },
          error: () => {
            preview.vehiclePhotoUrl = null;
          }
        });
      }

      if (vehicle.registrationCardFrontFileName) {
        this.deliveryApi.getRegistrationCardFront(vehicle.id).subscribe({
          next: (blob) => {
            preview.registrationCardFrontUrl = URL.createObjectURL(blob);
          },
          error: () => {
            preview.registrationCardFrontUrl = null;
          }
        });
      }

      if (vehicle.registrationCardBackFileName) {
        this.deliveryApi.getRegistrationCardBack(vehicle.id).subscribe({
          next: (blob) => {
            preview.registrationCardBackUrl = URL.createObjectURL(blob);
          },
          error: () => {
            preview.registrationCardBackUrl = null;
          }
        });
      }
    });
  }

  hasVehiclePhoto(vehicle: VehiculeResource): boolean {
    return Boolean(vehicle.vehiclePhotoFileName);
  }

  hasRegistrationCardFront(vehicle: VehiculeResource): boolean {
    return Boolean(vehicle.registrationCardFrontFileName);
  }

  hasRegistrationCardBack(vehicle: VehiculeResource): boolean {
    return Boolean(vehicle.registrationCardBackFileName);
  }

  openVehicleImagePreview(url: string | null, title: string): void {
    if (!url) {
      return;
    }

    this.selectedVehicleImageUrl = url;
    this.selectedVehicleImageTitle = title;
  }

  closeVehicleImagePreview(): void {
    this.selectedVehicleImageUrl = null;
    this.selectedVehicleImageTitle = '';
  }

  getVehiclePhotoPreviewUrl(vehicleId: number): string | null {
    return this.vehicleDocumentPreviews.get(vehicleId)?.vehiclePhotoUrl ?? null;
  }

  getRegistrationCardFrontPreviewUrl(vehicleId: number): string | null {
    return this.vehicleDocumentPreviews.get(vehicleId)?.registrationCardFrontUrl ?? null;
  }

  getRegistrationCardBackPreviewUrl(vehicleId: number): string | null {
    return this.vehicleDocumentPreviews.get(vehicleId)?.registrationCardBackUrl ?? null;
  }

  getEditingVehiclePhotoPreviewUrl(): string | null {
    if (!this.editingVehicleId) {
      return null;
    }

    return this.getVehiclePhotoPreviewUrl(this.editingVehicleId);
  }

  getEditingRegistrationCardFrontPreviewUrl(): string | null {
    if (!this.editingVehicleId) {
      return null;
    }

    return this.getRegistrationCardFrontPreviewUrl(this.editingVehicleId);
  }

  getEditingRegistrationCardBackPreviewUrl(): string | null {
    if (!this.editingVehicleId) {
      return null;
    }

    return this.getRegistrationCardBackPreviewUrl(this.editingVehicleId);
  }

  private extractImageFile(event: Event, label: string): File | null {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0] ?? null;

    if (!file) {
      return null;
    }

    if (!file.type.startsWith('image/')) {
      this.assignVehicleDocumentError(label, `${label} must be an image.`);
      if (input) {
        input.value = '';
      }
      return null;
    }

    if (file.size > this.maxVehicleFileSizeBytes) {
      this.assignVehicleDocumentError(label, `${label} must be 5 MB or less.`);
      if (input) {
        input.value = '';
      }
      return null;
    }

    this.vehicleErrorMessage = '';
    this.assignVehicleDocumentError(label, '');
    return file;
  }

  private clearSelectedVehicleFiles(): void {
    this.selectedVehiclePhotoFile = null;
    this.selectedRegistrationCardFrontFile = null;
    this.selectedRegistrationCardBackFile = null;
  }

  private validateVehicleDocumentFiles(): boolean {
    if (this.editingVehicleId) {
      return true;
    }

    let isValid = true;

    if (!this.selectedVehiclePhotoFile) {
      this.vehiclePhotoError = 'vehiclePhoto is required';
      isValid = false;
    }

    if (!this.selectedRegistrationCardFrontFile) {
      this.registrationCardFrontError = 'registrationCardFront is required';
      isValid = false;
    }

    if (!this.selectedRegistrationCardBackFile) {
      this.registrationCardBackError = 'registrationCardBack is required';
      isValid = false;
    }

    return isValid;
  }

  private clearVehicleDocumentErrors(): void {
    this.vehiclePhotoError = '';
    this.registrationCardFrontError = '';
    this.registrationCardBackError = '';
  }

  private assignVehicleDocumentError(label: string, message: string): void {
    if (label === 'Vehicle photo') {
      this.vehiclePhotoError = message;
      return;
    }

    if (label === 'Registration card front image') {
      this.registrationCardFrontError = message;
      return;
    }

    if (label === 'Registration card back image') {
      this.registrationCardBackError = message;
    }
  }

  private setVehicleDocumentPreviewsFromSelectedFiles(vehicleId: number): void {
    const current = this.vehicleDocumentPreviews.get(vehicleId) ?? {
      vehiclePhotoUrl: null,
      registrationCardFrontUrl: null,
      registrationCardBackUrl: null
    };

    if (this.selectedVehiclePhotoFile) {
      if (current.vehiclePhotoUrl) {
        URL.revokeObjectURL(current.vehiclePhotoUrl);
      }
      current.vehiclePhotoUrl = URL.createObjectURL(this.selectedVehiclePhotoFile);
    }

    if (this.selectedRegistrationCardFrontFile) {
      if (current.registrationCardFrontUrl) {
        URL.revokeObjectURL(current.registrationCardFrontUrl);
      }
      current.registrationCardFrontUrl = URL.createObjectURL(this.selectedRegistrationCardFrontFile);
    }

    if (this.selectedRegistrationCardBackFile) {
      if (current.registrationCardBackUrl) {
        URL.revokeObjectURL(current.registrationCardBackUrl);
      }
      current.registrationCardBackUrl = URL.createObjectURL(this.selectedRegistrationCardBackFile);
    }

    this.vehicleDocumentPreviews.set(vehicleId, current);
  }

  private clearAllVehicleDocumentPreviews(): void {
    this.vehicleDocumentPreviews.forEach((preview) => {
      if (preview.vehiclePhotoUrl) {
        URL.revokeObjectURL(preview.vehiclePhotoUrl);
      }
      if (preview.registrationCardFrontUrl) {
        URL.revokeObjectURL(preview.registrationCardFrontUrl);
      }
      if (preview.registrationCardBackUrl) {
        URL.revokeObjectURL(preview.registrationCardBackUrl);
      }
    });
    this.vehicleDocumentPreviews.clear();
    this.closeVehicleImagePreview();
  }
  // Position initiale (fallback: Tunis)
  center: google.maps.LatLngLiteral = {
    lat: 36.8065,
    lng: 10.1815
  };
  markerPosition: google.maps.LatLngLiteral = { ...this.center };
  zoom = 12;

  // Etat de la map
  mapsLoaded = true;
  errorMsg = '';
  activeRouteDelivery: { id: string; to: string } | null = null;
  directionsResult: google.maps.DirectionsResult | null = null;
  routePath: google.maps.LatLngLiteral[] = [];
  routeDistanceText = '';
  routeEtaText = '';
  isLiveRouteUpdating = false;
  destinationMarkerPosition: google.maps.LatLngLiteral | null = null;
  routePolylineOptions: google.maps.PolylineOptions = {
    strokeColor: '#2563eb',
    strokeOpacity: 0.95,
    strokeWeight: 6
  };
  private activeDestinationAddress: string | null = null;
  private activeTrackingDeliveryId: number | null = null;
  private lastTrackingUpdateAt = 0;
  private readonly minTrackingUpdateIntervalMs = 15000;
  private lastRouteRefreshAt = 0;
  private lastRouteRefreshPosition: google.maps.LatLngLiteral | null = null;
  private readonly minRouteRefreshIntervalMs = 12000;
  private readonly minRouteRefreshDistanceMeters = 25;

  // Fonctions utilisees dans le template
  mapsReady(): boolean {
    return this.mapsLoaded;
  }

  mapsError(): string {
    return this.errorMsg;
  }

  openMapTab(): void {
    this.activeTab = 'map';
    this.requestCurrentLocation();
  }

  private getDeliveryNumericId(deliveryId: string): number | null {
    const numeric = Number(deliveryId.replace(/^DEL\s*/i, ''));
    return Number.isFinite(numeric) ? numeric : null;
  }

  private updateDeliveryTrackingIfNeeded(): void {
    if (!this.activeTrackingDeliveryId || !this.markerPosition) {
      return;
    }

    const now = Date.now();
    if (now - this.lastTrackingUpdateAt < this.minTrackingUpdateIntervalMs) {
      return;
    }

    this.lastTrackingUpdateAt = now;

    const currentLocation = `${this.markerPosition.lat.toFixed(6)},${this.markerPosition.lng.toFixed(6)}`;
    const estimatedArrival = new Date(Date.now() + 10 * 60000).toISOString();

    this.deliveryApi.updateDeliveryTracking(this.activeTrackingDeliveryId, {
      currentLocation,
      lastUpdate: new Date().toISOString(),
      estimatedArrival
    }).subscribe({
      next: () => {
        // Tracking state saved to backend silently.
      },
      error: () => {
        // Ignore intermittent tracking failures.
      }
    });
  }

  private setActiveTrackingDelivery(delivery: { id: string }): void {
    const numericId = this.getDeliveryNumericId(delivery.id);
    if (numericId === null) {
      this.activeTrackingDeliveryId = null;
      return;
    }
    this.activeTrackingDeliveryId = numericId;
  }

  onMapInitialized(): void {
    this.renderNativeRouteOnMap();
  }

  acceptDelivery(delivery: { id: string; to: string }): void {
    this.activeRouteDelivery = delivery;
    this.activeDestinationAddress = delivery.to;
    this.setActiveTrackingDelivery(delivery);
    this.activeTab = 'map';
    this.errorMsg = '';
    this.directionsResult = null;
    this.routePath = [];
    this.routeDistanceText = '';
    this.routeEtaText = '';
    this.isLiveRouteUpdating = true;
    this.lastRouteRefreshAt = 0;
    this.lastRouteRefreshPosition = null;
    this.destinationMarkerPosition = null;
    this.clearNativePolyline();
    this.clearMarkers();
    this.cdr.detectChanges();

    if (!delivery.to?.trim()) {
      this.errorMsg = 'Destination address is missing for this delivery.';
      this.cdr.detectChanges();
      return;
    }

    this.requestCurrentLocation(() => {
      this.drawRouteToDestination(delivery.to, true);
      this.updateDeliveryTrackingIfNeeded();
    });
  }

  markDeliveryInProgress(delivery: { id: string }): void {
    const confirmed = window.confirm('Confirm setting this delivery to “In Progress”?');
    if (!confirmed) {
      return;
    }

    const deliveryId = Number(delivery.id.replace('DEL', ''));
    if (Number.isNaN(deliveryId)) {
      return;
    }

    const sourceDelivery = this.pendingDeliveriesSource.find((item) => item.id === deliveryId);
    if (!sourceDelivery) {
      return;
    }

    this.deliveryApi.deliveries.update(deliveryId, {
      deliverytype: sourceDelivery.deliverytype,
      status: 'In progress',
      deliverydate: sourceDelivery.deliverydate,
      address: sourceDelivery.address,
      orderId: sourceDelivery.order?.id,
      vehiculeId: sourceDelivery.vehicule?.id,
      courier: sourceDelivery.courier?.id ? { id: sourceDelivery.courier.id } : undefined,
      tracking: sourceDelivery.tracking?.id ? { id: sourceDelivery.tracking.id } : undefined
    }).subscribe({
      next: (updatedDelivery) => {
        // Synchronize order status with delivery status
        this.syncOrderStatus(updatedDelivery);
        this.pendingDeliveriesSource = this.pendingDeliveriesSource.map((item) =>
          item.id === updatedDelivery.id ? updatedDelivery : item
        );
        this.refreshPendingDeliveriesView();
      },
      error: () => {
        window.alert('Impossible de mettre a jour le statut de la livraison.');
      }
    });
  }

  startCancellation(deliveryId: string): void {
    this.cancellingDeliveryId = deliveryId;
    this.cancellationReason = '';
  }

  cancelCancellation(): void {
    this.cancellingDeliveryId = null;
    this.cancellationReason = '';
  }

  markDeliveryCancelled(delivery: { id: string }): void {
    const reason = this.cancellationReason.trim();
    if (!reason) {
      window.alert('Please provide a reason for cancellation.');
      return;
    }

    const confirmed = window.confirm('Confirm cancelling this delivery?');
    if (!confirmed) {
      return;
    }

    const deliveryId = Number(delivery.id.replace('DEL', ''));
    if (Number.isNaN(deliveryId)) {
      return;
    }

    const sourceDelivery = this.pendingDeliveriesSource.find((item) => item.id === deliveryId);
    if (!sourceDelivery) {
      return;
    }

    this.deliveryApi.deliveries.update(deliveryId, {
      deliverytype: sourceDelivery.deliverytype,
      status: 'Cancelled',
      cancellationReason: reason,
      deliverydate: sourceDelivery.deliverydate,
      address: sourceDelivery.address,
      orderId: sourceDelivery.order?.id,
      vehiculeId: sourceDelivery.vehicule?.id,
      courier: sourceDelivery.courier?.id ? { id: sourceDelivery.courier.id } : undefined,
      tracking: sourceDelivery.tracking?.id ? { id: sourceDelivery.tracking.id } : undefined
    }).subscribe({
      next: (updatedDelivery) => {
        this.syncOrderStatus(updatedDelivery);
        this.cancellingDeliveryId = null;
        this.cancellationReason = '';
        this.loadPendingDeliveries();
      },
      error: () => {
        window.alert('Impossible de mettre a jour le statut de la livraison.');
      }
    });
  }




markDeliveryDelivered(delivery: { id: string; from: string; to: string; distance: number; price: number; priority: string; date?: string; vehicule?: string }): void {
    const confirmed = window.confirm('Confirm setting this delivery to "Delivered"?');
    if (!confirmed) {
      return;
    }

    const deliveryId = Number(delivery.id.replace('DEL', ''));
    if (Number.isNaN(deliveryId)) {
      return;
    }

    const sourceDelivery = this.pendingDeliveriesSource.find((item) => item.id === deliveryId);
    if (!sourceDelivery) {
      return;
    }

    this.deliveryApi.deliveries.update(deliveryId, {
      deliverytype: sourceDelivery.deliverytype,
      status: 'Delivered',
      deliverydate: sourceDelivery.deliverydate,
      address: sourceDelivery.address,
      orderId: sourceDelivery.order?.id,
      vehiculeId: sourceDelivery.vehicule?.id,
      courier: sourceDelivery.courier?.id ? { id: sourceDelivery.courier.id } : undefined,
      tracking: sourceDelivery.tracking?.id ? { id: sourceDelivery.tracking.id } : undefined
    }).subscribe({
      next: (updatedDelivery) => {
        // Synchronize order status with delivery status
        this.syncOrderStatus(updatedDelivery);
        // Recharger depuis l'API pour mettre à jour pending et delivered
        this.loadPendingDeliveries();
      },
      error: () => {
        window.alert('Impossible de mettre a jour le statut de la livraison.');
      }
    });
  }








  private requestCurrentLocation(onPositionReady?: () => void): void {
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      this.errorMsg = 'Geolocation is not supported by this browser.';
      onPositionReady?.();
      return;
    }

    if (this.geoWatchId !== null) {
      navigator.geolocation.clearWatch(this.geoWatchId);
      this.geoWatchId = null;
    }

    navigator.geolocation.getCurrentPosition(
      (position: GeolocationPosition) => {
        this.ngZone.run(() => {
          const nextPosition: google.maps.LatLngLiteral = {
            lat: position.coords.latitude,
            lng: position.coords.longitude
          };

          this.applyDriverPositionUpdate(nextPosition, true);
          this.zoom = 15;
          this.errorMsg = '';
          onPositionReady?.();
          this.cdr.detectChanges();
        });
      },
      (error: GeolocationPositionError) => {
        this.ngZone.run(() => {
          if (error.code === error.PERMISSION_DENIED) {
            this.errorMsg = 'Location permission denied. Using current/default location for route.';
            onPositionReady?.();
            this.cdr.detectChanges();
            return;
          }

          if (error.code === error.TIMEOUT) {
            this.errorMsg = 'Location timeout. Using current/default location for route.';
            onPositionReady?.();
            this.cdr.detectChanges();
            return;
          }

          this.errorMsg = 'Unable to get your location. Using current/default location for route.';
          onPositionReady?.();
          this.cdr.detectChanges();
        });
      },
      {
        enableHighAccuracy: true,
        timeout: 20000,
        maximumAge: 0
      }
    );

    // Keep the marker updated if the user moves.
    this.geoWatchId = navigator.geolocation.watchPosition(
      (position: GeolocationPosition) => {
        this.ngZone.run(() => {
          const nextPosition: google.maps.LatLngLiteral = {
            lat: position.coords.latitude,
            lng: position.coords.longitude
          };

          this.applyDriverPositionUpdate(nextPosition, true);
          this.updateDeliveryTrackingIfNeeded();
          this.refreshLiveRouteIfNeeded();
          this.cdr.detectChanges();
        });
      },
      () => {
        // Keep current position if live tracking fails.
      },
      {
        enableHighAccuracy: true,
        timeout: 20000,
        maximumAge: 0
      }
    );
  }

  private refreshLiveRouteIfNeeded(): void {
    if (!this.activeDestinationAddress) {
      return;
    }

    const now = Date.now();
    if (now - this.lastRouteRefreshAt < this.minRouteRefreshIntervalMs) {
      return;
    }

    if (this.lastRouteRefreshPosition) {
      const movedMeters = this.getDistanceInMeters(this.lastRouteRefreshPosition, this.markerPosition);
      if (movedMeters < this.minRouteRefreshDistanceMeters) {
        return;
      }
    }

    this.drawRouteToDestination(this.activeDestinationAddress, false);
  }

  private getDistanceInMeters(from: google.maps.LatLngLiteral, to: google.maps.LatLngLiteral): number {
    const earthRadiusMeters = 6371000;
    const latDelta = this.toRadians(to.lat - from.lat);
    const lngDelta = this.toRadians(to.lng - from.lng);
    const fromLatRad = this.toRadians(from.lat);
    const toLatRad = this.toRadians(to.lat);

    const a =
      Math.sin(latDelta / 2) * Math.sin(latDelta / 2) +
      Math.cos(fromLatRad) * Math.cos(toLatRad) *
      Math.sin(lngDelta / 2) * Math.sin(lngDelta / 2);

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return earthRadiusMeters * c;
  }

  private toRadians(degrees: number): number {
    return (degrees * Math.PI) / 180;
  }

  private normalizeDeliveryAddress(address: string | null | undefined): string {
    return (address ?? '').trim().toLowerCase();
  }

  private drawRouteToDestination(destinationAddress: string, forceRefresh: boolean): void {
    if (!forceRefresh && this.isLiveRouteUpdating) {
      return;
    }

    this.isLiveRouteUpdating = true;
    const directionsService = new google.maps.DirectionsService();

    directionsService.route(
      {
        origin: this.markerPosition,
        destination: destinationAddress,
        travelMode: google.maps.TravelMode.DRIVING,
        drivingOptions: {
          departureTime: new Date(),
          trafficModel: google.maps.TrafficModel.BEST_GUESS
        }
      },
      (result: google.maps.DirectionsResult | null, status: string) => {
        this.ngZone.run(() => {
          this.isLiveRouteUpdating = false;
          if (status !== 'OK' || !result) {
            this.drawFallbackStraightRoute(destinationAddress, status);
            this.cdr.detectChanges();
            return;
          }

          const routeLeg = result.routes?.[0]?.legs?.[0];
          const distanceValueKm = routeLeg?.distance?.value ? routeLeg.distance.value / 1000 : null;
          this.routeDistanceText = routeLeg?.distance?.text ??
            (distanceValueKm !== null ? `${distanceValueKm.toFixed(1)} km` : '');
          this.routeEtaText = routeLeg?.duration_in_traffic?.text ?? routeLeg?.duration?.text ?? '';
          this.lastRouteRefreshAt = Date.now();
          this.lastRouteRefreshPosition = { ...this.markerPosition };

          const endLocation = result.routes?.[0]?.legs?.[0]?.end_location;
          this.destinationMarkerPosition = endLocation
            ? { lat: endLocation.lat(), lng: endLocation.lng() }
            : null;

          const mappedPath = result.routes?.[0]?.overview_path?.map((point) => ({
            lat: point.lat(),
            lng: point.lng()
          })) ?? [];

          if (mappedPath.length >= 2) {
            this.routePath = mappedPath;
            this.renderNativeRouteOnMap();
            this.cdr.detectChanges();
            return;
          }

          // Some responses may not expose an overview path in this setup.
          this.drawFallbackStraightRoute(destinationAddress, 'EMPTY_PATH');
          this.cdr.detectChanges();
        });
      }
    );
  }
// lorsque le livreur ca marche et sa posiition ce modifie
  private applyDriverPositionUpdate(nextPosition: google.maps.LatLngLiteral, keepCentered: boolean): void {
    this.center = nextPosition;
    this.markerPosition = nextPosition;

    const map = this.mapComponent?.googleMap;
    if (!map) {
      return;
    }

    if (!this.currentPositionMarker) {
      this.currentPositionMarker = new google.maps.Marker({
        position: nextPosition,
        map,
        title: 'Votre position',
        icon: 'http://maps.google.com/mapfiles/ms/icons/blue-dot.png'
      });
    } else {
      this.currentPositionMarker.setPosition(nextPosition);
    }

    if (this.nativeRoutePolyline && this.routePath.length >= 2) {
      this.routePath = [nextPosition, ...this.routePath.slice(1)];
      this.nativeRoutePolyline.setPath(this.routePath);
    }

    if (keepCentered) {
      map.panTo(nextPosition);
    }
    this.cdr.detectChanges();
  }

  private drawFallbackStraightRoute(destinationAddress: string, reason: string): void {
    const geocoder = new google.maps.Geocoder();
    geocoder.geocode({ address: destinationAddress }, (results, status) => {
      this.ngZone.run(() => {
        this.isLiveRouteUpdating = false;
        const first = results?.[0];
        const location = first?.geometry?.location;
        if (status !== google.maps.GeocoderStatus.OK || !location) {
          this.routePath = [];
          this.routeDistanceText = '';
          this.routeEtaText = '';
          this.destinationMarkerPosition = null;
          this.errorMsg = `Unable to display route (${reason}/${status}).`;
          this.cdr.detectChanges();
          return;
        }

        const destinationPoint = { lat: location.lat(), lng: location.lng() };
        this.destinationMarkerPosition = destinationPoint;
        this.routePath = [this.markerPosition, destinationPoint];
        const directDistanceKm = this.getDistanceInMeters(this.markerPosition, destinationPoint) / 1000;
        this.routeDistanceText = `${directDistanceKm.toFixed(1)} km (approx)`;
        const etaMinutes = Math.max(1, Math.round((directDistanceKm / 35) * 60));
        this.routeEtaText = `${etaMinutes} min (approx)`;
        this.lastRouteRefreshAt = Date.now();
        this.lastRouteRefreshPosition = { ...this.markerPosition };
        this.errorMsg = `Displayed direct route fallback (${reason}).`;
        this.renderNativeRouteOnMap();
        this.cdr.detectChanges();
      });
    });
  }

  private clearNativePolyline(): void {
    if (this.nativeRoutePolyline) {
      this.nativeRoutePolyline.setMap(null);
      this.nativeRoutePolyline = null;
    }
  }

  private clearMarkers(): void {
    if (this.currentPositionMarker) {
      this.currentPositionMarker.setMap(null);
      this.currentPositionMarker = null;
    }
    if (this.destinationMarker) {
      this.destinationMarker.setMap(null);
      this.destinationMarker = null;
    }
  }

  private renderNativeRouteOnMap(): void {
    const map = this.mapComponent?.googleMap;
    if (!map) {
      return;
    }

    // Always show current position marker
    this.clearMarkers();
    this.currentPositionMarker = new google.maps.Marker({
      position: this.markerPosition,
      map,
      title: 'Votre position',
      icon: 'http://maps.google.com/mapfiles/ms/icons/blue-dot.png'
    });

    // Only draw route if we have a path
    if (this.routePath.length < 2) {
      return;
    }

    this.clearNativePolyline();

    this.nativeRoutePolyline = new google.maps.Polyline({
      path: this.routePath,
      strokeColor: '#2563eb',
      strokeOpacity: 0.95,
      strokeWeight: 6,
      map
    });

    // Add marker for destination (red)
    if (this.destinationMarkerPosition) {
      this.destinationMarker = new google.maps.Marker({
        position: this.destinationMarkerPosition,
        map,
        title: 'Destination',
        icon: 'http://maps.google.com/mapfiles/ms/icons/red-dot.png'
      });
    }

    const bounds = new google.maps.LatLngBounds();
    bounds.extend(this.markerPosition);
    this.routePath.forEach((point) => bounds.extend(point));
    if (this.destinationMarkerPosition) {
      bounds.extend(this.destinationMarkerPosition);
    }
    map.fitBounds(bounds, 50);
    this.cdr.detectChanges();
  }

  quizUserId(): number | null {
    const profileId = this.profileUser?.id;
    if (typeof profileId === 'number' && profileId > 0) {
      return profileId;
    }

    const authId = this.authService.currentUser()?.id;
    if (typeof authId === 'number' && authId > 0) {
      return authId;
    }

    return null;
  }

  /**
   * Synchronizes the order status with the delivery status.
   * When delivery status changes, the corresponding order's status is updated to match.
   * For example:
   * - Delivery status "In progress" → Order status "In progress"
   * - Delivery status "Delivered" → Order status "Delivered"
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
    this.resetVerifyFlow();
    this.authService.logout();
    this.router.navigate(['/login']);
  }
blueIcon = {
  url: 'http://maps.google.com/mapfiles/ms/icons/blue-dot.png'
};
}
