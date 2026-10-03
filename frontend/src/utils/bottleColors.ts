/**
 * Associe un nom de couleur de bouteille (texte libre saisi par le vendeur, ex. "Bleue", "Grise métallisée")
 * à une couleur CSS, pour l'afficher comme pastille. Les mamans et les clients peu alphabétisés reconnaissent
 * souvent une bouteille à sa couleur avant sa marque : la pastille vient toujours avec le texte, jamais seule.
 */
const KNOWN: { match: string; hex: string; textOnLight?: boolean }[] = [
  { match: 'bleu', hex: '#1B7BFF' },
  { match: 'rouge', hex: '#D6342C' },
  { match: 'orange', hex: '#FF7A1A' },
  { match: 'jaune', hex: '#F2C94C', textOnLight: true },
  { match: 'vert', hex: '#12A150' },
  { match: 'gris', hex: '#9CA3AF' },
  { match: 'noir', hex: '#111827' },
  { match: 'blanc', hex: '#FFFFFF', textOnLight: true },
  { match: 'marron', hex: '#7C4A1E' },
  { match: 'brun', hex: '#7C4A1E' },
  { match: 'violet', hex: '#7C3AED' },
  { match: 'rose', hex: '#EC4899' },
  { match: 'beige', hex: '#D9C9A8', textOnLight: true },
];

function normalize(name: string): string {
  return name
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '');
}

/** Hash stable pour donner une teinte distincte (mais cohérente) aux couleurs non reconnues. */
function fallbackHex(name: string): string {
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = (hash << 5) - hash + name.charCodeAt(i);
  const hue = Math.abs(hash) % 360;
  return `hsl(${hue}, 55%, 55%)`;
}

export function bottleColorHex(name: string): string {
  const normalized = normalize(name);
  const found = KNOWN.find((k) => normalized.includes(k.match));
  return found?.hex ?? fallbackHex(name);
}
