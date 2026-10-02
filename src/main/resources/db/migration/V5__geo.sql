-- Réputation agrégée des dépôts, alimentée par le module commande (acceptations, notes).
CREATE TABLE seller_stats (
    seller_id       UUID PRIMARY KEY REFERENCES seller_profiles (id) ON DELETE CASCADE,
    rating_sum      BIGINT NOT NULL DEFAULT 0,
    rating_count    BIGINT NOT NULL DEFAULT 0,
    orders_decided  BIGINT NOT NULL DEFAULT 0,
    orders_accepted BIGINT NOT NULL DEFAULT 0
);
