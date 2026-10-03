import { useEffect, useState } from 'react';
import * as adminApi from '../../api/admin';
import type { Order } from '../../api/types';
import { EmptyState, ErrorBanner, Spinner } from '../../components/ui';
import { formatCfa, formatDateTime } from '../../utils/format';

export function AdminDisputesPage() {
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

  function load() {
    adminApi
      .disputedOrders()
      .then(setOrders)
      .catch(() => setError('Impossible de charger les litiges.'));
  }

  useEffect(load, []);

  async function resolve(orderId: string, outcome: adminApi.DisputeOutcome) {
    const note = window.prompt('Note pour cette décision (optionnel) :') ?? undefined;
    setBusy(orderId);
    try {
      await adminApi.resolveDispute(orderId, outcome, note);
      load();
    } catch {
      setError('La résolution a échoué.');
    } finally {
      setBusy(null);
    }
  }

  if (error) return <ErrorBanner message={error} />;
  if (!orders) return <Spinner label="Chargement…" />;
  if (orders.length === 0) return <EmptyState pose="sleep">Aucun litige en cours.</EmptyState>;

  return (
    <div className="stack">
      {orders.map((order) => (
        <div key={order.id} className="card stack">
          <div className="row" style={{ justifyContent: 'space-between' }}>
            <strong>{formatCfa(order.total)}</strong>
            <span className="muted">{formatDateTime(order.createdAt)}</span>
          </div>
          {order.statusReason && <p>{order.statusReason}</p>}
          <div className="row">
            <button className="btn secondary" disabled={busy === order.id} onClick={() => resolve(order.id, 'RELEASE_TO_SELLER')}>
              Libérer au vendeur
            </button>
            <button className="btn danger" disabled={busy === order.id} onClick={() => resolve(order.id, 'REFUND_BUYER')}>
              Rembourser l'acheteur
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
