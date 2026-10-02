package ci.allogaz.payment.infrastructure.events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import ci.allogaz.payment.application.PaymentSucceededEvent;
import ci.allogaz.payment.application.port.out.PaymentEventPublisher;

/** Événement synchrone : les écouteurs s'exécutent dans la transaction de confirmation. */
@Component
class SpringPaymentEventPublisher implements PaymentEventPublisher {

    private final ApplicationEventPublisher publisher;

    SpringPaymentEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void paymentSucceeded(PaymentSucceededEvent event) {
        publisher.publishEvent(event);
    }
}
