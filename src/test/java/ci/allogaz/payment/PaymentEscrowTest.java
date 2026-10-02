package ci.allogaz.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import ci.allogaz.AbstractIntegrationTest;
import ci.allogaz.identity.domain.User;
import ci.allogaz.payment.application.EscrowService;
import ci.allogaz.payment.application.PaymentService;
import ci.allogaz.payment.application.PaymentSucceededEvent;
import ci.allogaz.payment.domain.LedgerAccounts;
import ci.allogaz.payment.infrastructure.gateway.FakePaymentGateway;
import ci.allogaz.shared.domain.ConflictException;

@RecordApplicationEvents
class PaymentEscrowTest extends AbstractIntegrationTest {

    @Autowired PaymentService payments;
    @Autowired EscrowService escrow;
    @Autowired FakePaymentGateway gateway;
    @Autowired JdbcClient jdbc;
    @Autowired ApplicationEvents events;

    @AfterEach
    void restoreGateway() {
        gateway.setAutoSucceed(true);
    }

    @Test
    void immediate_payment_goes_to_escrow_then_released_minus_commission() {
        User buyer = fixtures.user();
        UUID orderId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();

        PaymentService.Started started = payments.start(orderId, buyer.id(), buyer.phone().value(), 10_500);

        assertThat(started.status()).isEqualTo("SUCCEEDED");
        assertThat(escrow.escrowed(orderId)).isEqualTo(10_500);
        assertThat(events.stream(PaymentSucceededEvent.class)).singleElement()
                .extracting(PaymentSucceededEvent::orderId).isEqualTo(orderId);

        // Notification en double : idempotente
        payments.handleCallback(started.providerReference());
        assertThat(escrow.escrowed(orderId)).isEqualTo(10_500);
        assertThat(events.stream(PaymentSucceededEvent.class)).hasSize(1);

        escrow.release(orderId, sellerId);
        assertThat(escrow.escrowed(orderId)).isZero();
        assertThat(escrow.sellerBalance(sellerId)).isEqualTo(9_975);
        assertThat(escrow.entries(orderId)).hasSize(5)
                .anySatisfy(e -> assertThat(e.account()).isEqualTo(LedgerAccounts.PLATFORM_COMMISSION));

        // Une seule sortie de séquestre par commande
        assertThatThrownBy(() -> escrow.refund(orderId)).hasMessageContaining("Aucun montant");
    }

    @Test
    void pending_payment_is_confirmed_by_callback_after_checking_the_provider() throws Exception {
        gateway.setAutoSucceed(false);
        User buyer = fixtures.user();
        UUID orderId = UUID.randomUUID();

        PaymentService.Started started = payments.start(orderId, buyer.id(), buyer.phone().value(), 5_700);
        assertThat(started.status()).isEqualTo("PENDING");

        // Notification reçue alors que l'opérateur n'a pas confirmé : rien ne bouge
        mvc.perform(post("/api/v1/payments/callback/fake").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reference\":\"" + started.providerReference() + "\"}")).andExpect(status().isNoContent());
        assertThat(escrow.escrowed(orderId)).isZero();

        gateway.complete(started.providerReference(), true);
        mvc.perform(post("/api/v1/payments/callback/fake").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reference\":\"" + started.providerReference() + "\"}")).andExpect(status().isNoContent());
        assertThat(escrow.escrowed(orderId)).isEqualTo(5_700);
        assertThat(payments.get(started.paymentId()).status().name()).isEqualTo("SUCCEEDED");
    }

    @Test
    void refused_payment_does_not_touch_escrow() {
        gateway.setAutoSucceed(false);
        User buyer = fixtures.user();
        UUID orderId = UUID.randomUUID();
        PaymentService.Started started = payments.start(orderId, buyer.id(), buyer.phone().value(), 5_700);

        gateway.complete(started.providerReference(), false);
        payments.handleCallback(started.providerReference());

        assertThat(payments.get(started.paymentId()).status().name()).isEqualTo("FAILED");
        assertThat(escrow.escrowed(orderId)).isZero();
    }

    @Test
    void escrow_deposit_is_unique_and_ledger_is_append_only() {
        UUID orderId = UUID.randomUUID();
        escrow.hold(orderId, 2_000);
        assertThatThrownBy(() -> escrow.hold(orderId, 2_000)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> jdbc.sql("UPDATE ledger_entries SET amount = 1 WHERE order_id = :o")
                .param("o", orderId).update()).hasMessageContaining("ajout seul");
    }
}
