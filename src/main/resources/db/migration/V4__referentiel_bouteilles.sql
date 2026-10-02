-- Référentiel des bouteilles de gaz en Côte d'Ivoire : une société peut avoir plusieurs couleurs
-- en circulation (anciennes et nouvelles flottes). Les couleurs passent donc en liste (minuscules),
-- et les détails visuels (capsule, nuance) vont dans « appearance ».
ALTER TABLE products ADD COLUMN bottle_colors TEXT[] NOT NULL DEFAULT '{}';
ALTER TABLE products ADD COLUMN appearance VARCHAR(255);
ALTER TABLE products DROP COLUMN bottle_color;
CREATE INDEX idx_products_bottle_colors ON products USING GIN (bottle_colors);

UPDATE products SET bottle_colors = '{bleu}', appearance = 'Bouteille bleue, capsule ou bouchon orange'
WHERE brand = 'TotalEnergies';

UPDATE products SET bottle_colors = '{bleu}', appearance = 'Bouteille bleue (bleu distinctif ou marine selon les séries)',
                    name = replace(name, 'Petro Ivoire', 'Pétro Ivoire'), brand = 'Pétro Ivoire', company = 'Pétro Ivoire'
WHERE brand = 'Petro Ivoire';

UPDATE products SET bottle_colors = '{gris,bleu}', appearance = 'Grise ou bleue selon la flotte en circulation'
WHERE brand = 'Oryx';

INSERT INTO products (id, category_id, name, brand, company, bottle_colors, appearance, capacity_grams) VALUES
    ('00000000-0000-0000-0001-000000000007', '00000000-0000-0000-0000-000000000001', 'Bouteille Corlay 6 kg', 'Corlay', 'Corlay Côte d''Ivoire', '{vert}', 'Bouteille verte', 6000),
    ('00000000-0000-0000-0001-000000000008', '00000000-0000-0000-0000-000000000001', 'Bouteille Corlay 12,5 kg', 'Corlay', 'Corlay Côte d''Ivoire', '{vert}', 'Bouteille verte', 12500),
    ('00000000-0000-0000-0001-000000000009', '00000000-0000-0000-0000-000000000001', 'Bouteille Shell 6 kg', 'Shell', 'Vivo Energy Côte d''Ivoire', '{gris,bleu}', 'Grise ou bleue selon la flotte en circulation', 6000),
    ('00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0000-000000000001', 'Bouteille Shell 12,5 kg', 'Shell', 'Vivo Energy Côte d''Ivoire', '{gris,bleu}', 'Grise ou bleue selon la flotte en circulation', 12500),
    ('00000000-0000-0000-0001-000000000011', '00000000-0000-0000-0000-000000000001', 'Bouteille Petroci 6 kg', 'Petroci', 'Petroci', '{gris,bleu}', 'Grise ou bleue selon la flotte en circulation', 6000),
    ('00000000-0000-0000-0001-000000000012', '00000000-0000-0000-0000-000000000001', 'Bouteille Petroci 12,5 kg', 'Petroci', 'Petroci', '{gris,bleu}', 'Grise ou bleue selon la flotte en circulation', 12500);
