import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import * as sellerApi from '../../api/seller';
import { watchMyOrders } from '../../api/sse';
import type { Order, OrderStatus } from '../../api/types';
import { EmptyState, ErrorBanner, Spinner, StatusBadge } from '../../components/ui';
import { formatCfa, formatDateTime } from '../../utils/format';
import { statusInfo } from '../../utils/orderStatus';

const FILTERS: { value: OrderStatus | ''; label: string }[] = [
  { value: '', label: 'Toutes' },
  { value: 'INTENT_SENT', label: 'Nouvelles' },
  { value: 'ACCEPTED', label: 'Acceptées' },
  { value: 'PAID', label: 'Payées' },
  { value: 'IN_PREPARATION', label: 'En préparation' },
  { value: 'OUT_FOR_DELIVERY', label: 'En livraison' },
  { value: 'DISPUTED', label: 'Litiges' },
];

export function SellerOrdersPage() {
  const [status, setStatus] = useState<OrderStatus | ''>('');
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  function load() {
    sellerApi
      .sellerOrders(status || undefined)
      .then(setOrders)
      .catch(() => setError('Impossible de charger les commandes.'));
  }

  useEffect(load, [status]);
  useEffect(() => watchMyOrders(() => load()), []); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="container">
      <h1 style={{ fontSize: 24, marginBottom: 12 }}>Commandes reçues</h1>
      <div className="row" style={{ flexWrap: 'wrap', marginBottom: 16 }}>
        {FILTERS.map((f) => (
          <button
            key={f.value}
            className={`badge ${status === f.value ? 'bleu' : 'neutre'}`}
            style={{ border: 'none', cursor: 'pointer' }}
            onClick={() => setStatus(f.value)}
          >
            {f.label}
          </button>
        ))}
      </div>

      {error && <ErrorBanner message={error} />}
      {!orders && !error && <Spinner label="Chargement des commandes…" />}
      {orders && orders.length === 0 && <EmptyState pose="sleep">Aucune commande dans cette catégorie.</EmptyState>}

      <div className="stack">
        {orders
          ?.slice()
          .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
          .map((order) => (
            <Link key={order.id} to={`/vendeur/commandes/${order.id}`} className="card">
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <strong>{formatCfa(order.total)}</strong>
                <StatusBadge info={statusInfo(order.status)} />
              </div>
              <p className="muted" style={{ fontSize: 14, marginTop: 4 }}>
                {order.lines.length} article{order.lines.length > 1 ? 's' : ''} · {formatDateTime(order.createdAt)}
              </p>
            </Link>
          ))}
      </div>
    </div>
  );
}
