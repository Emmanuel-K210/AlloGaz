import type { ReactNode } from 'react';
import { Mascot } from './Mascot';
import type { StatusInfo } from '../utils/orderStatus';

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
