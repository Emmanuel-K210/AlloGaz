import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { publicSeller } from '../api/catalog';
import type { Offer, PublicSeller } from '../api/types';
import { ColorChips, ErrorBanner, ExchangeBadge, Spinner } from '../components/ui';
import { formatCfa } from '../utils/format';
import { useCart } from '../cart/CartContext';
import { Mascot } from '../components/Mascot';

export function SellerDetailPage() {
  const { sellerId = '' } = useParams();
  const [seller, setSeller] = useState<PublicSeller | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const cart = useCart();
  const navigate = useNavigate();

  useEffect(() => {
    setSeller(null);
    setError(null);
    publicSeller(sellerId)
      .then(setSeller)
      .catch(() => setError('Ce dépôt est introuvable ou plus disponible.'));
  }, [sellerId]);

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), 2200);
    return () => clearTimeout(t);
  }, [toast]);

  if (error) return <div className="container"><ErrorBanner message={error} /></div>;
  if (!seller) return <Spinner label="Chargement du dépôt…" />;

  const { profile, openNow, offers } = seller;

  function handleAdd(offer: Offer, type: 'refillPrice' | 'purchasePrice', quantity: number) {
    const unitPrice = type === 'refillPrice' ? offer.refillPrice : offer.purchasePrice;
    if (!unitPrice) return;
    const outcome = cart.addLine(seller!.profile.id, seller!.profile.shopName, {
      offerId: offer.id,
      productId: offer.productId,
      productName: offer.productName,
      type: type === 'refillPrice' ? 'REFILL' : 'PURCHASE',
      quantity,
      unitPrice,
    });
    setToast(
      outcome === 'replaced-seller'
        ? 'Nouveau panier : les articles du dépôt précédent ont été remplacés.'
        : 'Ajouté au panier.',
    );
  }

  const cartHere = cart.sellerId === profile.id && cart.lines.length > 0;

  return (
    <div className="container">
      <div className="card" style={{ marginBottom: 16 }}>
        <div className="row" style={{ justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <h1 style={{ fontSize: 24 }}>{profile.shopName}</h1>
            <p className="muted" style={{ marginTop: 4 }}>{profile.address ?? 'Adresse non précisée'}</p>
          </div>
          <Mascot pose={openNow ? 'wave' : 'sleep'} size={64} />
        </div>
        <div className="row" style={{ marginTop: 10, flexWrap: 'wrap', gap: 6 }}>
          <span className={`badge ${openNow ? 'vert' : 'rouge'}`}>{openNow ? 'Ouvert' : 'Fermé'}</span>
          <span className="badge neutre">
            {profile.deliveryMode === 'PICKUP_ONLY'
              ? 'Retrait sur place uniquement'
              : profile.deliveryMode === 'INCLUDED'
                ? 'Livraison incluse'
                : `Livraison ${formatCfa(profile.deliveryFee)}`}
          </span>
          {profile.universalExchange && <ExchangeBadge />}
        </div>
        {profile.universalExchange && (
          <p className="muted" style={{ fontSize: 14, marginTop: 10 }}>
            Ce dépôt reprend ta bouteille vide même si ce n'est pas la même société que celle que tu choisis ici.
          </p>
        )}
      </div>

      {toast && <div className="badge bleu" style={{ marginBottom: 12, display: 'block', textAlign: 'center' }}>{toast}</div>}

      <h2 style={{ fontSize: 20, marginBottom: 10 }}>Bouteilles disponibles</h2>
      {offers.length === 0 && <p className="muted">Ce dépôt n'a pas encore d'offre active.</p>}
      <div className="stack">
        {offers.map((offer) => (
          <OfferRow key={offer.id} offer={offer} onAdd={handleAdd} />
        ))}
      </div>

      {cartHere && (
        <button
          className="btn block"
          style={{ position: 'sticky', bottom: 80, marginTop: 20 }}
          onClick={() => navigate('/commande')}
        >
          Voir le panier · {cart.totalItems} article{cart.totalItems > 1 ? 's' : ''} · {formatCfa(cart.totalAmount)}
        </button>
      )}

      <p style={{ marginTop: 16 }}>
        <Link to="/" className="muted">← Retour à la recherche</Link>
      </p>
    </div>
  );
}

function OfferRow({
  offer,
  onAdd,
}: {
  offer: Offer;
  onAdd: (offer: Offer, type: 'refillPrice' | 'purchasePrice', quantity: number) => void;
}) {
  const [qty, setQty] = useState(1);
  const outOfStock = offer.stock <= 0 || !offer.active;

  return (
    <div className="card">
      <div className="row" style={{ justifyContent: 'space-between' }}>
        <div>
          <strong>{offer.productName}</strong>
          {offer.company && <span className="muted" style={{ marginLeft: 6 }}>· {offer.company}</span>}
        </div>
        <span className={`badge ${outOfStock ? 'rouge' : 'neutre'}`}>
          {outOfStock ? 'Épuisé' : `${offer.stock} en stock`}
        </span>
      </div>
      {offer.bottleColors.length > 0 && (
        <div style={{ marginTop: 4 }}>
          <ColorChips colors={offer.bottleColors} />
        </div>
      )}

      <div className="row" style={{ marginTop: 10 }}>
        <label className="muted" htmlFor={`qty-${offer.id}`} style={{ fontSize: 14 }}>Qté</label>
        <input
          id={`qty-${offer.id}`}
          type="number"
          min={1}
          max={20}
          value={qty}
          onChange={(e) => setQty(Math.max(1, Math.min(20, Number(e.target.value) || 1)))}
          style={{ width: 70 }}
        />
      </div>

      <div className="row" style={{ marginTop: 10, flexWrap: 'wrap' }}>
        {offer.refillPrice != null && (
          <button className="btn secondary" disabled={outOfStock} onClick={() => onAdd(offer, 'refillPrice', qty)}>
            Recharge · {formatCfa(offer.refillPrice)}
          </button>
        )}
        {offer.purchasePrice != null && (
          <button className="btn secondary" disabled={outOfStock} onClick={() => onAdd(offer, 'purchasePrice', qty)}>
            Neuve · {formatCfa(offer.purchasePrice)}
          </button>
        )}
      </div>
    </div>
  );
}
