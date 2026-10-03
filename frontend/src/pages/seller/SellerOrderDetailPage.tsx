import { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import * as ordersApi from '../../api/orders';
import * as sellerApi from '../../api/seller';
import type { Order } from '../../api/types';
import { watchOrder } from '../../api/sse';
import { ApiRequestError } from '../../api/client';
import { ErrorBanner, Spinner } from '../../components/ui';
import { Mascot } from '../../components/Mascot';
import { formatCfa, formatDateTime } from '../../utils/format';
import { statusInfo } from '../../utils/orderStatus';

export function SellerOrderDetailPage() {
  const { orderId = '' } = useParams();
  const [order, setOrder] = useState<Order | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [code, setCode] = useState('');

  const refresh = useRef(() => {
    ordersApi.getOrder(orderId).then(setOrder).catch(() => setError('Commande introuvable.'));
  });

  useEffect(() => {
    refresh.current();
    return watchOrder(orderId, { onStatusChange: () => refresh.current() });
  }, [orderId]);

  async function run(action: () => Promise<Order>) {
    setBusy(true);
    setError(null);
    try {
      setOrder(await action());
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.detail ?? err.message : 'Action impossible.');
    } finally {
      setBusy(false);
    }
  }

  if (error && !order) return <div className="container"><ErrorBanner message={error} /></div>;
  if (!order) return <Spinner label="Chargement…" />;

  const info = statusInfo(order.status);

  return (
    <div className="container">
      <div className="card" style={{ textAlign: 'center', marginBottom: 16 }}>
        <Mascot pose={info.pose} size={100} />
        <h1 style={{ fontSize: 22, marginTop: 6 }}>{info.label}</h1>
        {order.deliveryAddress && <p className="muted" style={{ marginTop: 4 }}>Livraison : {order.deliveryAddress}</p>}
      </div>

      {error && <ErrorBanner message={error} />}

      <div className="card stack">
        {order.lines.map((line) => (
          <div key={line.offerId + line.type} className="row" style={{ justifyContent: 'space-between' }}>
            <span>{line.productName} ({line.type === 'REFILL' ? 'recharge' : 'neuve'}) × {line.quantity}</span>
            <span>{formatCfa(line.total)}</span>
          </div>
        ))}
        <div className="row" style={{ justifyContent: 'space-between', fontWeight: 800, fontSize: 19 }}>
          <span>Total</span>
          <span>{formatCfa(order.total)}</span>
        </div>
      </div>

      <div className="stack" style={{ marginTop: 16 }}>
        {order.status === 'INTENT_SENT' && (
          <>
            <button className="btn block" disabled={busy} onClick={() => run(() => sellerApi.acceptOrder(order.id))}>
              Accepter la commande
            </button>
            <button
              className="btn danger"
              disabled={busy}
              onClick={() => {
                const reason = window.prompt('Raison du refus (optionnel) :') ?? undefined;
                run(() => sellerApi.rejectOrder(order.id, reason));
              }}
            >
              Refuser
            </button>
          </>
        )}

        {order.status === 'PAID' && (
          <button className="btn block" disabled={busy} onClick={() => run(() => sellerApi.prepareOrder(order.id))}>
            Démarrer la préparation
          </button>
        )}

        {order.status === 'IN_PREPARATION' && (
          <button className="btn block" disabled={busy} onClick={() => run(() => sellerApi.dispatchOrder(order.id))}>
            {order.fulfillment === 'PICKUP' ? 'Prête pour le retrait' : 'Envoyer en livraison'}
          </button>
        )}

        {order.status === 'OUT_FOR_DELIVERY' && (
          <div className="card stack">
            <p className="muted">Demande le code à 4 chiffres reçu par l'acheteur :</p>
            <div className="row">
              <input
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 4))}
                placeholder="0000"
                inputMode="numeric"
                style={{ width: 100, fontSize: 22, textAlign: 'center' }}
              />
              <button
                className="btn"
                disabled={busy || code.length !== 4}
                onClick={() => run(() => sellerApi.submitDeliveryCode(order.id, code))}
              >
                Valider le code
              </button>
            </div>
            <button className="btn ghost" disabled={busy} onClick={() => run(() => sellerApi.markDelivered(order.id))}>
              Pas de code : marquer livré (validation auto sous 24 h)
            </button>
          </div>
        )}

        {order.deliveryCodeAttempts > 0 && order.status === 'OUT_FOR_DELIVERY' && (
          <p className="muted" style={{ fontSize: 13 }}>Essais de code déjà effectués : {order.deliveryCodeAttempts} / 5</p>
        )}
      </div>

      <p className="muted" style={{ marginTop: 20, fontSize: 13 }}>Reçue le {formatDateTime(order.createdAt)}</p>
    </div>
  );
}
