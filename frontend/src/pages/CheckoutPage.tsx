import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../cart/CartContext';
import { createOrder } from '../api/orders';
import { publicSeller } from '../api/catalog';
import type { Fulfillment, PublicSeller } from '../api/types';
import { ApiRequestError } from '../api/client';
import { ErrorBanner, Spinner, EmptyState } from '../components/ui';
import { formatCfa } from '../utils/format';
import { useGeolocation } from '../utils/useGeolocation';

export function CheckoutPage() {
  const cart = useCart();
  const navigate = useNavigate();
  const [seller, setSeller] = useState<PublicSeller | null>(null);
  const [fulfillment, setFulfillment] = useState<Fulfillment>('DELIVERY');
  const [address, setAddress] = useState('');
  const { coords } = useGeolocation();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!cart.sellerId) return;
    publicSeller(cart.sellerId).then((s) => {
      setSeller(s);
      if (s.profile.deliveryMode === 'PICKUP_ONLY') setFulfillment('PICKUP');
    });
  }, [cart.sellerId]);

  if (!cart.sellerId || cart.lines.length === 0) {
    return (
      <div className="container">
        <EmptyState pose="sleep">
          Ton panier est vide. Choisis un dépôt pour commencer une commande.
        </EmptyState>
      </div>
    );
  }

  const transportFee = fulfillment === 'DELIVERY' ? (seller?.profile.deliveryFee ?? 0) : 0;
  const total = cart.totalAmount + transportFee;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const order = await createOrder({
        sellerId: cart.sellerId!,
        fulfillment,
        deliveryAddress: fulfillment === 'DELIVERY' ? address || undefined : undefined,
        deliveryLatitude: fulfillment === 'DELIVERY' ? coords.lat : undefined,
        deliveryLongitude: fulfillment === 'DELIVERY' ? coords.lon : undefined,
        lines: cart.lines.map((l) => ({ offerId: l.offerId, type: l.type, quantity: l.quantity })),
        submit: true,
      });
      cart.clear();
      navigate(`/commandes/${order.id}`);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.detail ?? err.message : 'La commande n\'a pas pu être envoyée.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="container">
      <h1 style={{ fontSize: 24, marginBottom: 4 }}>Ta commande</h1>
      <p className="muted" style={{ marginBottom: 16 }}>{cart.sellerName}</p>

      {error && <ErrorBanner message={error} />}
      {!seller && <Spinner label="Vérification du dépôt…" />}

      <div className="stack">
        {cart.lines.map((line) => (
          <div key={`${line.offerId}-${line.type}`} className="card row" style={{ justifyContent: 'space-between' }}>
            <div>
              <strong>{line.productName}</strong>
              <p className="muted" style={{ fontSize: 13 }}>
                {line.type === 'REFILL' ? 'Recharge' : 'Bouteille neuve'} × {line.quantity}
              </p>
            </div>
            <div className="row">
              <span>{formatCfa(line.unitPrice * line.quantity)}</span>
              <button className="btn ghost" type="button" onClick={() => cart.removeLine(line.offerId, line.type)}>
                ✕
              </button>
            </div>
          </div>
        ))}
      </div>

      <form className="stack card" style={{ marginTop: 16 }} onSubmit={handleSubmit}>
        <div className="field">
          <label>Mode de réception</label>
          <div className="row">
            <label className="row" style={{ gap: 4 }}>
              <input
                type="radio"
                checked={fulfillment === 'DELIVERY'}
                disabled={seller?.profile.deliveryMode === 'PICKUP_ONLY'}
                onChange={() => setFulfillment('DELIVERY')}
              />
              Livraison
            </label>
            <label className="row" style={{ gap: 4 }}>
              <input type="radio" checked={fulfillment === 'PICKUP'} onChange={() => setFulfillment('PICKUP')} />
              Retrait sur place
            </label>
          </div>
        </div>

        {fulfillment === 'DELIVERY' && (
          <div className="field">
            <label htmlFor="address">Adresse de livraison</label>
            <input
              id="address"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              placeholder="Quartier, rue, repère…"
              required
            />
          </div>
        )}

        <div className="stack" style={{ borderTop: '2px solid var(--line)', paddingTop: 10 }}>
          <div className="row" style={{ justifyContent: 'space-between' }}>
            <span className="muted">Articles</span>
            <span>{formatCfa(cart.totalAmount)}</span>
          </div>
          <div className="row" style={{ justifyContent: 'space-between' }}>
            <span className="muted">Transport</span>
            <span>{transportFee > 0 ? formatCfa(transportFee) : 'Inclus'}</span>
          </div>
          <div className="row" style={{ justifyContent: 'space-between', fontWeight: 800, fontSize: 19 }}>
            <span>Total indicatif</span>
            <span>{formatCfa(total)}</span>
          </div>
          <p className="muted" style={{ fontSize: 13 }}>
            Le prix final est figé seulement si le dépôt accepte ta commande.
          </p>
        </div>

        <button className="btn block" type="submit" disabled={busy}>
          {busy ? 'Envoi…' : 'Envoyer la commande au dépôt'}
        </button>
      </form>
    </div>
  );
}
