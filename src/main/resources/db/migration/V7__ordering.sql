-- Montants en F CFA (entiers).
CREATE TABLE orders (
    id                     UUID PRIMARY KEY,
    buyer_id               UUID        NOT NULL REFERENCES users (id),
    seller_id              UUID        NOT NULL REFERENCES seller_profiles (id),
    seller_user_id         UUID        NOT NULL REFERENCES users (id),
    status                 VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'INTENT_SENT', 'ACCEPTED', 'REJECTED',
                               'EXPIRED', 'PAID', 'IN_PREPARATION', 'OUT_FOR_DELIVERY', 'DELIVERED', 'VALIDATED',
                               'FUNDS_RELEASED', 'CANCELLED', 'DISPUTED')),
    fulfillment            VARCHAR(10) NOT NULL CHECK (fulfillment IN ('DELIVERY', 'PICKUP')),
    delivery_address       VARCHAR(255),
    delivery_location      geometry(Point, 4326),
    items_total            BIGINT      NOT NULL CHECK (items_total >= 0),
    transport_fee          BIGINT      NOT NULL CHECK (transport_fee >= 0),
    total                  BIGINT      NOT NULL CHECK (total >= 0),
    prices_frozen          BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMPTZ NOT NULL,
    submitted_at           TIMESTAMPTZ,
    response_deadline      TIMESTAMPTZ,
    accepted_at            TIMESTAMPTZ,
    paid_at                TIMESTAMPTZ,
    delivered_at           TIMESTAMPTZ,
    auto_validate_at       TIMESTAMPTZ,
    validated_at           TIMESTAMPTZ,
    closed_at              TIMESTAMPTZ,
    delivery_code_hash     VARCHAR(64),
    delivery_code_attempts INTEGER     NOT NULL DEFAULT 0,
    status_reason          VARCHAR(500),
    dispute_outcome        VARCHAR(20) CHECK (dispute_outcome IN ('RELEASE_TO_SELLER', 'REFUND_BUYER')),
    buyer_rating           INTEGER CHECK (buyer_rating BETWEEN 1 AND 5),
    version                BIGINT      NOT NULL DEFAULT 0
);
CREATE INDEX idx_orders_buyer ON orders (buyer_id, created_at DESC);
CREATE INDEX idx_orders_seller_user ON orders (seller_user_id, created_at DESC);
-- Index partiels des tâches planifiées.
CREATE INDEX idx_orders_intent_deadline ON orders (response_deadline) WHERE status = 'INTENT_SENT';
CREATE INDEX idx_orders_auto_validation ON orders (auto_validate_at) WHERE status = 'DELIVERED';
CREATE INDEX idx_orders_disputed ON orders (created_at) WHERE status = 'DISPUTED';

CREATE TABLE order_lines (
    order_id     UUID         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    line_no      INTEGER      NOT NULL,
    offer_id     UUID         NOT NULL REFERENCES seller_offers (id),
    product_id   UUID         NOT NULL REFERENCES products (id),
    product_name VARCHAR(150) NOT NULL,
    sale_type    VARCHAR(10)  NOT NULL CHECK (sale_type IN ('PURCHASE', 'REFILL')),
    quantity     INTEGER      NOT NULL CHECK (quantity > 0),
    unit_price   BIGINT       NOT NULL CHECK (unit_price > 0),
    PRIMARY KEY (order_id, line_no)
);

CREATE TABLE order_status_history (
    id          BIGSERIAL PRIMARY KEY,
    order_id    UUID        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    from_status VARCHAR(20) NOT NULL,
    to_status   VARCHAR(20) NOT NULL,
    changed_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_order_status_history_order ON order_status_history (order_id, id);
