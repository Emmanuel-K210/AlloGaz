package ci.allogaz.payment.application.port.out;

import ci.allogaz.payment.application.PaymentSucceededEvent;

/** Prévient les autres modules, dans la même transaction, qu'un paiement a abouti. */
public interface PaymentEventPublisher {

    void paymentSucceeded(PaymentSucceededEvent event);
}
