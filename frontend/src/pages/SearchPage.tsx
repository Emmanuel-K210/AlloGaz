import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { searchSellers } from '../api/search';
import type { SaleType, SellerResult } from '../api/types';
import { EmptyState, ErrorBanner, ExchangeBadge, Spinner } from '../components/ui';
import { formatCfa, formatDistance } from '../utils/format';
import { useGeolocation } from '../utils/useGeolocation';
import { Mascot } from '../components/Mascot';

export function SearchPage() {
  const { coords, locate, locating, denied } = useGeolocation();
  const [type, setType] = useState<SaleType | ''>('');
  const [company, setCompany] = useState('');
  const [results, setResults] = useState<SellerResult[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    locate();
    // on ne relance pas automatiquement : l'utilisateur déclenche la localisation une fois
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    searchSellers({
      lat: coords.lat,
      lon: coords.lon,
      type: type || undefined,
      company: company || undefined,
      limit: 30,
    })
      .then((r) => {
        if (!cancelled) setResults(r);
      })
      .catch(() => {
        if (!cancelled) setError("Impossible de charger les dépôts pour l'instant.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [coords, type, company]);

  return (
    <div className="container">
      <h1 style={{ fontSize: 28, marginBottom: 4 }}>Dépôts près de toi</h1>
      <p className="muted" style={{ marginBottom: 16 }}>
        {denied
          ? "Position approximative (Abidjan). Autorise la localisation pour un classement plus précis."
          : locating
            ? 'Localisation en cours…'
            : 'Classés par distance, disponibilité et note.'}
      </p>
      <p className="muted" style={{ fontSize: 14, marginBottom: 16 }}>
        💡 Un dépôt reprend d'habitude une bouteille vide de sa propre société (Oryx, Shell, Petroci…). Repère le
        badge <ExchangeBadge /> pour les dépôts qui acceptent n'importe quelle bouteille en échange.
      </p>

      <div className="row" style={{ marginBottom: 16, flexWrap: 'wrap' }}>
        <button className="btn secondary" type="button" onClick={locate} disabled={locating}>
          📍 Me localiser
        </button>
        <select value={type} onChange={(e) => setType(e.target.value as SaleType | '')} aria-label="Type d'achat">
          <option value="">Achat ou recharge</option>
          <option value="REFILL">Recharge</option>
          <option value="PURCHASE">Bouteille neuve</option>
        </select>
        <input
          type="search"
          placeholder="Société (ex. Oryx, Total…)"
          value={company}
          onChange={(e) => setCompany(e.target.value)}
          style={{ flex: 1, minWidth: 160 }}
        />
      </div>

      {error && <ErrorBanner message={error} />}

      {loading && !results && <Spinner label="Recherche des dépôts…" />}

      {results && results.length === 0 && (
        <EmptyState pose="sorry">Aucun dépôt ouvert ne correspond à ta recherche pour l'instant.</EmptyState>
      )}

      <div className="stack" style={{ marginTop: 8 }}>
        {results?.map((seller) => (
          <Link key={seller.sellerId} to={`/depots/${seller.sellerId}`} className="card seller-card">
            <div className="score">
              <Mascot pose={seller.available ? 'wave' : 'sleep'} size={56} />
            </div>
            <div style={{ flex: 1 }}>
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <strong style={{ fontSize: 19 }}>{seller.shopName}</strong>
                <span className="badge bleu">{formatDistance(seller.distanceMeters)}</span>
              </div>
              <p className="muted" style={{ fontSize: 14, marginTop: 2 }}>
                {seller.address ?? 'Adresse non précisée'}
              </p>
              <div className="row" style={{ marginTop: 8, flexWrap: 'wrap', gap: 6 }}>
                {seller.deliversToYou ? (
                  <span className="badge vert">Livraison {seller.deliveryFee > 0 ? formatCfa(seller.deliveryFee) : 'incluse'}</span>
                ) : (
                  <span className="badge neutre">Retrait sur place</span>
                )}
                {!seller.available && <span className="badge rouge">Stock épuisé</span>}
                {seller.ratingCount > 0 && <span className="badge neutre">★ {seller.rating.toFixed(1)}</span>}
                {seller.universalExchange && <ExchangeBadge />}
              </div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
