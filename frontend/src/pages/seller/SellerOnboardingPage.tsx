import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { applyAsSeller } from '../../api/seller';
import type { DeliveryMode } from '../../api/types';
import { ApiRequestError } from '../../api/client';
import { ErrorBanner } from '../../components/ui';
import { Mascot } from '../../components/Mascot';
import { useGeolocation } from '../../utils/useGeolocation';
import { useAuth } from '../../auth/AuthContext';

export function SellerOnboardingPage() {
  const navigate = useNavigate();
  const { applyRoleLocally, refreshUser } = useAuth();
  const { coords, locate, locating } = useGeolocation();
  const [shopName, setShopName] = useState('');
  const [address, setAddress] = useState('');
  const [deliveryMode, setDeliveryMode] = useState<DeliveryMode>('FIXED_FEE');
  const [deliveryFee, setDeliveryFee] = useState(500);
  const [deliveryRadiusMeters, setDeliveryRadiusMeters] = useState(5000);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await applyAsSeller({
        shopName,
        address: address || undefined,
        latitude: coords.lat,
        longitude: coords.lon,
        deliveryRadiusMeters,
        deliveryMode,
        deliveryFee: deliveryMode === 'FIXED_FEE' ? deliveryFee : 0,
        openingHours: [],
      });
      applyRoleLocally('SELLER');
      await refreshUser().catch(() => {});
      navigate('/vendeur', { replace: true });
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.detail ?? err.message : "L'ouverture du dépôt a échoué.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="container" style={{ maxWidth: 480 }}>
      <div className="stack" style={{ alignItems: 'center', textAlign: 'center', marginBottom: 20 }}>
        <Mascot pose="wave" size={100} />
        <h1 style={{ fontSize: 24 }}>Ouvrir mon dépôt sur AlloGaz</h1>
        <p className="muted">Une fois validé par l'équipe, ton dépôt apparaîtra dans les recherches.</p>
      </div>

      {error && <ErrorBanner message={error} />}

      <form className="stack card" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="shopName">Nom du dépôt</label>
          <input id="shopName" required value={shopName} onChange={(e) => setShopName(e.target.value)} placeholder="Ex. Dépôt Gaz Riviera" />
        </div>
        <div className="field">
          <label htmlFor="address">Adresse</label>
          <input id="address" value={address} onChange={(e) => setAddress(e.target.value)} placeholder="Quartier, rue…" />
        </div>
        <div className="field">
          <label>Position GPS</label>
          <div className="row">
            <span className="muted">{coords.lat.toFixed(4)}, {coords.lon.toFixed(4)}</span>
            <button type="button" className="btn ghost" onClick={locate} disabled={locating}>
              📍 Utiliser ma position
            </button>
          </div>
        </div>
        <div className="field">
          <label htmlFor="deliveryMode">Transport</label>
          <select id="deliveryMode" value={deliveryMode} onChange={(e) => setDeliveryMode(e.target.value as DeliveryMode)}>
            <option value="INCLUDED">Livraison incluse (gratuite)</option>
            <option value="FIXED_FEE">Livraison à prix fixe</option>
            <option value="PICKUP_ONLY">Retrait sur place uniquement</option>
          </select>
        </div>
        {deliveryMode === 'FIXED_FEE' && (
          <div className="field">
            <label htmlFor="deliveryFee">Frais de livraison (F CFA)</label>
            <input
              id="deliveryFee"
              type="number"
              min={0}
              value={deliveryFee}
              onChange={(e) => setDeliveryFee(Number(e.target.value) || 0)}
            />
          </div>
        )}
        {deliveryMode !== 'PICKUP_ONLY' && (
          <div className="field">
            <label htmlFor="radius">Rayon de livraison (mètres)</label>
            <input
              id="radius"
              type="number"
              min={0}
              max={50000}
              value={deliveryRadiusMeters}
              onChange={(e) => setDeliveryRadiusMeters(Number(e.target.value) || 0)}
            />
          </div>
        )}
        <button className="btn block" type="submit" disabled={busy}>
          {busy ? 'Envoi…' : 'Ouvrir mon dépôt'}
        </button>
      </form>
    </div>
  );
}
