import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { CrudApiService } from './crud-api.service';
import { ApiConfigService } from './api-config.service';
import {
  AdminCourierInfoResponse,
  CourierStatisticsResponse,
  DeliveryAddressDetailsResource,
  DeliveryAddressDetailsWritePayload,
  CourierStatus,
  CourierProfileResponse,
  CourierProfileUpdatePayload,
  DeliveryCourierContactResponse,
  CourierResource,
  CourierWritePayload,
  DeliveryResource,
  DeliveryWritePayload,
  MapTrackingResource,
  MapTrackingWritePayload,
  TopCourierResponse,
  AdminDeliveryOverviewResponse,
  VerificationResponse,
  VehiculeResource,
  VehiculeWritePayload
} from './models/api-resource.model';

export interface VehiculeMultipartPayload extends VehiculeWritePayload {
  vehiclePhoto?: File | null;
  registrationCardFront?: File | null;
  registrationCardBack?: File | null;
}

@Injectable({ providedIn: 'root' })
export class DeliveryApiService {
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);
  private readonly vehiculesBaseUrl = this.apiConfig.buildUrl('/api/delivery/vehicules');
  private readonly courierProfileBaseUrl = this.apiConfig.buildUrl('/api/delivery/courier-profile/me');
  private readonly myAddressDetailsBaseUrl = this.apiConfig.buildUrl('/api/delivery/address-details/me');
  private readonly adminCouriersBaseUrl = this.apiConfig.buildUrl('/api/admin/livreurs');

  readonly couriers = new CrudApiService<CourierResource, CourierWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/delivery/couriers')
  );

  readonly deliveries = new CrudApiService<DeliveryResource, DeliveryWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/delivery/deliveries')
  );

  readonly mapTracking = new CrudApiService<MapTrackingResource, MapTrackingWritePayload>(
    this.http,
    this.apiConfig.buildUrl('/api/delivery/map-tracking')
  );

  readonly addressDetails = new CrudApiService<DeliveryAddressDetailsResource, DeliveryAddressDetailsWritePayload>(
    this.http,
    this.myAddressDetailsBaseUrl
  );

  readonly vehicules = new CrudApiService<VehiculeResource, VehiculeWritePayload>(
    this.http,
    this.vehiculesBaseUrl,
    this.apiConfig.buildUrl('/api/delivery/vehicules/voitures')
  );

  listAllVehicules(): Observable<VehiculeResource[]> {
    return this.http.get<VehiculeResource[]>(this.vehiculesBaseUrl);
  }

  getMyDeliveries(): Observable<DeliveryResource[]> {
    return this.http.get<DeliveryResource[]>(
      this.apiConfig.buildUrl('/api/delivery/deliveries/me')
    );
  }

//ajouter image de permis de conduire pour les livreurs
  getMyCourierProfile(): Observable<CourierProfileResponse> {
    return this.http.get<CourierProfileResponse>(this.courierProfileBaseUrl);
  }

  updateMyCourierProfile(payload: CourierProfileUpdatePayload): Observable<CourierProfileResponse> {
    return this.http.put<CourierProfileResponse>(this.courierProfileBaseUrl, payload);
  }

  uploadMyPermitImage(file: File): Observable<CourierProfileResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.put<CourierProfileResponse>(`${this.courierProfileBaseUrl}/permit-image`, formData);
  }

  getMyPermitImage(): Observable<Blob> {
    return this.http.get(`${this.courierProfileBaseUrl}/permit-image`, { responseType: 'blob' });
  }

  verifyMyPermitWithSelfie(selfie: File): Observable<VerificationResponse> {
    const formData = new FormData();
    formData.append('selfie', selfie);
    return this.http.post<VerificationResponse>(`${this.courierProfileBaseUrl}/verify-permit`, formData);
  }

  getCourierByVehiculeId(vehiculeId: number): Observable<DeliveryCourierContactResponse> {
    return this.http.get<DeliveryCourierContactResponse>(
      `${this.apiConfig.buildUrl('/api/delivery/courier-profile/by-vehicule')}/${vehiculeId}`
    );
  }

  listAdminCouriers(): Observable<AdminCourierInfoResponse[]> {
    return this.http.get<AdminCourierInfoResponse[]>(this.adminCouriersBaseUrl);
  }

  getCourierStatistics(courierId: number): Observable<CourierStatisticsResponse> {
    return this.http.get<CourierStatisticsResponse>(`${this.adminCouriersBaseUrl}/${courierId}/statistics`);
  }

  getTopCouriersByDeliveredDeliveries(limit = 5): Observable<TopCourierResponse[]> {
    const safeLimit = Math.max(1, Math.min(limit, 50));
    return this.http.get<TopCourierResponse[]>(
      `${this.apiConfig.buildUrl('/api/delivery/couriers')}/top-couriers`,
      { params: { limit: String(safeLimit) } }
    );
  }
