import { Link, Route, Routes } from 'react-router-dom';
import { AdminSellersPage } from './AdminSellersPage';
import { AdminDisputesPage } from './AdminDisputesPage';

export function AdminPage() {
  return (
    <div className="container">
      <h1 style={{ fontSize: 26, marginBottom: 12 }}>Administration</h1>
      <nav className="row" style={{ marginBottom: 16 }}>
        <Link className="badge bleu" to="/admin/depots">Dépôts à valider</Link>
        <Link className="badge bleu" to="/admin/litiges">Litiges</Link>
      </nav>
      <Routes>
        <Route index element={<AdminSellersPage />} />
        <Route path="depots" element={<AdminSellersPage />} />
        <Route path="litiges" element={<AdminDisputesPage />} />
      </Routes>
    </div>
  );
}
