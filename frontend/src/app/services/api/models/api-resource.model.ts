export type ApiResourceId = number;

export interface RelationId {
  id: ApiResourceId;
}

export interface ApiResource {
  id: ApiResourceId;
}

export enum Role {
  USER = 'USER',
  COURIER = 'COURIER',
  ADMIN_DELIVERY = 'ADMIN_DELIVERY',
  SELLER = 'SELLER',
  ORGANIZER = 'ORGANIZER',
  SERVICE_PROVIDER = 'SERVICE_PROVIDER',
  PARTNER = 'PARTNER',
  EVENT = 'EVENT',
  ADMIN = 'ADMIN',
  ADMIN_MARKET = 'ADMIN_MARKET',
  RECRUITER = 'RECRUITER'
}

export interface AuthRequest {
  email: string;
  password: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  email: string;
  code: string;
  newPassword: string;
}

export interface ApiMessageResponse {
  message?: string;
}

export interface AuthResponse {
  token: string;
  email: string;
  name: string;
  role: string;
  userId?: number;
  id?: number;
  refreshToken?: string;
}

export enum ProjectStatus {
  PLANNED = 'PLANNED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

export enum ServiceStatus {
  AVAILABLE = 'AVAILABLE',
  UNAVAILABLE = 'UNAVAILABLE',
  BOOKED = 'BOOKED'
}

export type CourierStatus = 'PENDING' | 'ACCEPTED' | 'REFUSED';
export type CourierProfileStatus = 'INCOMPLETE' | 'COMPLETED';

export interface VerificationResponse {
  verdict: string;
  reason?: string;
  similarityPercent?: number;
  faceVerified?: boolean;
  permitName?: string;
  permitNumber?: string;
  expiryDate?: string;
}

export interface UserResource extends ApiResource {
  name: string;
  email: string;
  password: string;
  role: Role;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface CourierResource extends ApiResource {
  name: string;
  status: string;
  vehicule?: VehiculeResource;
}

export interface CourierProfileResponse {
  id?: ApiResourceId;
  userId?: ApiResourceId;
  name?: string;
  email?: string;
  phoneNumber?: string;
  role?: string;
  status?: CourierStatus | string;
  profileStatus?: CourierProfileStatus | string;
  permitImageUploaded?: boolean;
  hasPermitImage?: boolean;
  permitImageExists?: boolean;
}

export interface CourierProfileUpdatePayload {
  phoneNumber?: string;
}

export interface DeliveryCourierContactResponse {
  courierId?: number;
  userId?: number;
  name?: string;
  email?: string;
  phoneNumber?: string;
}

export interface VehicleInfoDto {
  serie: string;
  type: string;
}

export interface AdminCourierInfoResponse {
  courierId: number;
  userId: number;
  nom: string;
  prenom: string;
  email: string;
  phoneNumber?: string;
  phone_number?: string;
  status?: CourierStatus | string;
  quizResult?: string;
  quizScore?: number;
  meetingLink?: string;
  interviewDate?: string;
  voitures: VehicleInfoDto[];
  permitImage: string;
}

export interface CourierStatisticsResponse {
  courierId: number;
  userId: number;
  courierName: string;
  courierEmail: string;
  courierStatus: CourierStatus | string;
  totalVehicles: number;
  totalDeliveries: number;
  deliveredDeliveries: number;
  pendingDeliveries: number;
  cancelledDeliveries: number;
  totalDistanceKm: number;
  averageDistanceKm: number;
  completionRate: number;
}

export interface TopCourierResponse {
  courierId: number;
  userId: number;
  courierName: string;
  courierEmail: string;
  deliveredDeliveries: number;
}

export interface DateCount {
  date: string;
  count: number;
}

export interface MonthCount {
  month: string;
  count: number;
}

export interface AdminDeliveryOverviewResponse {
  ordersPerDay?: DateCount[];
  deliveriesPerDay?: DateCount[];
  interviewsPerDay?: DateCount[];

  totalOrders?: number;
  totalDeliveries?: number;

  pendingDeliveries?: number;
  acceptedDeliveries?: number;
  deliveredDeliveries?: number;
  refusedDeliveries?: number;
  cancelledDeliveries?: number;

  totalVehicles?: number;

  totalCouriers?: number;
  acceptedCouriers?: number;
  pendingCouriers?: number;
  refusedCouriers?: number;

