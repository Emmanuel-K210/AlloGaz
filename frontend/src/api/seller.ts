import { apiFetch } from './client';
import type { Offer, Order, OrderStatus, SellerProfile, SellerProfileInput } from './types';

export function applyAsSeller(input: SellerProfileInput) {
  return apiFetch<SellerProfile>('/seller/profile', { method: 'POST', body: input });
}

export function myProfile() {
  return apiFetch<SellerProfile>('/seller/profile');
}

export function updateProfile(input: SellerProfileInput) {
  return apiFetch<SellerProfile>('/seller/profile', { method: 'PUT', body: input });
}

export function openShop() {
  return apiFetch<SellerProfile>('/seller/profile/open', { method: 'POST' });
}

export function pauseShop() {
  return apiFetch<SellerProfile>('/seller/profile/pause', { method: 'POST' });
}

export function myOffers() {
  return apiFetch<Offer[]>('/seller/offers');
}

export interface OfferInput {
  refillPrice?: number;
  purchasePrice?: number;
  stock: number;
  active?: boolean;
}

export function upsertOffer(productId: string, input: OfferInput) {
  return apiFetch<Offer>(`/seller/offers/${productId}`, { method: 'PUT', body: input });
}

export interface SellerBalance {
  sellerId: string;
  amount: number;
}

export function balance() {
  return apiFetch<SellerBalance>('/seller/balance');
}

export function sellerOrders(status?: OrderStatus) {
  return apiFetch<Order[]>('/seller/orders', { query: { status } });
}

export function acceptOrder(orderId: string) {
  return apiFetch<Order>(`/seller/orders/${orderId}/accept`, { method: 'POST' });
}

export function rejectOrder(orderId: string, reason?: string) {
  return apiFetch<Order>(`/seller/orders/${orderId}/reject`, {
    method: 'POST',
    body: reason ? { reason } : undefined,
  });
}

export function prepareOrder(orderId: string) {
  return apiFetch<Order>(`/seller/orders/${orderId}/prepare`, { method: 'POST' });
}

export function dispatchOrder(orderId: string) {
  return apiFetch<Order>(`/seller/orders/${orderId}/dispatch`, { method: 'POST' });
}

export function markDelivered(orderId: string) {
  return apiFetch<Order>(`/seller/orders/${orderId}/delivered`, { method: 'POST' });
}

export function submitDeliveryCode(orderId: string, code: string) {
  return apiFetch<Order>(`/seller/orders/${orderId}/delivery-code`, { method: 'POST', body: { code } });
}
