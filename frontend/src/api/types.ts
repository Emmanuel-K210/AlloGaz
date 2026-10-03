// Types alignés sur les DTO du backend (ci.allogaz.*.infrastructure.web.*Dtos)
// Les noms de champs JSON correspondent aux composants des records Java (Jackson).

export type Role = 'BUYER' | 'SELLER' | 'ADMIN';

export interface User {
  id: string;
  phone: string;
  displayName: string | null;
  roles: Role[];
  createdAt: string;
}

export interface AuthTokens {
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
  tokenType: string;
  newUser: boolean;
  user: User;
}

export interface OtpRequested {
  expiresInSeconds: number;
}

// --- Catalogue ---

export interface CategoryView {
  id: string;
  slug: string;
  name: string;
}

/** Le prix (recharge/achat) est national et réglementé : identique chez tous les dépôts, modifiable par l'admin seul. */
export interface ProductView {
  id: string;
  categorySlug: string;
  name: string;
  brand: string | null;
  company: string | null;
  bottleColors: string[];
  appearance: string | null;
  capacityGrams: number | null;
  refillPrice: number | null;
  purchasePrice: number | null;
}

export type DeliveryMode = 'INCLUDED' | 'FIXED_FEE' | 'PICKUP_ONLY';

export interface OpeningSlot {
  day: string; // DayOfWeek Java: MONDAY, TUESDAY, ...
  opensAt: string; // "HH:mm:ss"
  closesAt: string;
}

export interface SellerProfile {
  id: string;
  userId: string;
  shopName: string;
  address: string | null;
  latitude: number;
  longitude: number;
  deliveryRadiusMeters: number;
  deliveryMode: DeliveryMode;
  deliveryFee: number;
  openingHours: OpeningSlot[];
  acceptingOrders: boolean;
  status: 'PENDING' | 'VERIFIED' | 'SUSPENDED';
  /** Point d'échange toutes marques : reprend une bouteille vide d'une autre société en échange. */
  universalExchange: boolean;
  createdAt: string;
}

export interface SellerProfileInput {
  shopName: string;
  address?: string;
  latitude: number;
  longitude: number;
  deliveryRadiusMeters: number;
  deliveryMode: DeliveryMode;
  deliveryFee: number;
  openingHours: OpeningSlot[];
  universalExchange: boolean;
}

export interface Offer {
  id: string;
  sellerId: string;
  productId: string;
  productName: string;
  brand: string | null;
  company: string | null;
  bottleColors: string[];
  appearance: string | null;
  capacityGrams: number | null;
  refillPrice: number | null;
  purchasePrice: number | null;
  stock: number;
  active: boolean;
}

export interface PublicSeller {
  profile: SellerProfile;
  openNow: boolean;
  offers: Offer[];
}

// --- Recherche ---

export type SaleType = 'PURCHASE' | 'REFILL';

export interface MatchingOffer {
  offerId: string;
  productId: string;
  productName: string;
  brand: string | null;
  company: string | null;
  bottleColors: string[];
  appearance: string | null;
  capacityGrams: number | null;
  refillPrice: number | null;
  purchasePrice: number | null;
  stock: number;
}

export interface SellerResult {
  sellerId: string;
  shopName: string;
  address: string | null;
  latitude: number;
  longitude: number;
  distanceMeters: number;
  deliversToYou: boolean;
  deliveryMode: DeliveryMode;
  deliveryFee: number;
  rating: number;
  ratingCount: number;
  acceptanceRate: number;
  available: boolean;
  score: number;
  offers: MatchingOffer[];
  universalExchange: boolean;
}

export type SearchCriteria = {
  lat: number;
  lon: number;
  productId?: string;
  brand?: string;
  company?: string;
  color?: string;
  sizeKg?: number;
  type?: SaleType;
  limit?: number;
};

// --- Commandes ---

export type Fulfillment = 'DELIVERY' | 'PICKUP';

export type OrderStatus =
  | 'DRAFT'
  | 'INTENT_SENT'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'EXPIRED'
  | 'PAID'
  | 'IN_PREPARATION'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'VALIDATED'
  | 'FUNDS_RELEASED'
  | 'CANCELLED'
  | 'DISPUTED';

export interface OrderLine {
  offerId: string;
  productId: string;
  productName: string;
  type: SaleType;
  quantity: number;
  unitPrice: number;
  total: number;
}

export interface Order {
  id: string;
  status: OrderStatus;
  buyerId: string;
  sellerId: string;
  fulfillment: Fulfillment;
  deliveryAddress: string | null;
  deliveryLatitude: number | null;
  deliveryLongitude: number | null;
  lines: OrderLine[];
  itemsTotal: number;
  transportFee: number;
  total: number;
  pricesFrozen: boolean;
  createdAt: string;
  responseDeadline: string | null;
  acceptedAt: string | null;
  paidAt: string | null;
  deliveredAt: string | null;
  autoValidateAt: string | null;
  validatedAt: string | null;
  closedAt: string | null;
  statusReason: string | null;
  disputeOutcome: 'RELEASE_TO_SELLER' | 'REFUND_BUYER' | null;
  deliveryCodeAttempts: number;
  buyerRating: number | null;
}

export interface CreateOrderLine {
  offerId: string;
  type: SaleType;
  quantity: number;
}

export interface CreateOrderInput {
  sellerId: string;
  fulfillment: Fulfillment;
  deliveryAddress?: string;
  deliveryLatitude?: number;
  deliveryLongitude?: number;
  lines: CreateOrderLine[];
  submit: boolean;
}

export interface PaymentStart {
  paymentId: string;
  providerReference: string;
  status: string;
  checkoutUrl: string | null;
}

export interface DeliveryCodeResponse {
  code: string;
}

export interface OrderStatusEvent {
  orderId: string;
  from: OrderStatus | null;
  to: OrderStatus;
  at: string;
}

export interface ApiError {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
}
