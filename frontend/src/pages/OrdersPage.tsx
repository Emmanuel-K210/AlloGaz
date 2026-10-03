import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { myOrders } from '../api/orders';
import type { Order } from '../api/types';
import { EmptyState, ErrorBanner, Spinner, StatusBadge } from '../components/ui';
import { formatCfa, formatDateTime } from '../utils/format';
import { statusInfo } from '../utils/orderStatus';

export function OrdersPage() {
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    myOrders()
      .then(setOrders)
      .catch(() => setError('Impossible de charger tes commandes.'));
  }, []);

  return (
    <div className="container">
      <h1 style={{ fontSize: 26, marginBottom: 16 }}>Mes commandes</h1>
      {error && <ErrorBanner message={error} />}
      {!orders && !error && <Spinner label="Chargement de tes commandes…" />}
      {orders && orders.length === 0 && (
        <EmptyState pose="sleep">Tu n'as pas encore passé de commande. Cherche un dépôt pour commencer.</EmptyState>
      )}
      <div className="stack">
        {orders
          ?.slice()
          .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
          .map((order) => (
            <Link key={order.id} to={`/commandes/${order.id}`} className="card">
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
