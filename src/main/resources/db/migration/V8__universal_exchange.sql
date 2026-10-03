-- Point d'échange toutes marques : le dépôt reprend une bouteille vide d'une autre société en échange
-- (ex. Oryx contre Shell ou Pétro Ivoire), au lieu de ne recharger que les bouteilles de sa propre société.
ALTER TABLE seller_profiles ADD COLUMN universal_exchange BOOLEAN NOT NULL DEFAULT FALSE;
