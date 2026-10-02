package ci.allogaz.payment.infrastructure.gateway;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ci.allogaz.payment.application.port.out.PaymentGateway;

/**
 * Agrégateur factice, en attendant CinetPay ou PayDunya. Avec auto-succeed, le paiement est confirmé
 * immédiatement ; sinon il reste en attente jusqu'à l'appel de {@link #complete(String, boolean)}
 * (exposé par un endpoint de simulation).
 */
@Component
@ConditionalOnProperty(name = "allogaz.payment.provider", havingValue = "fake", matchIfMissing = true)
public class FakePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(FakePaymentGateway.class);

    private volatile boolean autoSucceed;
    private final Map<String, GatewayStatus> statuses = new ConcurrentHashMap<>();

    public FakePaymentGateway(@Value("${allogaz.payment.fake.auto-succeed:true}") boolean autoSucceed) {
        this.autoSucceed = autoSucceed;
    }

    @Override
    public String providerName() {
        return "fake";
    }

    @Override
    public Initiation initiate(Request request) {
        String reference = "FAKE-" + UUID.randomUUID();
        GatewayStatus status = autoSucceed ? GatewayStatus.SUCCEEDED : GatewayStatus.PENDING;
        statuses.put(reference, status);
        log.info("[PAIEMENT FACTICE] {} F CFA depuis {} -> {} ({})", request.amount(), request.payerPhone(),
                reference, status);
        return new Initiation(reference, status, null);
    }

    @Override
    public GatewayStatus fetchStatus(String providerReference) {
        return statuses.getOrDefault(providerReference, GatewayStatus.FAILED);
    }

    /** Bascule la confirmation immédiate (simulation et tests). */
    public void setAutoSucceed(boolean autoSucceed) {
        this.autoSucceed = autoSucceed;
    }

    /** Simule la réponse de l'opérateur Mobile Money (validation ou refus sur le téléphone). */
    public void complete(String providerReference, boolean success) {
        statuses.computeIfPresent(providerReference,
                (k, v) -> success ? GatewayStatus.SUCCEEDED : GatewayStatus.FAILED);
    }
}
