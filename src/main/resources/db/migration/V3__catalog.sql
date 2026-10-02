CREATE TABLE categories (
    id   UUID PRIMARY KEY,
    slug VARCHAR(60)  NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL
);

CREATE TABLE products (
    id             UUID PRIMARY KEY,
    category_id    UUID         NOT NULL REFERENCES categories (id),
    name           VARCHAR(150) NOT NULL,
    brand          VARCHAR(80),
    -- Société de provenance (marketeur qui met la bouteille en circulation) et couleur de la bouteille :
    -- c'est ainsi que l'acheteur reconnaît sa bouteille, et une recharge suppose un échange à l'identique.
    company        VARCHAR(120),
    bottle_color   VARCHAR(40),
    capacity_grams INTEGER CHECK (capacity_grams > 0),
    active         BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_company ON products (lower(company));

CREATE TABLE seller_profiles (
    id                UUID PRIMARY KEY,
    user_id           UUID                  NOT NULL UNIQUE REFERENCES users (id),
    shop_name         VARCHAR(120)          NOT NULL,
    address           VARCHAR(255),
    location          geometry(Point, 4326) NOT NULL,
    delivery_radius_m INTEGER               NOT NULL CHECK (delivery_radius_m BETWEEN 0 AND 50000),
    accepting_orders  BOOLEAN               NOT NULL DEFAULT FALSE,
    status            VARCHAR(20)           NOT NULL CHECK (status IN ('PENDING', 'VERIFIED', 'SUSPENDED')),
    delivery_mode     VARCHAR(20)           NOT NULL CHECK (delivery_mode IN ('INCLUDED', 'FIXED_FEE', 'PICKUP_ONLY')),
    delivery_fee      BIGINT                NOT NULL DEFAULT 0 CHECK (delivery_fee >= 0),
    created_at        TIMESTAMPTZ           NOT NULL,
    version           BIGINT                NOT NULL DEFAULT 0
);
-- Les recherches de proximité se font en géographie (mètres) : index sur la conversion.
CREATE INDEX idx_seller_profiles_location_geog ON seller_profiles USING GIST ((location::geography));
CREATE INDEX idx_seller_profiles_status ON seller_profiles (status) WHERE accepting_orders;

CREATE TABLE seller_opening_hours (
    seller_id   UUID     NOT NULL REFERENCES seller_profiles (id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    opens_at    TIME     NOT NULL,
    closes_at   TIME     NOT NULL CHECK (closes_at > opens_at)
);
CREATE INDEX idx_seller_opening_hours_seller ON seller_opening_hours (seller_id);

-- Prix en F CFA (entiers, pas de décimales).
CREATE TABLE seller_offers (
    id             UUID PRIMARY KEY,
    seller_id      UUID    NOT NULL REFERENCES seller_profiles (id) ON DELETE CASCADE,
    product_id     UUID    NOT NULL REFERENCES products (id),
    refill_price   BIGINT CHECK (refill_price > 0),
    purchase_price BIGINT CHECK (purchase_price > 0),
    stock          INTEGER NOT NULL CHECK (stock >= 0),
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    version        BIGINT  NOT NULL DEFAULT 0,
    UNIQUE (seller_id, product_id),
    CHECK (refill_price IS NOT NULL OR purchase_price IS NOT NULL)
);
CREATE INDEX idx_seller_offers_product ON seller_offers (product_id) WHERE active;

-- Référentiel : la catégorie gaz et ses bouteilles courantes en Côte d'Ivoire.
INSERT INTO categories (id, slug, name) VALUES
    ('00000000-0000-0000-0000-000000000001', 'gaz-butane', 'Gaz butane');

-- ATTENTION : couleurs indicatives, à valider avec le terrain avant la mise en production.
INSERT INTO products (id, category_id, name, brand, company, bottle_color, capacity_grams) VALUES
    ('00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0000-000000000001', 'Bouteille TotalEnergies 6 kg', 'TotalEnergies', 'TotalEnergies Marketing Côte d''Ivoire', 'bleu', 6000),
    ('00000000-0000-0000-0001-000000000002', '00000000-0000-0000-0000-000000000001', 'Bouteille TotalEnergies 12,5 kg', 'TotalEnergies', 'TotalEnergies Marketing Côte d''Ivoire', 'bleu', 12500),
    ('00000000-0000-0000-0001-000000000003', '00000000-0000-0000-0000-000000000001', 'Bouteille Oryx 6 kg', 'Oryx', 'Oryx Energies Côte d''Ivoire', 'orange', 6000),
    ('00000000-0000-0000-0001-000000000004', '00000000-0000-0000-0000-000000000001', 'Bouteille Oryx 12,5 kg', 'Oryx', 'Oryx Energies Côte d''Ivoire', 'orange', 12500),
    ('00000000-0000-0000-0001-000000000005', '00000000-0000-0000-0000-000000000001', 'Bouteille Petro Ivoire 6 kg', 'Petro Ivoire', 'Petro Ivoire', 'vert', 6000),
    ('00000000-0000-0000-0001-000000000006', '00000000-0000-0000-0000-000000000001', 'Bouteille Petro Ivoire 12,5 kg', 'Petro Ivoire', 'Petro Ivoire', 'vert', 12500);
