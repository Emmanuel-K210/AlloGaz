package ci.allogaz.payment.application.port.out;

import java.util.List;
import java.util.UUID;

import ci.allogaz.payment.domain.LedgerEntry;
import ci.allogaz.payment.domain.LedgerTransaction;

/** Grand livre en ajout seul. */
public interface LedgerRepository {

    void append(LedgerTransaction transaction);

    long balance(String account);

    long balance(String account, UUID orderId);

    List<LedgerEntry> entriesForOrder(UUID orderId);
}
