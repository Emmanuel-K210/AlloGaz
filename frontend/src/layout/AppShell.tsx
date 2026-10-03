import type { ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { Logo } from '../components/Logo';
import { useAuth } from '../auth/AuthContext';

export function AppShell({ children }: { children: ReactNode }) {
  const { user, hasRole, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="app-shell">
      <header className="topbar">
        <NavLink to="/" aria-label="Accueil AlloGaz">
          <Logo size={30} />
        </NavLink>
        {user && (
          <button
            className="btn ghost"
            onClick={async () => {
              await logout();
              navigate('/connexion');
            }}
          >
            Déconnexion
          </button>
        )}
      </header>

      <main className="content">{children}</main>

      {user && (
        <nav className="tabbar">
          <NavLink to="/" end className={({ isActive }) => (isActive ? 'active' : '')}>
            Rechercher
          </NavLink>
          <NavLink to="/commandes" className={({ isActive }) => (isActive ? 'active' : '')}>
            Mes commandes
          </NavLink>
          <NavLink to="/vendeur" className={({ isActive }) => (isActive ? 'active' : '')}>
            {hasRole('SELLER') ? 'Mon dépôt' : 'Devenir vendeur'}
          </NavLink>
          {hasRole('ADMIN') && (
            <NavLink to="/admin" className={({ isActive }) => (isActive ? 'active' : '')}>
              Admin
            </NavLink>
          )}
        </nav>
      )}
    </div>
  );
}
