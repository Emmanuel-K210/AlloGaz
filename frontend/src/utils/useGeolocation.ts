import { useCallback, useState } from 'react';

export interface Coords {
  lat: number;
  lon: number;
}

/** Centre d'Abidjan (Plateau), utilisé si la géolocalisation est refusée ou indisponible. */
export const ABIDJAN_FALLBACK: Coords = { lat: 5.3267, lon: -4.0273 };

export function useGeolocation(initial: Coords = ABIDJAN_FALLBACK) {
  const [coords, setCoords] = useState<Coords>(initial);
  const [locating, setLocating] = useState(false);
  const [denied, setDenied] = useState(false);

  const locate = useCallback(() => {
    if (!navigator.geolocation) {
      setDenied(true);
      return;
    }
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setCoords({ lat: pos.coords.latitude, lon: pos.coords.longitude });
        setLocating(false);
        setDenied(false);
      },
      () => {
        setLocating(false);
        setDenied(true);
      },
      { enableHighAccuracy: true, timeout: 8000 },
    );
  }, []);

  return { coords, setCoords, locate, locating, denied };
}
