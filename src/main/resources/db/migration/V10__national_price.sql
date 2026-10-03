-- Le prix de la bouteille (recharge et/ou achat) est national et réglementé par le syndicat des gaziers :
-- un dépôt ne peut pas le modifier. Il migre donc de seller_offers (par dépôt) vers products (référentiel,
-- modifiable uniquement par un administrateur).

ALTER TABLE products ADD COLUMN refill_price BIGINT CHECK (refill_price > 0);
ALTER TABLE products ADD COLUMN purchase_price BIGINT CHECK (purchase_price > 0);

-- Reprise des données existantes : le prix le plus fréquemment pratiqué par les dépôts devient le tarif
-- national de référence (en développement/démo, les dépôts avaient des prix légèrement différents ;
-- en production, un administrateur ajustera au tarif officiel juste après cette migration).
UPDATE products p SET
    refill_price = (SELECT MODE() WITHIN GROUP (ORDER BY o.refill_price)
                    FROM seller_offers o WHERE o.product_id = p.id AND o.refill_price IS NOT NULL),
    purchase_price = (SELECT MODE() WITHIN GROUP (ORDER BY o.purchase_price)
                       FROM seller_offers o WHERE o.product_id = p.id AND o.purchase_price IS NOT NULL);

-- Le CHECK multi-colonnes (au moins un prix) dépend des deux colonnes : il est automatiquement supprimé
-- par PostgreSQL dès qu'on retire l'une d'elles (DROP CONSTRAINT explicite en secours si le nom généré
-- diffère de la convention habituelle).
ALTER TABLE seller_offers DROP CONSTRAINT IF EXISTS seller_offers_check;
ALTER TABLE seller_offers DROP COLUMN refill_price;
ALTER TABLE seller_offers DROP COLUMN purchase_price;
