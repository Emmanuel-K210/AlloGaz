import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from './layout/AppShell';
import { RequireAuth } from './auth/RequireAuth';
import { LoginPage } from './pages/LoginPage';
import { SearchPage } from './pages/SearchPage';
import { SellerDetailPage } from './pages/SellerDetailPage';
import { CheckoutPage } from './pages/CheckoutPage';
import { OrdersPage } from './pages/OrdersPage';
import { OrderDetailPage } from './pages/OrderDetailPage';
import { SellerAreaPage } from './pages/seller/SellerAreaPage';
import { SellerOffersPage } from './pages/seller/SellerOffersPage';
import { SellerOrdersPage } from './pages/seller/SellerOrdersPage';
import { SellerOrderDetailPage } from './pages/seller/SellerOrderDetailPage';
import { AdminPage } from './pages/admin/AdminPage';

export function App() {
  return (
    <AppShell>
      <Routes>
        <Route path="/connexion" element={<LoginPage />} />

        <Route path="/" element={<RequireAuth><SearchPage /></RequireAuth>} />
        <Route path="/depots/:sellerId" element={<RequireAuth><SellerDetailPage /></RequireAuth>} />
        <Route path="/commande" element={<RequireAuth><CheckoutPage /></RequireAuth>} />
        <Route path="/commandes" element={<RequireAuth><OrdersPage /></RequireAuth>} />
        <Route path="/commandes/:orderId" element={<RequireAuth><OrderDetailPage /></RequireAuth>} />

        <Route path="/vendeur" element={<RequireAuth><SellerAreaPage /></RequireAuth>} />
        <Route
          path="/vendeur/offres"
          element={<RequireAuth role="SELLER"><SellerOffersPage /></RequireAuth>}
        />
        <Route
          path="/vendeur/commandes"
          element={<RequireAuth role="SELLER"><SellerOrdersPage /></RequireAuth>}
        />
        <Route
          path="/vendeur/commandes/:orderId"
          element={<RequireAuth role="SELLER"><SellerOrderDetailPage /></RequireAuth>}
        />

        <Route path="/admin/*" element={<RequireAuth role="ADMIN"><AdminPage /></RequireAuth>} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AppShell>
  );
}
