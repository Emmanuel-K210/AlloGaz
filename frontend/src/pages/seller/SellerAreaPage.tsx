import { useAuth } from '../../auth/AuthContext';
import { SellerOnboardingPage } from './SellerOnboardingPage';
import { SellerDashboardPage } from './SellerDashboardPage';

/** Aiguillage : formulaire d'ouverture de dépôt si l'utilisateur n'est pas encore vendeur, sinon tableau de bord. */
export function SellerAreaPage() {
  const { hasRole } = useAuth();
  return hasRole('SELLER') ? <SellerDashboardPage /> : <SellerOnboardingPage />;
}
