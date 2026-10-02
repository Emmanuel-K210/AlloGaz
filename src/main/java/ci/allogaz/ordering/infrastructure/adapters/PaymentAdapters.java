package ci.allogaz.ordering.infrastructure.adapters;

import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import ci.allogaz.ordering.application.OrderService;
import ci.allogaz.ordering.application.port.out.EscrowPort;
import ci.allogaz.ordering.application.port.out.PaymentPort;
import ci.allogaz.payment.application.EscrowService;
import ci.allogaz.payment.application.PaymentService;
import ci.allogaz.payment.application.PaymentSucceededEvent;

/** Liaison avec le module payment : séquestre, lancement du paiement, réception de la confirmation. */
@Component
class PaymentAdapters implements EscrowPort, PaymentPort {

    private final EscrowService escrow;
    private final PaymentService payments;

    PaymentAdapters(EscrowService escrow, PaymentService payments) {
        this.escrow = escrow;
        this.payments = payments;
    }

    @Override
    public long releaseToSeller(UUID orderId, UUID sellerId) {
        return escrow.release(orderId, sellerId).sellerAmount();
    }

    @Override
    public long refundBuyer(UUID orderId) {
        return escrow.refund(orderId);
    }

    @Override
    public PaymentStart start(UUID orderId, UUID payerId, String payerPhone, long amount) {
        PaymentService.Started s = payments.start(orderId, payerId, payerPhone, amount);
        return new PaymentStart(s.paymentId(), s.providerReference(), s.status(), s.checkoutUrl());
    }

    @Component
    static class PaymentSucceededListener {

        private final OrderService orders;

        PaymentSucceededListener(OrderService orders) {
            this.orders = orders;
        }

        /** Synchrone, dans la transaction de confirmation du paiement. */
        @EventListener
        void on(PaymentSucceededEvent event) {
            orders.onPaymentConfirmed(event.orderId());
        }
    }
}
