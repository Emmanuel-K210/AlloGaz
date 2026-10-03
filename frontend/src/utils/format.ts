export function formatCfa(amount: number): string {
  return `${amount.toLocaleString('fr-FR')} F CFA`;
}

export function formatDistance(meters: number): string {
  if (meters < 1000) return `${Math.round(meters)} m`;
  return `${(meters / 1000).toFixed(1)} km`;
}

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('fr-FR', {
    timeZone: 'Africa/Abidjan',
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function formatPhoneDisplay(phone: string): string {
  return phone;
}
