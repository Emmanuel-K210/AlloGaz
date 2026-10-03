import { useEffect, useState } from 'react';
import { categories as fetchCategories, products as fetchProducts } from '../../api/catalog';
import * as adminApi from '../../api/admin';
import type { CategoryView, ProductView } from '../../api/types';
import { ColorChips, ErrorBanner, Spinner } from '../../components/ui';
import { formatCfa } from '../../utils/format';

export function AdminProductsPage() {
  const [categories, setCategories] = useState<CategoryView[] | null>(null);
  const [products, setProducts] = useState<ProductView[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  function load() {
    Promise.all([fetchCategories(), fetchProducts()])
      .then(([c, p]) => {
        setCategories(c);
        setProducts(p);
      })
      .catch(() => setError('Impossible de charger le référentiel.'));
  }

  useEffect(load, []);

  if (error) return <ErrorBanner message={error} />;
  if (!categories || !products) return <Spinner label="Chargement du référentiel…" />;

  return (
    <div className="stack">
      <p className="muted" style={{ fontSize: 14 }}>
        Le prix ici est national (recharge et/ou achat) : il s'applique chez tous les dépôts qui vendent ce
        produit. À mettre à jour quand le syndicat des gaziers annonce un nouveau tarif officiel.
      </p>

      <NewProductForm categories={categories} onCreated={load} />

      <div className="stack">
        {products.map((p) => (
          <ProductPriceEditor
            key={p.id}
            product={p}
            onSaved={(updated) => setProducts((prev) => (prev ?? []).map((x) => (x.id === updated.id ? updated : x)))}
          />
        ))}
      </div>
    </div>
  );
}

function ProductPriceEditor({ product, onSaved }: { product: ProductView; onSaved: (p: ProductView) => void }) {
  const [refillPrice, setRefillPrice] = useState(product.refillPrice?.toString() ?? '');
  const [purchasePrice, setPurchasePrice] = useState(product.purchasePrice?.toString() ?? '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function save() {
    setBusy(true);
    setError(null);
    try {
      const updated = await adminApi.updateProductPrice(
        product.id,
        refillPrice ? Number(refillPrice) : undefined,
        purchasePrice ? Number(purchasePrice) : undefined,
      );
      onSaved(updated);
    } catch {
      setError('Prix invalide (doit être positif) ou enregistrement impossible.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="card stack">
      <div className="row" style={{ justifyContent: 'space-between' }}>
        <strong>
          {product.name}
          {product.company && <span className="muted"> · {product.company}</span>}
        </strong>
        <span className="muted" style={{ fontSize: 13 }}>
          {product.refillPrice == null && product.purchasePrice == null
            ? 'Tarif non fixé'
            : `Actuel : ${product.refillPrice != null ? `recharge ${formatCfa(product.refillPrice)}` : ''}${
                product.refillPrice != null && product.purchasePrice != null ? ' · ' : ''
              }${product.purchasePrice != null ? `neuve ${formatCfa(product.purchasePrice)}` : ''}`}
        </span>
      </div>
      {product.bottleColors.length > 0 && <ColorChips colors={product.bottleColors} />}
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
      </div>
      <button className="btn secondary" disabled={busy} onClick={save}>
        {busy ? 'Enregistrement…' : 'Mettre à jour le tarif national'}
      </button>
    </div>
  );
}

function NewProductForm({ categories, onCreated }: { categories: CategoryView[]; onCreated: () => void }) {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [categorySlug, setCategorySlug] = useState(categories[0]?.slug ?? '');
  const [brand, setBrand] = useState('');
  const [company, setCompany] = useState('');
  const [colors, setColors] = useState('');
  const [capacityKg, setCapacityKg] = useState('12.5');
  const [refillPrice, setRefillPrice] = useState('');
  const [purchasePrice, setPurchasePrice] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!open) {
    return (
      <button className="btn secondary" onClick={() => setOpen(true)}>
        + Ajouter un produit au référentiel
      </button>
    );
  }

  async function create() {
    setBusy(true);
    setError(null);
    try {
      await adminApi.createProduct({
        categorySlug,
        name,
        brand: brand || undefined,
        company: company || undefined,
        bottleColors: colors ? colors.split(',').map((c) => c.trim()).filter(Boolean) : undefined,
        capacityGrams: capacityKg ? Math.round(Number(capacityKg) * 1000) : undefined,
        refillPrice: refillPrice ? Number(refillPrice) : undefined,
        purchasePrice: purchasePrice ? Number(purchasePrice) : undefined,
      });
      setOpen(false);
      setName('');
      setBrand('');
      setCompany('');
      setColors('');
      setRefillPrice('');
      setPurchasePrice('');
      onCreated();
    } catch {
      setError('Création impossible (vérifie les champs).');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="card stack">
      <strong>Nouveau produit</strong>
      {error && <ErrorBanner message={error} />}
      <div className="field">
        <label>Nom</label>
        <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Ex. Bouteille Shell 12,5 kg" />
      </div>
      <div className="row" style={{ flexWrap: 'wrap' }}>
        <div className="field" style={{ flex: 1, minWidth: 140 }}>
          <label>Catégorie</label>
          <select value={categorySlug} onChange={(e) => setCategorySlug(e.target.value)}>
            {categories.map((c) => (
              <option key={c.slug} value={c.slug}>{c.name}</option>
            ))}
          </select>
        </div>
        <div className="field" style={{ flex: 1, minWidth: 120 }}>
          <label>Marque</label>
          <input value={brand} onChange={(e) => setBrand(e.target.value)} placeholder="Ex. Shell" />
        </div>
        <div className="field" style={{ flex: 1, minWidth: 160 }}>
          <label>Société</label>
          <input value={company} onChange={(e) => setCompany(e.target.value)} placeholder="Ex. Vivo Energy Côte d'Ivoire" />
        </div>
      </div>
      <div className="row" style={{ flexWrap: 'wrap' }}>
        <div className="field" style={{ flex: 1, minWidth: 140 }}>
          <label>Couleurs (séparées par une virgule)</label>
          <input value={colors} onChange={(e) => setColors(e.target.value)} placeholder="Ex. Jaune, Rouge" />
        </div>
        <div className="field" style={{ width: 110 }}>
          <label>Contenance (kg)</label>
          <input type="number" step="0.5" min={0} value={capacityKg} onChange={(e) => setCapacityKg(e.target.value)} />
        </div>
      </div>
      <div className="row" style={{ flexWrap: 'wrap' }}>
        <div className="field" style={{ flex: 1, minWidth: 130 }}>
          <label>Prix recharge (optionnel)</label>
          <input type="number" min={0} value={refillPrice} onChange={(e) => setRefillPrice(e.target.value)} placeholder="F CFA" />
        </div>
        <div className="field" style={{ flex: 1, minWidth: 130 }}>
          <label>Prix neuve (optionnel)</label>
          <input type="number" min={0} value={purchasePrice} onChange={(e) => setPurchasePrice(e.target.value)} placeholder="F CFA" />
        </div>
      </div>
      <div className="row">
        <button className="btn secondary" disabled={busy || !name || !categorySlug} onClick={create}>
          {busy ? 'Création…' : 'Créer le produit'}
        </button>
        <button className="btn ghost" onClick={() => setOpen(false)}>Annuler</button>
      </div>
    </div>
  );
}
