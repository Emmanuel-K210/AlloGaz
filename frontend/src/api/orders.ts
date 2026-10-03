import { apiFetch } from './client';
import type { CreateOrderInput, DeliveryCodeResponse, Order, PaymentStart } from './types';

export function createOrder(input: CreateOrderInput) {
  return apiFetch<Order>('/orders', { method: 'POST', body: input });
}

export function myOrders() {
  return apiFetch<Order[]>('/orders');
}

export function getOrder(orderId: string) {
  return apiFetch<Order>(`/orders/${orderId}`);
}

export function submitOrder(orderId: string) {
  return apiFetch<Order>(`/orders/${orderId}/submit`, { method: 'POST' });
}

export function payOrder(orderId: string) {
  return apiFetch<PaymentStart>(`/orders/${orderId}/pay`, { method: 'POST' });
}

export function regenerateDeliveryCode(orderId: string) {
  return apiFetch<DeliveryCodeResponse>(`/orders/${orderId}/delivery-code`, { method: 'POST' });
}

export function confirmReceipt(orderId: string) {
  return apiFetch<Order>(`/orders/${orderId}/confirm-receipt`, { method: 'POST' });
}

export function cancelOrder(orderId: string, reason?: string) {
  return apiFetch<Order>(`/orders/${orderId}/cancel`, { method: 'POST', body: reason ? { reason } : undefined });
}

export function disputeOrder(orderId: string, reason: string) {
  return apiFetch<Order>(`/orders/${orderId}/dispute`, { method: 'POST', body: { reason } });
}

export function rateOrder(orderId: string, stars: number) {
  return apiFetch<Order>(`/orders/${orderId}/rating`, { method: 'POST', body: { stars } });
}

export function simulateFakePayment(reference: string, success: boolean) {
  return apiFetch<void>(`/payments/fake/${reference}/complete`, {
    method: 'POST',
    query: { success },
    auth: false,
  });
}
