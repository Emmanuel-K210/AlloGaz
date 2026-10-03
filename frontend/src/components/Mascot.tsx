import { useEffect, useRef, useState } from 'react';
import './Mascot.css';

export type MascotPose = 'idle' | 'wave' | 'think' | 'sorry' | 'sleep' | 'party';

interface MascotProps {
  pose?: MascotPose;
  size?: number;
  clickable?: boolean;
  label?: string;
}

/**
 * La mascotte AlloGaz : une bouteille de gaz souriante avec une flamme sur la tête.
 * Ported depuis la charte graphique (identité visuelle) : poses = contente/réfléchit/désolée/dort/fête.
 */
export function Mascot({ pose = 'idle', size = 96, clickable = false, label }: MascotProps) {
  const [jumping, setJumping] = useState(false);
  const timeoutRef = useRef<number | undefined>(undefined);

  useEffect(() => () => window.clearTimeout(timeoutRef.current), []);

  const hop = () => {
    if (!clickable) return;
    setJumping(false);
    window.clearTimeout(timeoutRef.current);
    requestAnimationFrame(() => {
      setJumping(true);
      timeoutRef.current = window.setTimeout(() => setJumping(false), 700);
    });
  };

  const classes = ['mascot-stage', pose, jumping ? 'jump' : ''].filter(Boolean).join(' ');
  const width = size;
  const height = (size * 270) / 200;

  return (
    <div
      className={classes}
      style={{ width, height, cursor: clickable ? 'pointer' : undefined }}
      role={clickable ? 'button' : 'img'}
      aria-label={label ?? 'Mascotte AlloGaz'}
      tabIndex={clickable ? 0 : undefined}
      onClick={hop}
      onKeyDown={(e) => {
        if (clickable && (e.key === 'Enter' || e.key === ' ')) {
          e.preventDefault();
          hop();
        }
      }}
    >
      <svg className="mascot" viewBox="0 0 200 270" aria-hidden="true" width={width} height={height}>
        <ellipse cx="100" cy="258" rx="58" ry="9" fill="#000" opacity=".16" />
        <g className="m-hop">
          <g className="m-bob">
            <g className="m-arm m-arm-l">
              <rect x="14" y="160" width="40" height="14" rx="7" fill="#1B7BFF" />
              <circle cx="17" cy="167" r="9" fill="#1B7BFF" />
            </g>
            <g className="m-arm m-arm-r">
              <rect x="146" y="160" width="40" height="14" rx="7" fill="#1B7BFF" />
              <circle cx="183" cy="167" r="9" fill="#1B7BFF" />
            </g>
            <path
              d="M72 86V64Q72 42 100 42Q128 42 128 64V86"
              fill="none"
              stroke="#FF7A1A"
              strokeWidth="9"
              strokeLinecap="round"
            />
            <g transform="translate(100 44)">
              <g className="m-flame">
                <path d="M0 0C-17-2-19-19-6-30C-6-19 2-21 2-34C15-23 19-4 0 0Z" fill="#4FA8FF" />
                <path d="M0 0C-8-1-9-9-3-15C-3-9 1-10 1-17C8-11 9-2 0 0Z" fill="#E3F3FF" />
              </g>
            </g>
            <rect x="86" y="62" width="28" height="26" rx="7" fill="#C95A00" />
            <rect x="72" y="82" width="56" height="18" rx="9" fill="#FF7A1A" />
            <rect x="48" y="94" width="104" height="150" rx="44" fill="#1B7BFF" />
            <rect x="60" y="110" width="10" height="92" rx="5" fill="#fff" opacity=".24" />
            <g className="m-eyes">
              <circle cx="78" cy="148" r="12" fill="#fff" />
              <circle cx="122" cy="148" r="12" fill="#fff" />
              <g className="m-pupils">
                <circle cx="80" cy="150" r="6.5" fill="#0D2B45" />
                <circle cx="124" cy="150" r="6.5" fill="#0D2B45" />
                <circle cx="82" cy="147" r="2.2" fill="#fff" />
                <circle cx="126" cy="147" r="2.2" fill="#fff" />
              </g>
            </g>
            <g className="m-brows" fill="none" stroke="#0D2B45" strokeWidth="4" strokeLinecap="round">
              <path d="M66 136L90 126" />
              <path d="M110 126L134 136" />
            </g>
            <circle cx="66" cy="174" r="8" fill="#FF7A1A" opacity=".5" />
            <circle cx="134" cy="174" r="8" fill="#FF7A1A" opacity=".5" />
            <path
              className="m-smile"
              d="M87 172Q100 188 113 172"
              fill="none"
              stroke="#0D2B45"
              strokeWidth="5"
              strokeLinecap="round"
            />
            <path
              className="m-frown"
              d="M88 184Q100 170 112 184"
              fill="none"
              stroke="#0D2B45"
              strokeWidth="5"
              strokeLinecap="round"
            />
            <ellipse className="m-o" cx="100" cy="178" rx="7" ry="9" fill="#0D2B45" />
            <g className="m-open">
              <path d="M83 168Q100 206 117 168Z" fill="#0D2B45" />
              <path d="M91 189Q100 182 109 189Q100 198 91 189Z" fill="#FF7A1A" />
            </g>
          </g>
        </g>
      </svg>
      {pose === 'sleep' && <span className="mascot-zzz">z z</span>}
    </div>
  );
}
