package ci.allogaz.payment.application.port.out;

import java.util.UUID;

/**
 * Agrégateur Mobile Money (CinetPay ou PayDunya à brancher). Le statut d'un paiement est toujours
 * revérifié auprès de l'agrégateur : on ne fait pas confiance au contenu d'une notification entrante.
 */
public interface PaymentGateway {

    String providerName();

    Initiation initiate(Request request);

    GatewayStatus fetchStatus(String providerReference);

    record Request(UUID paymentId, long amount, String payerPhone, String description) {
    }

    /** checkoutUrl : page de paiement éventuelle vers laquelle rediriger l'acheteur. */
    record Initiation(String providerReference, GatewayStatus status, String checkoutUrl) {
    }

    enum GatewayStatus {
        PENDING,
        SUCCEEDED,
        FAILED
    }
}
