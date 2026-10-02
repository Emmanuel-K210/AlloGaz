package ci.allogaz.payment.domain;

import java.time.Instant;
import java.util.UUID;

import ci.allogaz.shared.domain.DomainException;

/** Tentative de paiement Mobile Money d'une commande auprès de l'agrégateur. */
public class Payment {

    private final UUID id;
    private final UUID orderId;
    private final UUID payerId;
    private final long amount;
    private final String provider;
    private String providerReference;
    private PaymentStatus status;
    private final Instant createdAt;
    private Instant completedAt;

    public Payment(UUID id, UUID orderId, UUID payerId, long amount, String provider, String providerReference,
            PaymentStatus status, Instant createdAt, Instant completedAt) {
        this.id = id;
        this.orderId = orderId;
        this.payerId = payerId;
        this.amount = amount;
        this.provider = provider;
        this.providerReference = providerReference;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public static Payment start(UUID orderId, UUID payerId, long amount, String provider, Instant now) {
        if (amount <= 0) {
            throw new DomainException("INVALID_AMOUNT", "Montant à payer invalide.");
        }
        return new Payment(UUID.randomUUID(), orderId, payerId, amount, provider, null, PaymentStatus.PENDING, now,
                null);
    }

    public void attachReference(String reference) {
        this.providerReference = reference;
    }

    /** @return vrai si le paiement vient de passer à réussi (faux s'il l'était déjà : idempotence). */
    public boolean succeed(Instant now) {
        if (status == PaymentStatus.SUCCEEDED) {
            return false;
        }
        if (status == PaymentStatus.FAILED) {
            throw new DomainException("PAYMENT_ALREADY_FAILED", "Ce paiement a déjà échoué.");
        }
        status = PaymentStatus.SUCCEEDED;
        completedAt = now;
        return true;
    }

    public void fail(Instant now) {
        if (status == PaymentStatus.PENDING) {
            status = PaymentStatus.FAILED;
            completedAt = now;
        }
    }

    public UUID id() {
        return id;
    }

    public UUID orderId() {
        return orderId;
    }

    public UUID payerId() {
        return payerId;
    }

    public long amount() {
        return amount;
    }

    public String provider() {
        return provider;
    }

    public String providerReference() {
        return providerReference;
    }

    public PaymentStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant completedAt() {
        return completedAt;
    }
}
