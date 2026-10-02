package ci.allogaz.payment.infrastructure.persistence;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import ci.allogaz.payment.application.port.out.LedgerRepository;
import ci.allogaz.payment.domain.EntryType;
import ci.allogaz.payment.domain.LedgerEntry;
import ci.allogaz.payment.domain.LedgerTransaction;
import ci.allogaz.shared.domain.ConflictException;

@Repository
class JdbcLedgerRepository implements LedgerRepository {

    private final JdbcClient jdbc;

    JdbcLedgerRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void append(LedgerTransaction transaction) {
        try {
            for (LedgerEntry e : transaction.entries()) {
                jdbc.sql("""
                        INSERT INTO ledger_entries (id, transaction_id, order_id, account, entry_type, amount, created_at)
                        VALUES (:id, :tx, :orderId, :account, :type, :amount, :createdAt)
                        """)
                        .param("id", e.id()).param("tx", e.transactionId()).param("orderId", e.orderId())
                        .param("account", e.account()).param("type", e.type().name()).param("amount", e.amount())
                        .param("createdAt", Timestamp.from(e.createdAt()))
                        .update();
            }
        } catch (DuplicateKeyException ex) {
            // Index uniques : un seul dépôt et une seule sortie de séquestre par commande.
            throw new ConflictException("LEDGER_DUPLICATE", "Ce mouvement a déjà été enregistré pour la commande.");
        }
    }

    @Override
    public long balance(String account) {
        return jdbc.sql("SELECT COALESCE(SUM(amount), 0) FROM ledger_entries WHERE account = :account")
                .param("account", account).query(Long.class).single();
    }

    @Override
    public long balance(String account, UUID orderId) {
        return jdbc.sql("SELECT COALESCE(SUM(amount), 0) FROM ledger_entries WHERE account = :account AND order_id = :orderId")
                .param("account", account).param("orderId", orderId).query(Long.class).single();
    }

    @Override
    public List<LedgerEntry> entriesForOrder(UUID orderId) {
        return jdbc.sql("SELECT * FROM ledger_entries WHERE order_id = :orderId ORDER BY created_at, entry_type, amount")
                .param("orderId", orderId)
                .query((rs, i) -> new LedgerEntry(rs.getObject("id", UUID.class),
                        rs.getObject("transaction_id", UUID.class), rs.getObject("order_id", UUID.class),
                        rs.getString("account"), EntryType.valueOf(rs.getString("entry_type")), rs.getLong("amount"),
                        rs.getTimestamp("created_at").toInstant()))
                .list();
    }
}
