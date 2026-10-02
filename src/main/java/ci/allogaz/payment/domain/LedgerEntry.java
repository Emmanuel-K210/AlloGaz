package ci.allogaz.payment.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Ligne du grand livre (en partie double) : un montant signé en F CFA sur un compte.
 * Les lignes d'une même transaction s'équilibrent (somme nulle). Elles ne sont jamais modifiées.
 */
public record LedgerEntry(UUID id, UUID transactionId, UUID orderId, String account, EntryType type, long amount,
                          Instant createdAt) {
}
