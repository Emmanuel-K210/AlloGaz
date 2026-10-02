-- =====================================================================================
-- Données de démonstration (Abidjan). Chargées uniquement si FLYWAY_LOCATIONS inclut
-- classpath:db/demo (c'est le cas dans docker-compose.yml). Idempotent.
-- Prix indicatifs en F CFA, à titre de démonstration.
-- Connexion : demander un OTP pour l'un des numéros ci-dessous, le code apparaît dans les logs.
-- =====================================================================================

-- Utilisateurs ------------------------------------------------------------------------
INSERT INTO users (id, phone, display_name, created_at) VALUES
    ('d0000000-0000-0000-0000-000000000001', '+2250700000001', 'Admin AlloGaz',        now()),
    ('d0000000-0000-0000-0000-000000000011', '+2250700000011', 'Yao (dépôt Cocody)',   now()),
    ('d0000000-0000-0000-0000-000000000012', '+2250500000012', 'Bamba (dépôt Yopougon)', now()),
    ('d0000000-0000-0000-0000-000000000013', '+2250100000013', 'Adjoua (dépôt Marcory)', now()),
    ('d0000000-0000-0000-0000-000000000014', '+2250700000014', 'Konan (dépôt Anoumabo)', now()),
    ('d0000000-0000-0000-0000-000000000021', '+2250700000021', 'Awa (Cocody)',         now()),
    ('d0000000-0000-0000-0000-000000000022', '+2250500000022', 'Koffi (Yopougon)',     now()),
    ('d0000000-0000-0000-0000-000000000023', '+2250100000023', 'Fatou (Marcory)',      now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_roles (user_id, role) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'BUYER'), ('d0000000-0000-0000-0000-000000000001', 'ADMIN'),
    ('d0000000-0000-0000-0000-000000000011', 'BUYER'), ('d0000000-0000-0000-0000-000000000011', 'SELLER'),
    ('d0000000-0000-0000-0000-000000000012', 'BUYER'), ('d0000000-0000-0000-0000-000000000012', 'SELLER'),
    ('d0000000-0000-0000-0000-000000000013', 'BUYER'), ('d0000000-0000-0000-0000-000000000013', 'SELLER'),
    ('d0000000-0000-0000-0000-000000000014', 'BUYER'), ('d0000000-0000-0000-0000-000000000014', 'SELLER'),
    ('d0000000-0000-0000-0000-000000000021', 'BUYER'),
    ('d0000000-0000-0000-0000-000000000022', 'BUYER'),
    ('d0000000-0000-0000-0000-000000000023', 'BUYER')
ON CONFLICT DO NOTHING;

-- Dépôts ------------------------------------------------------------------------------
INSERT INTO seller_profiles (id, user_id, shop_name, address, location, delivery_radius_m, accepting_orders,
                             status, delivery_mode, delivery_fee, created_at) VALUES
    ('5e000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000011', 'Dépôt Gaz Riviera',
     'Riviera 2, près du carrefour Anono, Cocody', ST_SetSRID(ST_MakePoint(-3.9640, 5.3590), 4326),
     4000, TRUE, 'VERIFIED', 'FIXED_FEE', 500, now()),
    ('5e000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000012', 'Yop Gaz Service',
     'Siporex, Yopougon', ST_SetSRID(ST_MakePoint(-4.0890, 5.3364), 4326),
     3000, TRUE, 'VERIFIED', 'INCLUDED', 0, now()),
    ('5e000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000013', 'Marcory Gaz Express',
     'Zone 4, rue du Docteur Blanchard, Marcory', ST_SetSRID(ST_MakePoint(-3.9820, 5.2990), 4326),
     5000, TRUE, 'VERIFIED', 'FIXED_FEE', 700, now()),
    -- En attente de vérification : pour tester la validation par l'administrateur.
    ('5e000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000014', 'Dépôt Anoumabo',
     'Anoumabo, Marcory', ST_SetSRID(ST_MakePoint(-3.9750, 5.2960), 4326),
     0, FALSE, 'PENDING', 'PICKUP_ONLY', 0, now())
ON CONFLICT (id) DO NOTHING;

-- Horaires (1 = lundi). Yop Gaz Service n'a pas d'horaires : ouvert en continu, pratique pour une démo.
INSERT INTO seller_opening_hours (seller_id, day_of_week, opens_at, closes_at)
SELECT '5e000000-0000-0000-0000-000000000001', d, '07:00', '20:00' FROM generate_series(1, 6) d
WHERE NOT EXISTS (SELECT 1 FROM seller_opening_hours WHERE seller_id = '5e000000-0000-0000-0000-000000000001');
INSERT INTO seller_opening_hours (seller_id, day_of_week, opens_at, closes_at)
SELECT '5e000000-0000-0000-0000-000000000001', 7, '08:00', '13:00'
WHERE NOT EXISTS (SELECT 1 FROM seller_opening_hours WHERE seller_id = '5e000000-0000-0000-0000-000000000001' AND day_of_week = 7);
INSERT INTO seller_opening_hours (seller_id, day_of_week, opens_at, closes_at)
SELECT '5e000000-0000-0000-0000-000000000003', d, '06:30', '21:00' FROM generate_series(1, 7) d
WHERE NOT EXISTS (SELECT 1 FROM seller_opening_hours WHERE seller_id = '5e000000-0000-0000-0000-000000000003');

-- Offres (refill = recharge, purchase = bouteille neuve consigne comprise) ----------------
INSERT INTO seller_offers (id, seller_id, product_id, refill_price, purchase_price, stock) VALUES
    -- Cocody : TotalEnergies et Pétro Ivoire
    ('0f000000-0000-0000-0000-000000000101', '5e000000-0000-0000-0000-000000000001', '00000000-0000-0000-0001-000000000001', 2000, 15000, 12),
    ('0f000000-0000-0000-0000-000000000102', '5e000000-0000-0000-0000-000000000001', '00000000-0000-0000-0001-000000000002', 5200, 27000, 20),
    ('0f000000-0000-0000-0000-000000000103', '5e000000-0000-0000-0000-000000000001', '00000000-0000-0000-0001-000000000006', 5200, NULL, 8),
    -- Yopougon : Oryx, Corlay, Petroci (recharge uniquement sur Petroci)
    ('0f000000-0000-0000-0000-000000000201', '5e000000-0000-0000-0000-000000000002', '00000000-0000-0000-0001-000000000003', 2000, 14500, 15),
    ('0f000000-0000-0000-0000-000000000202', '5e000000-0000-0000-0000-000000000002', '00000000-0000-0000-0001-000000000004', 5200, 26500, 25),
    ('0f000000-0000-0000-0000-000000000203', '5e000000-0000-0000-0000-000000000002', '00000000-0000-0000-0001-000000000008', 5250, 27000, 10),
    ('0f000000-0000-0000-0000-000000000204', '5e000000-0000-0000-0000-000000000002', '00000000-0000-0000-0001-000000000012', 5200, NULL, 0),
    -- Marcory : TotalEnergies, Shell, Corlay
    ('0f000000-0000-0000-0000-000000000301', '5e000000-0000-0000-0000-000000000003', '00000000-0000-0000-0001-000000000002', 5300, 27500, 6),
    ('0f000000-0000-0000-0000-000000000302', '5e000000-0000-0000-0000-000000000003', '00000000-0000-0000-0001-000000000009', 2100, 15000, 10),
    ('0f000000-0000-0000-0000-000000000303', '5e000000-0000-0000-0000-000000000003', '00000000-0000-0000-0001-000000000010', 5300, 27000, 14),
    ('0f000000-0000-0000-0000-000000000304', '5e000000-0000-0000-0000-000000000003', '00000000-0000-0000-0001-000000000007', 2050, NULL, 9)
ON CONFLICT (id) DO NOTHING;

-- Réputation de départ (pour illustrer le classement) ---------------------------------
INSERT INTO seller_stats (seller_id, rating_sum, rating_count, orders_decided, orders_accepted) VALUES
    ('5e000000-0000-0000-0000-000000000001', 92, 20, 25, 23),
    ('5e000000-0000-0000-0000-000000000002', 61, 15, 20, 15),
    ('5e000000-0000-0000-0000-000000000003', 45, 10, 12, 12)
ON CONFLICT (seller_id) DO NOTHING;
