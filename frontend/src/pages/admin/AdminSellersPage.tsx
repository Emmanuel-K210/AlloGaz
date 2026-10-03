import { useEffect, useState } from 'react';
import * as adminApi from '../../api/admin';
import type { SellerProfile } from '../../api/types';
import { EmptyState, ErrorBanner, Spinner } from '../../components/ui';

export function AdminSellersPage() {
  const [sellers, setSellers] = useState<SellerProfile[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

  function load() {
    adminApi
      .pendingSellers('PENDING')
      .then(setSellers)
      .catch(() => setError('Impossible de charger les dépôts en attente.'));
  }

  useEffect(load, []);

  async function verify(id: string) {
    setBusy(id);
    try {
      await adminApi.verifySeller(id);
      load();
    } catch {
      setError('La validation a échoué.');
    } finally {
      setBusy(null);
    }
  }

  if (error) return <ErrorBanner message={error} />;
  if (!sellers) return <Spinner label="Chargement…" />;
  if (sellers.length === 0) return <EmptyState pose="sleep">Aucun dépôt en attente de validation.</EmptyState>;

  return (
    <div className="stack">
      {sellers.map((seller) => (
        <div key={seller.id} className="card row" style={{ justifyContent: 'space-between' }}>
          <div>
            <strong>{seller.shopName}</strong>
            <p className="muted" style={{ fontSize: 14 }}>{seller.address ?? 'Adresse non précisée'}</p>
          </div>
          <button className="btn secondary" disabled={busy === seller.id} onClick={() => verify(seller.id)}>
            Valider
          </button>
        </div>
      ))}
    </div>
  );
}
