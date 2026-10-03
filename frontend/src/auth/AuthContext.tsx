import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import * as authApi from '../api/auth';
import { onSessionExpired } from '../api/client';
import { clearSession, loadSession, saveSession } from '../api/tokenStore';
import type { Role, User } from '../api/types';

interface AuthContextValue {
  user: User | null;
  loading: boolean;
  isAuthenticated: boolean;
  hasRole: (role: Role) => boolean;
  requestOtp: (phone: string) => Promise<number>;
  verifyOtp: (phone: string, code: string) => Promise<User>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
  applyRoleLocally: (role: Role) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => loadSession()?.user ?? null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    onSessionExpired(() => setUser(null));
  }, []);

  useEffect(() => {
    const session = loadSession();
    if (!session) {
      setLoading(false);
      return;
    }
    authApi
      .me()
      .then((fresh) => setUser(fresh))
      .catch(() => {
        clearSession();
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, []);

  const requestOtp = useCallback(async (phone: string) => {
    const r = await authApi.requestOtp(phone);
    return r.expiresInSeconds;
  }, []);

  const verifyOtp = useCallback(async (phone: string, code: string) => {
    const tokens = await authApi.verifyOtp(phone, code);
    saveSession(tokens);
    setUser(tokens.user);
    return tokens.user;
  }, []);

  const logout = useCallback(async () => {
    const session = loadSession();
    clearSession();
    setUser(null);
    if (session) {
      try {
        await authApi.logout(session.refreshToken);
      } catch {
        // la session locale est déjà effacée
      }
    }
  }, []);

  const refreshUser = useCallback(async () => {
    const fresh = await authApi.me();
    setUser(fresh);
    const session = loadSession();
    if (session) saveSession({ ...session, user: fresh, tokenType: 'Bearer', newUser: false });
  }, []);

  /** Met à jour le rôle affiché localement en attendant le prochain /me (ex. après ouverture d'un dépôt). */
  const applyRoleLocally = useCallback((role: Role) => {
    setUser((prev) => (prev && !prev.roles.includes(role) ? { ...prev, roles: [...prev.roles, role] } : prev));
  }, []);

  const hasRole = useCallback((role: Role) => user?.roles.includes(role) ?? false, [user]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      loading,
      isAuthenticated: !!user,
      hasRole,
      requestOtp,
      verifyOtp,
      logout,
      refreshUser,
      applyRoleLocally,
    }),
    [user, loading, hasRole, requestOtp, verifyOtp, logout, refreshUser, applyRoleLocally],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth doit être utilisé dans <AuthProvider>');
  return ctx;
}
