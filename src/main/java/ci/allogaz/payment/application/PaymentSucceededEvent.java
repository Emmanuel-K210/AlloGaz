package ci.allogaz.payment.application;

import java.util.UUID;

/** Le paiement d'une commande est confirmé et les fonds sont en séquestre. */
public record PaymentSucceededEvent(UUID paymentId, UUID orderId, long amount) {
}
