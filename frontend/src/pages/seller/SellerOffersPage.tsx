import { useEffect, useState } from 'react';
import { products as fetchProducts } from '../../api/catalog';
import * as sellerApi from '../../api/seller';
import type { Offer, ProductView } from '../../api/types';
import { ColorChips, ErrorBanner, Spinner } from '../../components/ui';
import { formatCfa } from '../../utils/format';

export function SellerOffersPage() {
  const [products, setProducts] = useState<ProductView[] | null>(null);
  const [offers, setOffers] = useState<Offer[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  function load() {
    Promise.all([fetchProducts(), sellerApi.myOffers()])
      .then(([p, o]) => {
        setProducts(p);
        setOffers(o);
      })
      .catch(() => setError('Impossible de charger le catalogue.'));
  }

  useEffect(load, []);

  if (error) return <div className="container"><ErrorBanner message={error} /></div>;
  if (!products || !offers) return <Spinner label="Chargement du catalogue…" />;

  return (
    <div className="container">
      <h1 style={{ fontSize: 24, marginBottom: 4 }}>Mes offres</h1>
      <p className="muted" style={{ marginBottom: 16 }}>
        Le prix est national (fixé par l'administration) : tu gères seulement ton stock et si tu affiches
        l'offre aux acheteurs.
      </p>
      <div className="stack">
        {products.map((product) => (
          <OfferEditor
            key={product.id}
            product={product}
            offer={offers.find((o) => o.productId === product.id) ?? null}
            onSaved={(updated) => setOffers((prev) => [...(prev ?? []).filter((o) => o.productId !== product.id), updated])}
          />
        ))}
      </div>
    </div>
  );
}

function OfferEditor({
  product,
  offer,
  onSaved,
}: {
  product: ProductView;
  offer: Offer | null;
  onSaved: (offer: Offer) => void;
}) {
  const [stock, setStock] = useState(offer?.stock ?? 0);
  const [active, setActive] = useState(offer?.active ?? true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const noPrice = product.refillPrice == null && product.purchasePrice == null;

  async function save() {
    setBusy(true);
    setError(null);
    try {
      const updated = await sellerApi.upsertOffer(product.id, { stock, active });
      onSaved(updated);
    } catch {
      setError('Enregistrement impossible.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="card stack">
      <strong>
        {product.name}
        {product.company && <span className="muted"> · {product.company}</span>}
      </strong>
      {product.bottleColors.length > 0 && <ColorChips colors={product.bottleColors} />}
      {error && <ErrorBanner message={error} />}
      <div className="row" style={{ flexWrap: 'wrap', gap: 16 }}>
        <div>
          <span className="muted" style={{ fontSize: 14, display: 'block' }}>Prix recharge (national)</span>
          <strong>{product.refillPrice != null ? formatCfa(product.refillPrice) : '—'}</strong>
        </div>
        <div>
          <span className="muted" style={{ fontSize: 14, display: 'block' }}>Prix neuve (national)</span>
          <strong>{product.purchasePrice != null ? formatCfa(product.purchasePrice) : '—'}</strong>
        </div>
      </div>
      {noPrice && (
        <p className="muted" style={{ fontSize: 13 }}>
          Tarif pas encore fixé par l'administration : cette offre ne sera pas achetable tant qu'il ne l'est pas.
        </p>
      )}
      <div className="field" style={{ width: 90 }}>
        <label>Stock</label>
        <input type="number" min={0} value={stock} onChange={(e) => setStock(Number(e.target.value) || 0)} />
      </div>
      <label className="row" style={{ gap: 6 }}>
        <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
        Offre active (visible des acheteurs)
      </label>
      <button className="btn secondary" disabled={busy} onClick={save}>
        {busy ? 'Enregistrement…' : 'Enregistrer'}
      </button>
    </div>
  );
}
