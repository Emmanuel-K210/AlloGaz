CREATE TABLE payments (
    id                 UUID PRIMARY KEY,
    order_id           UUID        NOT NULL,
    payer_id           UUID        NOT NULL REFERENCES users (id),
    amount             BIGINT      NOT NULL CHECK (amount > 0),
    provider           VARCHAR(30) NOT NULL,
    provider_reference VARCHAR(100) UNIQUE,
    status             VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED')),
    created_at         TIMESTAMPTZ NOT NULL,
    completed_at       TIMESTAMPTZ,
    version            BIGINT      NOT NULL DEFAULT 0
);
CREATE INDEX idx_payments_order ON payments (order_id);
-- Au plus un paiement réussi par commande.
CREATE UNIQUE INDEX uq_payments_order_succeeded ON payments (order_id) WHERE status = 'SUCCEEDED';

-- Grand livre en partie double, montants signés en F CFA ; chaque transaction a une somme nulle.
CREATE TABLE ledger_entries (
    id             UUID PRIMARY KEY,
    transaction_id UUID         NOT NULL,
    order_id       UUID         NOT NULL,
    account        VARCHAR(80)  NOT NULL,
    entry_type     VARCHAR(20)  NOT NULL CHECK (entry_type IN ('ESCROW_DEPOSIT', 'ESCROW_RELEASE', 'COMMISSION', 'REFUND')),
    amount         BIGINT       NOT NULL CHECK (amount <> 0),
    created_at     TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_ledger_order ON ledger_entries (order_id);
CREATE INDEX idx_ledger_account ON ledger_entries (account);
CREATE INDEX idx_ledger_transaction ON ledger_entries (transaction_id);
-- Idempotence : un seul dépôt en séquestre et une seule sortie (libération ou remboursement) par commande.
CREATE UNIQUE INDEX uq_ledger_escrow_in ON ledger_entries (order_id)
    WHERE account = 'ESCROW' AND entry_type = 'ESCROW_DEPOSIT';
CREATE UNIQUE INDEX uq_ledger_escrow_out ON ledger_entries (order_id)
    WHERE account = 'ESCROW' AND entry_type IN ('ESCROW_RELEASE', 'REFUND');

-- Le grand livre est en ajout seul.
CREATE FUNCTION ledger_entries_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'ledger_entries est en ajout seul';
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_ledger_entries_append_only BEFORE UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION ledger_entries_append_only();
