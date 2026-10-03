import { Link, Route, Routes } from 'react-router-dom';
import { AdminSellersPage } from './AdminSellersPage';
import { AdminDisputesPage } from './AdminDisputesPage';
import { AdminProductsPage } from './AdminProductsPage';

export function AdminPage() {
  return (
    <div className="container">
      <h1 style={{ fontSize: 26, marginBottom: 12 }}>Administration</h1>
      <nav className="row" style={{ marginBottom: 16, flexWrap: 'wrap' }}>
        <Link className="badge bleu" to="/admin/depots">Dépôts à valider</Link>
        <Link className="badge bleu" to="/admin/litiges">Litiges</Link>
        <Link className="badge bleu" to="/admin/produits">Tarifs nationaux</Link>
      </nav>
      <Routes>
        <Route index element={<AdminSellersPage />} />
        <Route path="depots" element={<AdminSellersPage />} />
        <Route path="litiges" element={<AdminDisputesPage />} />
        <Route path="produits" element={<AdminProductsPage />} />
      </Routes>
    </div>
  );
}
