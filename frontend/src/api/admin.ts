import { apiFetch } from './client';
import type { Order, ProductView, SellerProfile } from './types';

export function pendingSellers(status: 'PENDING' | 'VERIFIED' | 'SUSPENDED' = 'PENDING') {
  return apiFetch<SellerProfile[]>('/admin/sellers', { query: { status } });
}

export function verifySeller(sellerId: string) {
  return apiFetch<SellerProfile>(`/admin/sellers/${sellerId}/verify`, { method: 'POST' });
}

export function suspendSeller(sellerId: string) {
  return apiFetch<SellerProfile>(`/admin/sellers/${sellerId}/suspend`, { method: 'POST' });
}

export interface AdminProductInput {
  categorySlug: string;
  name: string;
  brand?: string;
  company?: string;
  bottleColors?: string[];
  appearance?: string;
  capacityGrams?: number;
  refillPrice?: number;
  purchasePrice?: number;
}

export function createProduct(input: AdminProductInput) {
  return apiFetch<ProductView>('/admin/products', { method: 'POST', body: input });
}

/** Tarif national (recharge et/ou achat), par exemple quand le syndicat des gaziers change le prix officiel. */
export function updateProductPrice(productId: string, refillPrice?: number, purchasePrice?: number) {
  return apiFetch<ProductView>(`/admin/products/${productId}/price`, {
    method: 'PUT',
    body: { refillPrice, purchasePrice },
  });
}

export function disputedOrders() {
  return apiFetch<Order[]>('/admin/disputes');
}

export type DisputeOutcome = 'RELEASE_TO_SELLER' | 'REFUND_BUYER';

export function resolveDispute(orderId: string, outcome: DisputeOutcome, note?: string) {
  return apiFetch<Order>(`/admin/orders/${orderId}/resolve`, { method: 'POST', body: { outcome, note } });
}
