import { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import * as ordersApi from '../api/orders';
import type { Order } from '../api/types';
import { watchOrder } from '../api/sse';
import { ApiRequestError } from '../api/client';
import { ErrorBanner, Spinner } from '../components/ui';
import { Mascot } from '../components/Mascot';
import { formatCfa, formatDateTime } from '../utils/format';
import { statusInfo } from '../utils/orderStatus';

export function OrderDetailPage() {
  const { orderId = '' } = useParams();
  const [order, setOrder] = useState<Order | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [paymentPending, setPaymentPending] = useState<string | null>(null);
  const [deliveryCode, setDeliveryCode] = useState<string | null>(null);

  const refresh = useRef(() => {
    ordersApi
      .getOrder(orderId)
      .then(setOrder)
      .catch(() => setError('Commande introuvable.'));
  });

  useEffect(() => {
    refresh.current();
    const stop = watchOrder(orderId, {
      onStatusChange: () => refresh.current(),
    });
    return stop;
  }, [orderId]);

  async function run(action: () => Promise<Order | void>, successMessage?: string) {
    setBusy(true);
    setError(null);
    try {
      const result = await action();
      if (result) setOrder(result);
      else refresh.current();
      if (successMessage) setNotice(successMessage);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.detail ?? err.message : 'Action impossible pour le moment.');
    } finally {
      setBusy(false);
    }
  }

  if (error && !order) return <div className="container"><ErrorBanner message={error} /></div>;
  if (!order) return <Spinner label="Chargement de la commande…" />;

  const info = statusInfo(order.status);

  return (
    <div className="container">
      <div className="card" style={{ textAlign: 'center', marginBottom: 16 }}>
        <Mascot pose={info.pose} size={110} />
        <h1 style={{ fontSize: 22, marginTop: 8 }}>{info.label}</h1>
        <p className="muted" style={{ marginTop: 4 }}>{info.description}</p>
        {order.statusReason && <p style={{ marginTop: 6, color: 'var(--rouge)', fontWeight: 700 }}>{order.statusReason}</p>}
      </div>

      {error && <ErrorBanner message={error} />}
      {notice && <div className="badge vert" style={{ display: 'block', marginBottom: 12 }}>{notice}</div>}

      <div className="card stack">
        {order.lines.map((line) => (
          <div key={line.offerId + line.type} className="row" style={{ justifyContent: 'space-between' }}>
            <span>{line.productName} ({line.type === 'REFILL' ? 'recharge' : 'neuve'}) × {line.quantity}</span>
            <span>{formatCfa(line.total)}</span>
          </div>
        ))}
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <span className="muted">Transport</span>
          <span>{formatCfa(order.transportFee)}</span>
        </div>
        <div className="row" style={{ justifyContent: 'space-between', fontWeight: 800, fontSize: 19 }}>
          <span>Total</span>
          <span>{formatCfa(order.total)}</span>
        </div>
        {order.pricesFrozen && <span className="badge neutre">Prix figé</span>}
      </div>

      {deliveryCode && (
        <div className="card" style={{ marginTop: 12, textAlign: 'center' }}>
          <p className="muted">Ton code de livraison (démo : normalement reçu par SMS)</p>
          <p style={{ fontSize: 32, fontWeight: 800, letterSpacing: 4 }}>{deliveryCode}</p>
        </div>
      )}

      <div className="stack" style={{ marginTop: 16 }}>
        {order.status === 'ACCEPTED' && (
          <button
            className="btn block"
            disabled={busy}
            onClick={() =>
              run(async () => {
                const start = await ordersApi.payOrder(order.id);
                if (start.status !== 'SUCCEEDED') {
                  setPaymentPending(start.providerReference);
                } else {
                  setPaymentPending(null);
                }
              }, 'Paiement lancé.')
            }
          >
            Payer {formatCfa(order.total)}
          </button>
        )}

        {paymentPending && (
          <div className="card stack" style={{ borderColor: 'var(--orange)' }}>
            <p className="muted">Paiement en attente de confirmation Mobile Money (simulation).</p>
            <div className="row">
              <button
                className="btn secondary"
                disabled={busy}
                onClick={() => run(async () => {
                  await ordersApi.simulateFakePayment(paymentPending, true);
                  setPaymentPending(null);
                })}
              >
                Simuler succès
              </button>
              <button
                className="btn ghost"
                disabled={busy}
                onClick={() => run(async () => {
                  await ordersApi.simulateFakePayment(paymentPending, false);
                  setPaymentPending(null);
                })}
              >
                Simuler échec
              </button>
            </div>
          </div>
        )}

        {(order.status === 'OUT_FOR_DELIVERY' || order.status === 'DELIVERED') && (
          <button
            className="btn secondary"
            disabled={busy}
            onClick={() =>
              run(async () => {
                const r = await ordersApi.regenerateDeliveryCode(order.id);
                setDeliveryCode(r.code);
              })
            }
          >
            Revoir mon code de livraison
          </button>
        )}

        {order.status === 'DELIVERED' && (
          <button className="btn block" disabled={busy} onClick={() => run(() => ordersApi.confirmReceipt(order.id), 'Réception confirmée, merci !')}>
            Confirmer la réception
          </button>
        )}

        {(order.status === 'DRAFT' || order.status === 'INTENT_SENT' || order.status === 'ACCEPTED') && (
          <button className="btn danger" disabled={busy} onClick={() => run(() => ordersApi.cancelOrder(order.id))}>
            Annuler la commande
          </button>
        )}

        {['PAID', 'IN_PREPARATION', 'OUT_FOR_DELIVERY', 'DELIVERED'].includes(order.status) && (
          <button
            className="btn ghost"
            disabled={busy}
            onClick={() => {
              const reason = window.prompt('Explique le problème rencontré :');
              if (reason) run(() => ordersApi.disputeOrder(order.id, reason));
            }}
          >
            Signaler un problème
          </button>
        )}

        {(order.status === 'VALIDATED' || order.status === 'FUNDS_RELEASED') && order.buyerRating == null && (
          <div className="card">
            <p className="muted" style={{ marginBottom: 8 }}>Note le dépôt :</p>
            <div className="row">
              {[1, 2, 3, 4, 5].map((n) => (
                <button key={n} className="btn ghost" disabled={busy} onClick={() => run(() => ordersApi.rateOrder(order.id, n))}>
                  {n}★
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      <p className="muted" style={{ marginTop: 20, fontSize: 13 }}>
        Commande passée le {formatDateTime(order.createdAt)}
      </p>
    </div>
  );
}
