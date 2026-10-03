import type { ReactNode } from 'react';
import { Mascot } from './Mascot';
import type { StatusInfo } from '../utils/orderStatus';
import { bottleColorHex } from '../utils/bottleColors';

export function Spinner({ label = 'Chargement…' }: { label?: string }) {
  return (
    <div className="empty" role="status">
      <Mascot pose="think" size={72} />
      <p style={{ marginTop: 8 }}>{label}</p>
    </div>
  );
}

export function ErrorBanner({ message }: { message: string }) {
  return <div className="error-banner">{message}</div>;
}

export function EmptyState({ pose = 'sleep', children }: { pose?: 'sleep' | 'sorry' | 'think'; children: ReactNode }) {
  return (
    <div className="empty">
      <Mascot pose={pose} size={84} />
      <p style={{ marginTop: 8 }}>{children}</p>
    </div>
  );
}

export function StatusBadge({ info }: { info: StatusInfo }) {
  return <span className={`badge ${info.tone}`}>{info.label}</span>;
}

/** Pastille de couleur + nom : toujours les deux ensemble, pour rester lisible sans distinguer les couleurs. */
export function ColorChip({ name }: { name: string }) {
  return (
    <span className="color-chip">
      <span className="color-chip-dot" style={{ background: bottleColorHex(name) }} aria-hidden="true" />
      {name}
    </span>
  );
}

export function ColorChips({ colors }: { colors: string[] }) {
  if (colors.length === 0) return null;
  return (
    <span className="row" style={{ gap: 6, flexWrap: 'wrap' }}>
      {colors.map((c) => (
        <ColorChip key={c} name={c} />
      ))}
    </span>
  );
}

/** Badge pour un dépôt qui reprend une bouteille vide d'une autre société en échange. */
export function ExchangeBadge() {
  return <span className="badge bleu" title="Ce dépôt accepte ta bouteille vide même si ce n'est pas sa marque">🔄 Échange toutes marques</span>;
}
