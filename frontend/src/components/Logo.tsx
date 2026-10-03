interface LogoMarkProps {
  bubble?: string;
  flame?: string;
  heart?: string;
  size?: number;
}

/** La bulle d'appel qui contient une flamme : « Allo » et « Gaz » en un seul signe. */
export function LogoMark({ bubble = '#1B7BFF', flame = '#FF7A1A', heart = '#FFE3C7', size = 32 }: LogoMarkProps) {
  return (
    <svg viewBox="0 0 100 100" width={size} height={size} aria-hidden="true">
      <path
        d="M20 12H80A14 14 0 0 1 94 26V62A14 14 0 0 1 80 76H50L28 94V76H20A14 14 0 0 1 6 62V26A14 14 0 0 1 20 12Z"
        fill={bubble}
      />
      <path
        d="M50 22C52 34 66 40 66 53A16 16 0 0 1 34 53C34 46 38 42 41 38C43 42 45 44 47 44C49 38 49 30 50 22Z"
        fill={flame}
      />
      <path
        d="M50 43C53 49 58 51 58 57A8 8 0 0 1 42 57C42 53 45 51 46 49C47 51 48 52 49 52C50 49 50 46 50 43Z"
        fill={heart}
      />
    </svg>
  );
}

export function Logo({ size = 32 }: { size?: number }) {
  return (
    <span className="lockup">
      <LogoMark size={size} />
      <span>
        Allo<i>Gaz</i>
      </span>
    </span>
  );
}
