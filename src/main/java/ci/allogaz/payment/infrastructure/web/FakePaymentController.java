package ci.allogaz.payment.infrastructure.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ci.allogaz.payment.application.PaymentService;
import ci.allogaz.payment.infrastructure.gateway.FakePaymentGateway;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Simulation de la confirmation Mobile Money ; n'existe qu'avec la passerelle factice. */
@RestController
@ConditionalOnBean(FakePaymentGateway.class)
@Tag(name = "Paiements")
public class FakePaymentController {

    private final FakePaymentGateway gateway;
    private final PaymentService payments;

    public FakePaymentController(FakePaymentGateway gateway, PaymentService payments) {
        this.gateway = gateway;
        this.payments = payments;
    }

    @PostMapping("/api/v1/payments/fake/{reference}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "[Développement] Simule la validation (ou le refus) du paiement sur le téléphone")
    public void complete(@PathVariable String reference, @RequestParam(defaultValue = "true") boolean success) {
        gateway.complete(reference, success);
        payments.handleCallback(reference);
    }
}
