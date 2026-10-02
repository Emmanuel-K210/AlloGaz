package ci.allogaz.payment.application;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ci.allogaz.payment.application.port.out.LedgerRepository;
import ci.allogaz.payment.domain.Commission;
import ci.allogaz.payment.domain.LedgerAccounts;
import ci.allogaz.payment.domain.LedgerEntry;
import ci.allogaz.payment.domain.LedgerTransaction;

/**
 * Séquestre : l'argent de l'acheteur reste bloqué jusqu'à la validation de la commande, puis il est
 * libéré au vendeur moins la commission, ou rendu à l'acheteur. En litige, rien ne bouge (fonds gelés)
 * tant qu'un administrateur n'a pas tranché.
 */
@Service
public class EscrowService {

    private final LedgerRepository ledger;
    private final Commission commission;
    private final Clock clock;

    public EscrowService(LedgerRepository ledger, Commission commission, Clock clock) {
        this.ledger = ledger;
        this.commission = commission;
        this.clock = clock;
    }

    @Transactional
    public void hold(UUID orderId, long amount) {
        ledger.append(LedgerTransaction.escrowDeposit(orderId, amount, clock.instant()));
    }

    /** @return la part versée au vendeur. */
    @Transactional
    public Commission.Split release(UUID orderId, UUID sellerId) {
        long escrowed = escrowed(orderId);
        LedgerTransaction tx = LedgerTransaction.release(orderId, sellerId, escrowed, commission, clock.instant());
        ledger.append(tx);
        return commission.split(escrowed);
    }

    @Transactional
    public long refund(UUID orderId) {
        long escrowed = escrowed(orderId);
        ledger.append(LedgerTransaction.refund(orderId, escrowed, clock.instant()));
        return escrowed;
    }

    @Transactional(readOnly = true)
    public long escrowed(UUID orderId) {
        return ledger.balance(LedgerAccounts.ESCROW, orderId);
    }

    @Transactional(readOnly = true)
    public long sellerBalance(UUID sellerId) {
        return ledger.balance(LedgerAccounts.seller(sellerId));
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> entries(UUID orderId) {
        return ledger.entriesForOrder(orderId);
    }
}
