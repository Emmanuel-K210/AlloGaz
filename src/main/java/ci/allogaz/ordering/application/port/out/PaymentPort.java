package ci.allogaz.ordering.application.port.out;

import java.util.UUID;

/** Lancement d'un paiement Mobile Money (module payment). */
public interface PaymentPort {

    PaymentStart start(UUID orderId, UUID payerId, String payerPhone, long amount);

    record PaymentStart(UUID paymentId, String providerReference, String status, String checkoutUrl) {
    }
}
