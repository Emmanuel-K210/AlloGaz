import { useState } from 'react';
import type { FormEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Mascot } from '../components/Mascot';
import { Logo } from '../components/Logo';
import { ErrorBanner } from '../components/ui';
import { ApiRequestError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

type Step = 'phone' | 'code';

export function LoginPage() {
  const { requestOtp, verifyOtp } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? '/';

  const [step, setStep] = useState<Step>('phone');
  const [phone, setPhone] = useState('+225 ');
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [cooldown, setCooldown] = useState(0);

  async function handleRequestOtp(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const ttl = await requestOtp(phone.replace(/\s+/g, ''));
      setStep('code');
      setCooldown(ttl);
    } catch (err) {
      setError(describeError(err));
    } finally {
      setBusy(false);
    }
  }

  async function handleVerify(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await verifyOtp(phone.replace(/\s+/g, ''), code.trim());
      navigate(from, { replace: true });
    } catch (err) {
      setError(describeError(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="container" style={{ maxWidth: 420, paddingTop: 48 }}>
      <div className="stack" style={{ alignItems: 'center', textAlign: 'center', marginBottom: 24 }}>
        <Mascot pose={step === 'code' ? 'think' : 'wave'} size={120} />
        <Logo size={36} />
        <p className="muted">Le gaz, à un appel de chez toi.</p>
      </div>

      {error && <ErrorBanner message={error} />}

      {step === 'phone' ? (
        <form className="stack card" onSubmit={handleRequestOtp} style={{ marginTop: 16 }}>
          <div className="field">
            <label htmlFor="phone">Ton numéro de téléphone</label>
            <input
              id="phone"
              type="tel"
              inputMode="tel"
              autoComplete="tel"
              required
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="+225 07 00 00 00 00"
            />
          </div>
          <button className="btn block" type="submit" disabled={busy}>
            {busy ? 'Envoi…' : 'Recevoir le code par SMS'}
          </button>
        </form>
      ) : (
        <form className="stack card" onSubmit={handleVerify} style={{ marginTop: 16 }}>
          <p className="muted">
            Code envoyé au <b style={{ color: 'var(--ink)' }}>{phone}</b>
            {cooldown > 0 ? ` · valable ${Math.round(cooldown / 60)} min` : ''}
          </p>
          <div className="field">
            <label htmlFor="code">Code reçu par SMS</label>
            <input
              id="code"
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              required
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="0000"
              maxLength={8}
            />
          </div>
          <button className="btn block" type="submit" disabled={busy || code.length < 4}>
            {busy ? 'Vérification…' : 'Valider'}
          </button>
          <button
            type="button"
            className="btn ghost"
            onClick={() => {
              setStep('phone');
              setCode('');
              setError(null);
            }}
          >
            Changer de numéro
          </button>
        </form>
      )}
    </div>
  );
}

function describeError(err: unknown): string {
  if (err instanceof ApiRequestError) {
    if (err.code === 'OTP_INVALID') return 'Code incorrect ou expiré. Réessaie.';
    if (err.code === 'TOO_MANY_REQUESTS' || err.status === 429) return 'Trop de tentatives, réessaie dans un instant.';
    return err.detail ?? err.message;
  }
  return "Une erreur est survenue. Vérifie ta connexion et réessaie.";
}