// ajouter une méthode pour mettre à jour le statut du livreur

  updateAdminCourierStatus(courierId: number, status: CourierStatus): Observable<AdminCourierInfoResponse> {
    return this.http.put<AdminCourierInfoResponse>(
      `${this.adminCouriersBaseUrl}/${courierId}/status`,
      null,
      { params: { status } }
    );
  }

  updateAdminCourierInterviewDate(
    courierId: number,
    interviewDate: string,
    sendEmail = false,
    notificationMessage?: string
  ): Observable<AdminCourierInfoResponse> {
    return this.http.put<AdminCourierInfoResponse>(
      `${this.adminCouriersBaseUrl}/${courierId}/interview-date`,
      {
        interviewDate,
        sendEmail,
        notificationMessage
      }
    );
  }

  createVehiculeWithDocuments(payload: VehiculeMultipartPayload): Observable<VehiculeResource> {
    return this.http.post<VehiculeResource>(this.vehiculesBaseUrl, this.buildVehiculeFormData(payload));
  }

  updateVehiculeWithDocuments(id: number, payload: VehiculeMultipartPayload): Observable<VehiculeResource> {
    return this.http.put<VehiculeResource>(`${this.vehiculesBaseUrl}/${id}`, this.buildVehiculeFormData(payload));
  }

  getVehiclePhoto(vehicleId: number): Observable<Blob> {
    return this.http.get(`${this.vehiculesBaseUrl}/${vehicleId}/vehicle-photo`, { responseType: 'blob' });
  }

  getRegistrationCardFront(vehicleId: number): Observable<Blob> {
    return this.http.get(`${this.vehiculesBaseUrl}/${vehicleId}/registration-card/front`, { responseType: 'blob' });
  }

  getRegistrationCardBack(vehicleId: number): Observable<Blob> {
    return this.http.get(`${this.vehiculesBaseUrl}/${vehicleId}/registration-card/back`, { responseType: 'blob' });
  }

  getDeliveryQrCode(deliveryId: number): Observable<Blob> {
    return this.http.get(`${this.apiConfig.buildUrl('/api/delivery/deliveries')}/${deliveryId}/qr-code`, { responseType: 'blob' });
  }

  getInvoice(deliveryId: number): Observable<Blob> {
    return this.http.get(`${this.apiConfig.buildUrl('/api/delivery/deliveries')}/${deliveryId}/invoice`, { responseType: 'blob' });
  }

  scanDeliveryQrToken(token: string): Observable<DeliveryResource> {
    const safeToken = encodeURIComponent(token.trim());
    return this.http.get<DeliveryResource>(`${this.apiConfig.buildUrl('/api/delivery/deliveries')}/scan/${safeToken}`);
  }

  getDeliveryTracking(deliveryId: number): Observable<MapTrackingResource> {
    return this.http.get<MapTrackingResource>(`${this.apiConfig.buildUrl('/api/delivery/deliveries')}/${deliveryId}/tracking`);
  }

  updateDeliveryTracking(deliveryId: number, payload: MapTrackingWritePayload): Observable<MapTrackingResource> {
    return this.http.patch<MapTrackingResource>(
      `${this.apiConfig.buildUrl('/api/delivery/deliveries')}/${deliveryId}/tracking`,
      payload
    );
  }

  getAdminInterviewWeek(date?: string): Observable<any> {
    const url = this.apiConfig.buildUrl('/api/quiz/admin/interviews/week');
    const params = date ? { date } : undefined;
    return this.http.get<any>(url, { params });
  }

  getDashboardStatistics(days = 7): Observable<AdminDeliveryOverviewResponse> {
    const url = this.apiConfig.buildUrl('/api/admin/delivery/overview');
    return this.http.get<AdminDeliveryOverviewResponse>(url, { params: { days: String(days) } });
  }

  private buildVehiculeFormData(payload: VehiculeMultipartPayload): FormData {
    const formData = new FormData();
    formData.append('type', payload.type);
    formData.append('registrationnumbers', payload.registrationnumbers);
    formData.append('capacity', String(payload.capacity));
    formData.append('status', payload.status);

    if (payload.vehiclePhoto) {
      formData.append('vehiclePhoto', payload.vehiclePhoto);
    }
    if (payload.registrationCardFront) {
      formData.append('registrationCardFront', payload.registrationCardFront);
    }
    if (payload.registrationCardBack) {
      formData.append('registrationCardBack', payload.registrationCardBack);
    }

    return formData;
  }
}
