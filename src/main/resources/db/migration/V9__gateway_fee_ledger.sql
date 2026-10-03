-- Nouveau type d'écriture du grand livre : frais de l'agrégateur Mobile Money (encaissement et
-- reversement), prélevés sur la commission de la plateforme pour que sa marge nette réelle reste visible.
ALTER TABLE ledger_entries DROP CONSTRAINT ledger_entries_entry_type_check;
ALTER TABLE ledger_entries ADD CONSTRAINT ledger_entries_entry_type_check
    CHECK (entry_type IN ('ESCROW_DEPOSIT', 'ESCROW_RELEASE', 'COMMISSION', 'GATEWAY_FEE', 'REFUND'));
