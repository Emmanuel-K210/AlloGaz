package ci.allogaz.payment.application;

import java.time.Clock;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ci.allogaz.payment.application.port.out.PaymentEventPublisher;
import ci.allogaz.payment.application.port.out.PaymentGateway;
import ci.allogaz.payment.application.port.out.PaymentGateway.GatewayStatus;
import ci.allogaz.payment.application.port.out.PaymentRepository;
import ci.allogaz.payment.domain.Payment;
import ci.allogaz.shared.domain.NotFoundException;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final int MAX_ATTEMPTS = 3;

    private final PaymentGateway gateway;
    private final PaymentRepository payments;
    private final EscrowService escrow;
    private final PaymentEventPublisher events;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public PaymentService(PaymentGateway gateway, PaymentRepository payments, EscrowService escrow,
            PaymentEventPublisher events, TransactionTemplate transactions, Clock clock) {
        this.gateway = gateway;
        this.payments = payments;
        this.escrow = escrow;
        this.events = events;
        this.transactions = transactions;
        this.clock = clock;
    }

    public record Started(UUID paymentId, String providerReference, String status, String checkoutUrl) {
    }

    /**
     * Lance le paiement auprès de l'agrégateur. Si celui-ci le confirme immédiatement, il est traité
     * sur-le-champ ; sinon la confirmation arrivera par notification (callback).
     */
    public Started start(UUID orderId, UUID payerId, String payerPhone, long amount) {
        Payment payment = transactions.execute(s -> payments.save(
                Payment.start(orderId, payerId, amount, gateway.providerName(), clock.instant())));
        PaymentGateway.Initiation initiation = gateway.initiate(new PaymentGateway.Request(payment.id(), amount,
                payerPhone, "AlloGaz - commande " + orderId));
        transactions.executeWithoutResult(s -> {
            Payment p = payments.findById(payment.id()).orElseThrow();
            p.attachReference(initiation.providerReference());
            payments.save(p);
        });
        apply(initiation.providerReference(), initiation.status());
        Payment current = payments.findById(payment.id()).orElseThrow();
        return new Started(current.id(), current.providerReference(), current.status().name(),
                initiation.checkoutUrl());
    }

    /** Notification de l'agrégateur : on revérifie le statut à la source. */
    public void handleCallback(String providerReference) {
        payments.findByProviderReference(providerReference).orElseThrow(
                () -> new NotFoundException("PAYMENT_NOT_FOUND", "Paiement inconnu."));
        apply(providerReference, gateway.fetchStatus(providerReference));
    }

    private void apply(String reference, GatewayStatus status) {
        switch (status) {
            case SUCCEEDED -> confirmWithRetry(reference);
            case FAILED -> transactions.executeWithoutResult(s -> payments.findByProviderReference(reference)
                    .ifPresent(p -> {
                        p.fail(clock.instant());
                        payments.save(p);
                    }));
            case PENDING -> { }
        }
    }

    /**
     * Confirmation dans une seule transaction (statut du paiement, séquestre, commande payée, stock).
     * Un conflit de verrouillage optimiste (stock modifié en parallèle) rejoue l'ensemble.
     */
    private void confirmWithRetry(String reference) {
        for (int attempt = 1; ; attempt++) {
            try {
                transactions.executeWithoutResult(s -> confirm(reference));
                return;
            } catch (OptimisticLockingFailureException e) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw e;
                }
                log.info("Conflit concurrent sur le paiement {}, nouvel essai ({}/{})", reference, attempt,
                        MAX_ATTEMPTS);
            }
        }
    }

    private void confirm(String reference) {
        Payment payment = payments.findByProviderReference(reference).orElseThrow();
        if (!payment.succeed(clock.instant())) {
            return;
        }
        payments.save(payment);
        escrow.hold(payment.orderId(), payment.amount());
        events.paymentSucceeded(new PaymentSucceededEvent(payment.id(), payment.orderId(), payment.amount()));
    }

    @Transactional(readOnly = true)
    public Payment get(UUID paymentId) {
        return payments.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("PAYMENT_NOT_FOUND", "Paiement introuvable."));
    }
}
