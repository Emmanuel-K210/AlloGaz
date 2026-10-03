import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { Spinner } from '../components/ui';
import type { Role } from '../api/types';

export function RequireAuth({ children, role }: { children: ReactNode; role?: Role }) {
  const { isAuthenticated, loading, hasRole } = useAuth();
  const location = useLocation();

  if (loading) return <Spinner label="On vérifie ta session…" />;
  if (!isAuthenticated) {
    return <Navigate to="/connexion" replace state={{ from: location.pathname }} />;
  }
  if (role && !hasRole(role)) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}