  couriersPerMonth?: MonthCount[];
}

export interface DeliveryResource extends ApiResource {
  deliverytype: string;
  status: string;
  cancellationReason?: string;
  deliverydate: string;
  address: string;
  distanceKm?: number;
  deliveryAddress?: string;
  city?: string;
  postalCode?: string;
  phoneNumber?: string;
  deliveryMode?: string;
  paymentMode?: string;
  connectedUserId?: number;
  order?: OrderResource;
  courier?: CourierResource;
  tracking?: MapTrackingResource;
  vehicule?: VehiculeResource | null;
}

export interface DeliveryAddressDetailsResource extends ApiResource {
  deliveryAddress: string;
  city: string;
  postalCode: string;
  phoneNumber: string;
  connectedUserId?: number;
  delivery?: DeliveryResource;
}

export interface MapTrackingResource extends ApiResource {
  currentLocation: string;
  lastUpdate: string;
  estimatedArrival: string;
}

export interface VehiculeResource extends ApiResource {
  type: string;
  registrationnumbers: string;
  capacity: number;
  status: string;
  vehiclePhotoFileName?: string;
  vehiclePhotoContentType?: string;
  registrationCardFrontFileName?: string;
  registrationCardFrontContentType?: string;
  registrationCardBackFileName?: string;
  registrationCardBackContentType?: string;
  courier?: CourierResource;
}

export interface CollaborationResource extends ApiResource {
  name: string;
  type: string;
  description: string;
  event?: EventResource;
}

export interface EquipmentResource extends ApiResource {
  name: string;
  type: string;
  status: string;
  quantity?: number;
  imageUrl?: string;
  event?: EventResource;
}

export interface EquipmentReservationResource extends ApiResource {
  quantity: number;
  reservation?: ReservationResource;
  equipment?: EquipmentResource;
  stall?: StallResource;
}

export interface EventResource extends ApiResource {
  name: string;
  date: string;
  location: string;
  description?: string;
  online?: boolean;
  meetingLink?: string;
  status?: string;
  nbTickets?: number;
  tickets?: TicketResource[];
}

export interface ReservationResource extends ApiResource {
  name: string;
  date: string;
  event?: EventResource;
}

export interface StallResource extends ApiResource {
  name: string;
  number: number;
  event?: EventResource;
  user?: any;
}

export interface TicketResource extends ApiResource {
  type: string;
  price: number;
  event?: EventResource;
  user?: any;
}

export interface TicketPromoOfferResource {
  date: string;
  label: string;
  discountRate: number;
  discountLabel: string;
}

export interface CategoryResource extends ApiResource {
  name: string;
  description: string;
  store?: StoreResource;
}

export interface OrderResource extends ApiResource {
  date: string;
  status: string;
  totalAmount: number;
  paymentMethod: string;
  shippingAddress: string;
  user?: any;
  orderLines?: OrderLineResource[];
  delivery?: DeliveryResource;
}

export interface OrderLineResource extends ApiResource {
  quantity: number;
  price: number;
  subtotal: number;
  dimensionsLabel?: string;
  weight?: number;
  order?: OrderResource;
  product?: ProductResource;
}

export interface ProductResource extends ApiResource {
  name: string;
  description: string;
  price: number;
  stock: number;
  imageUrl: string;
  originalPrice?: number | null;
  discountPercent?: number | null;
  promoStartAt?: string | null;
  promoEndAt?: string | null;
  discountedPrice?: number | null;
  remainingSeconds?: number | null;
  isPromotionActive?: boolean;
  promotionStatus?: string | null;
  dimensionsLabel?: string;
  weight?: number;
  soldQuantity?: number | null;
  store?: StoreResource;
  category?: CategoryResource;
  categoryId?: number;
  categoryName?: string;
  reviews?: ReviewResource[];
  orderLines?: OrderLineResource[];
  users?: any[];
}

export interface ReviewResource extends ApiResource {
  comment: string;
  rating: number;
  product?: ProductResource;
  user?: any;
}

export interface StoreResource extends ApiResource {
  name: string;
  description: string;
  address: string;
  phone: string;
  rating: number;
  balance?: number;
  categories?: Array<{ id: number; name: string } | string>;
  owner?: UserResource;
}

export interface StoreStatsResource {
  totalRevenue: number;
  totalOrders: number;
  bestSeller?: {
    productId?: number | null;
    productName?: string | null;
    totalQuantity: number;
    revenue: number;
  } | null;
  monthlyRevenue: Array<{
    month: string;
    revenue: number;
  }>;
}

export interface PartnerResource extends ApiResource {
  name: string;
  contactInfo: string;
  role: Role;
}

export interface ProjectResource extends ApiResource {
  title: string;
  details: string;
  startDate: string;
  estimatedEndDate: string;
  endDate: string;
  budget: number;
  status: ProjectStatus;
  priority: string;
}

export interface ServiceResource extends ApiResource {
  name: string;
  description: string;
  category: string;
  hourlyRate: number;
  status: ServiceStatus;
  rating: number;
  location: string;
  provider?: UserResource;
}

export interface ServiceRequestResource extends ApiResource {
  date: string;
  duration: number;
  status: string;
  user?: UserResource;
  service?: ServiceResource;
  partner?: PartnerResource;
}

export interface ServiceReviewResource extends ApiResource {
  comment: string;
  rating: number;
  user?: UserResource;
  service?: ServiceResource;
}

export interface UserWritePayload {
  name: string;
  email: string;
  password: string;
  role: Role;
}

export interface CourierWritePayload {
  name: string;
  status: string;
  vehicule?: RelationId;
}

export interface DeliveryWritePayload {
  deliverytype: string;
  status: string;
  cancellationReason?: string;
  deliverydate: string;
  address: string;
  distanceKm?: number;
  realWeight?: number;
  length?: number;
  width?: number;
  height?: number;
  addressDetailsId?: number | null;
  deliveryAddress?: string;
  city?: string;
  postalCode?: string;
  phoneNumber?: string;
  deliveryMode?: string;
  paymentMode?: string;
  orderId?: number | null;
  vehiculeId?: number | null;
  courier?: RelationId;
  tracking?: RelationId;
}

export interface DeliveryAddressDetailsWritePayload {
  deliveryAddress: string;
  city: string;
  postalCode: string;
  phoneNumber: string;
  deliveryId?: number | null;
}

export interface MapTrackingWritePayload {
  currentLocation: string;
  lastUpdate: string;
  estimatedArrival: string;
}

export interface VehiculeWritePayload {
  type: string;
  registrationnumbers: string;
  capacity: number;
  status: string;
  courier?: RelationId;
}

export interface CollaborationWritePayload {
  name: string;
  type: string;
  description: string;
  event?: RelationId;
}

export interface EquipmentWritePayload {
  name: string;
  type: string;
  status: string;
  quantity?: number;
  imageUrl?: string;
  event?: RelationId;
}

export interface EquipmentReservationWritePayload {
  quantity: number;
  reservation?: RelationId;
  equipment?: RelationId;
  stall?: RelationId;
}

export interface EventWritePayload {
  name: string;
  date: string;
  location: string;
  description?: string;
  online?: boolean;
  meetingLink?: string;
  nbTickets?: number;
}

export interface ReservationWritePayload {
  name: string;
  date: string;
  event?: RelationId;
}

export interface StallWritePayload {
  name: string;
  number: number;
  location: string;
  event?: RelationId;
  user?: RelationId;
}

export interface TicketWritePayload {
  type: string;
  price: number;
  eventId: number;
  originalPrice?: number;
  discountApplied?: boolean;
  discountRate?: number;
  discountLabel?: string;
  user?: RelationId;
}

export interface TicketPromoSelectionWritePayload {
  eventId: number;
  discountPercent: number;
  discountLabel?: string;
}

export interface CategoryWritePayload {
  name: string;
  description: string;
  store?: RelationId;
}

export interface OrderWritePayload {
  date: string;
  status: string;
  totalAmount: number;
  paymentMethod?: string;
  shippingAddress: string;
  userId?: number;
  user?: RelationId;
  orderLines?: Array<{
    quantity: number;
    price: number;
    productId: number;
    dimensionsLabel?: string;
    weight?: number;
  }>;
  delivery?: RelationId;
}

export interface OrderLineWritePayload {
  quantity: number;
  price: number;
  subtotal: number;
  dimensionsLabel?: string;
  weight?: number;
  orderId: number;
  productId: number;
}

export interface ProductWritePayload {
  name: string;
  description: string;
  price: number;
  stock: number;
  imageUrl: string;
  dimensionsLabel?: string;
  weight?: number;
  categoryId?: number;
  store?: RelationId;
  category?: RelationId;
  reviews?: RelationId[];
  orderLines?: RelationId[];
  users?: RelationId[];
}

export interface ProductPromotionPayload {
  discountPercent: number;
  promoEndAt: string;
}

export interface ReviewWritePayload {
  comment: string;
  rating: number;
  product?: RelationId;
  user?: RelationId;
}

export interface StoreWritePayload {
  name: string;
  description: string;
  address: string;
  phone: string;
  rating: number;
  owner?: RelationId;
}

export interface PartnerWritePayload {
  name: string;
  contactInfo: string;
  role: Role;
}

export interface ProjectWritePayload {
  title: string;
  details: string;
  startDate: string;
  estimatedEndDate: string;
  endDate: string;
  budget: number;
  status: ProjectStatus;
  priority: string;
}

export interface ServiceWritePayload {
  name: string;
  description?: string;
  category: string;
  hourlyRate: number;
  status: ServiceStatus;
  rating: number;
  location: string;
  providerId: number;
}

export interface ServiceRequestWritePayload {
  date: string;
  duration: number;
  status: string;
  userId: number;
  serviceId: number;
  partnerId?: number;
}

export interface ServiceReviewWritePayload {
  comment: string;
  rating: number;
  userId: number;
  serviceId: number;
}

export interface VisualSearchRequest {
  imageBase64: string;
}

export interface VisualSearchResponseResource {
  detectedType: string;
  colors: string[];
  style: string;
  keywords: string[];
  results: Array<{
    product: ProductResource;
    similarityScore: number;
  }>;
}

export interface SemanticSearchRequest {
  query: string;
}

export interface SemanticSearchResponseResource {
  originalQuery: string;
  correctedQuery: string;
  category: string;
  occasion: string;
  season: string;
  extractedKeywords: string[];
  products: ProductResource[];
}

export interface PartnerCompanyResource extends ApiResource {
  name: string;
  sector: string;
  contactEmail: string;
  partnershipStatus: string;
}

export interface PartnerCompanyWritePayload {
  name: string;
  sector: string;
  contactEmail: string;
  partnershipStatus: string;
}

export interface JobOfferResource extends ApiResource {
  title: string;
  description: string;
  type: string;
  status: string;
  location: string;
  experienceLevel: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
  requiredSkills: string | string[];
  company?: PartnerCompanyResource;
}

export interface JobOfferWritePayload {
  title: string;
  description: string;
  type: string;
  status: string;
  location: string;
  experienceLevel: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
  requiredSkills: string;
  companyId: number;
}

export interface ApplicationResource extends ApiResource {
  status: string;
  matchingScore: number;
  motivation?: string;
  applicantId?: number;
  jobOfferId?: number;
  skills?: string[];
  experienceLevel?: string;
  fieldOfStudy?: string;
  yearsOfExperience?: string;
  languages?: string[];
  applicant?: UserResource;
  jobOffer?: JobOfferResource;
  interviews?: any[];
  createdAt?: string;
  lastCandidateActionAt?: string;
  activityStatus?: 'ACTIVE' | 'INACTIVE' | 'AT_RISK';
  flagged?: boolean;
}

export interface ApplicationWritePayload {
  status: string;
  matchingScore: number;
  applicantId: number;
  jobOfferId: number;
  motivation?: string;
}

export interface InterviewResource extends ApiResource {
  interviewDate: string;
  type: string;
  location?: string;
  status: 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';
  result?: 'ACCEPTED' | 'REJECTED' | 'WAITING_LIST' | null;
  resultNotes?: string | null;
  applicationId: number;
  studentFirstName: string;
  studentLastName: string;
  jobTitle: string;
}

export interface InterviewWritePayload {
  interviewDate?: string;
  type?: 'PHONE' | 'VIDEO' | 'IN_PERSON';
  location?: string;
  applicationId?: number;
  status?: 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';
  result?: 'ACCEPTED' | 'REJECTED' | 'WAITING_LIST' | null;
  resultNotes?: string | null;
}

export interface JobOfferPerformanceDTO {
  jobOfferId: number;
  title: string;
  type: string;
  status: string;
  companyName: string;
  applicationCount: number;
  averageMatchingScore: number;
  interviewCompletionRate: number;
}
