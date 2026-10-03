import { useEffect, useState } from 'react';
import { products as fetchProducts } from '../../api/catalog';
import * as sellerApi from '../../api/seller';
import type { Offer, ProductView } from '../../api/types';
import { ErrorBanner, Spinner } from '../../components/ui';

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
      <p className="muted" style={{ marginBottom: 16 }}>Fixe tes prix et ton stock pour chaque bouteille du référentiel.</p>
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
  const [refillPrice, setRefillPrice] = useState(offer?.refillPrice?.toString() ?? '');
  const [purchasePrice, setPurchasePrice] = useState(offer?.purchasePrice?.toString() ?? '');
  const [stock, setStock] = useState(offer?.stock ?? 0);
  const [active, setActive] = useState(offer?.active ?? true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function save() {
    setBusy(true);
    setError(null);
    try {
      const updated = await sellerApi.upsertOffer(product.id, {
        refillPrice: refillPrice ? Number(refillPrice) : undefined,
        purchasePrice: purchasePrice ? Number(purchasePrice) : undefined,
        stock,
        active,
      });
      onSaved(updated);
    } catch {
      setError('Prix invalide ou enregistrement impossible.');
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
      {error && <ErrorBanner message={error} />}
      <div className="row" style={{ flexWrap: 'wrap' }}>
        <div className="field" style={{ flex: 1, minWidth: 130 }}>
          <label>Prix recharge</label>
          <input type="number" min={0} value={refillPrice} onChange={(e) => setRefillPrice(e.target.value)} placeholder="F CFA" />
        </div>
        <div className="field" style={{ flex: 1, minWidth: 130 }}>
          <label>Prix neuve</label>
          <input type="number" min={0} value={purchasePrice} onChange={(e) => setPurchasePrice(e.target.value)} placeholder="F CFA" />
        </div>
        <div className="field" style={{ width: 90 }}>
          <label>Stock</label>
          <input type="number" min={0} value={stock} onChange={(e) => setStock(Number(e.target.value) || 0)} />
        </div>
      </div>
      <label className="row" style={{ gap: 6 }}>
        <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
        Offre active (visible des acheteurs)
      </label>
      <button className="btn secondary" disabled={busy || (!refillPrice && !purchasePrice)} onClick={save}>
        {busy ? 'Enregistrement…' : 'Enregistrer'}
      </button>
    </div>
  );
}
