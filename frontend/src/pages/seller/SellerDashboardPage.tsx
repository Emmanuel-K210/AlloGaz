import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import * as sellerApi from '../../api/seller';
import type { SellerBalance } from '../../api/seller';
import type { SellerProfile } from '../../api/types';
import { ErrorBanner, Spinner } from '../../components/ui';
import { Mascot } from '../../components/Mascot';
import { formatCfa } from '../../utils/format';

const STATUS_LABEL: Record<SellerProfile['status'], { label: string; tone: string }> = {
  PENDING: { label: 'En attente de validation', tone: 'neutre' },
  VERIFIED: { label: 'Validé', tone: 'vert' },
  SUSPENDED: { label: 'Suspendu', tone: 'rouge' },
};

export function SellerDashboardPage() {
  const [profile, setProfile] = useState<SellerProfile | null>(null);
  const [balance, setBalance] = useState<SellerBalance | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  function load() {
    sellerApi
      .myProfile()
      .then(setProfile)
      .catch(() => setError('Impossible de charger ton dépôt.'));
    sellerApi.balance().then(setBalance).catch(() => {});
  }

  useEffect(load, []);

  if (error) return <div className="container"><ErrorBanner message={error} /></div>;
  if (!profile) return <Spinner label="Chargement de ton dépôt…" />;

  const status = STATUS_LABEL[profile.status];

  async function toggleOpen() {
    setBusy(true);
    try {
      const updated = profile!.acceptingOrders ? await sellerApi.pauseShop() : await sellerApi.openShop();
      setProfile(updated);
    } catch {
      setError("Impossible de changer l'état du dépôt.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="container">
      <div className="card" style={{ textAlign: 'center', marginBottom: 16 }}>
        <Mascot pose={profile.acceptingOrders ? 'wave' : 'sleep'} size={100} />
        <h1 style={{ fontSize: 22, marginTop: 6 }}>{profile.shopName}</h1>
        <span className={`badge ${status.tone}`}>{status.label}</span>
      </div>

      {profile.status === 'VERIFIED' && (
        <button className="btn block" disabled={busy} onClick={toggleOpen} style={{ marginBottom: 16 }}>
          {profile.acceptingOrders ? 'Mettre en pause' : 'Ouvrir aux commandes'}
        </button>
      )}
      {profile.status === 'PENDING' && (
        <p className="muted" style={{ marginBottom: 16 }}>
          Ton dépôt est en attente de validation par l'équipe AlloGaz. Tu pourras l'ouvrir ensuite.
        </p>
      )}

      {balance && (
        <div className="card" style={{ marginBottom: 16 }}>
          <p className="muted">Solde disponible</p>
          <p style={{ fontSize: 28, fontWeight: 800 }}>{formatCfa(balance.amount)}</p>
        </div>
      )}

      <div className="stack">
        <Link className="btn secondary block" to="/vendeur/offres">Gérer mes offres</Link>
        <Link className="btn secondary block" to="/vendeur/commandes">Commandes reçues</Link>
      </div>
    </div>
  );
}
