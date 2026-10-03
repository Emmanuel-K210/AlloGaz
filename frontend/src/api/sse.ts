import { API_BASE, accessTokenForEventSource } from './client';
import type { OrderStatusEvent } from './types';

export interface OrderSseHandlers {
  onSnapshot?: (status: string) => void;
  onStatusChange?: (event: OrderStatusEvent) => void;
  onError?: () => void;
}

/** Ouvre le flux SSE d'une commande. EventSource ne peut pas poser d'en-tête : le jeton passe en paramètre. */
export function watchOrder(orderId: string, handlers: OrderSseHandlers): () => void {
  const token = accessTokenForEventSource();
  const url = `${API_BASE}/orders/${orderId}/events?access_token=${encodeURIComponent(token ?? '')}`;
  const source = new EventSource(url);

  source.addEventListener('snapshot', (e) => {
    try {
      const data = JSON.parse((e as MessageEvent).data) as { to: string };
      handlers.onSnapshot?.(data.to);
    } catch {
      // ignoré
    }
  });

  source.addEventListener('order-status', (e) => {
    try {
      const data = JSON.parse((e as MessageEvent).data) as OrderStatusEvent;
      handlers.onStatusChange?.(data);
    } catch {
      // ignoré
    }
  });

  source.onerror = () => {
    handlers.onError?.();
  };

  return () => source.close();
}

/** Flux de toutes les commandes de l'utilisateur courant (nouvelles intentions côté vendeur, mises à jour côté acheteur). */
export function watchMyOrders(onStatusChange: (event: OrderStatusEvent) => void): () => void {
  const token = accessTokenForEventSource();
  const url = `${API_BASE}/me/events?access_token=${encodeURIComponent(token ?? '')}`;
  const source = new EventSource(url);

  source.addEventListener('order-status', (e) => {
    try {
      onStatusChange(JSON.parse((e as MessageEvent).data) as OrderStatusEvent);
    } catch {
      // ignoré
    }
  });

  return () => source.close();
}
