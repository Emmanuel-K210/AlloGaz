package ci.allogaz.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import ci.allogaz.AbstractIntegrationTest;
import ci.allogaz.catalog.domain.SellerOffer;
import ci.allogaz.ordering.application.OrderCommands;
import ci.allogaz.ordering.application.OrderService;
import ci.allogaz.ordering.domain.Fulfillment;
import ci.allogaz.ordering.domain.Order;
import ci.allogaz.ordering.domain.SaleType;
import ci.allogaz.support.Fixtures;
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

    @Autowired OrderService orders;

    @AfterEach
    void restoreGateway() {
        gateway.setAutoSucceed(true);
    }

    /** Commande acceptée (retrait sur place) d'un montant de quantity x 5 000 F CFA ; renvoie son id. */
    private Order acceptedOrder(User buyer, int quantity) {
        Fixtures.Seller seller = fixtures.verifiedSeller("Dépôt paiement", 5.36, -3.97);
        SellerOffer offer = fixtures.offer(seller, Fixtures.TOTAL_12KG, 5_000L, null, 10);
        Order order = orders.create(buyer.id(), new OrderCommands.CreateOrder(seller.id(), Fulfillment.PICKUP, null,
                null, List.of(new OrderCommands.Line(offer.id(), SaleType.REFILL, quantity))), true);
        return orders.accept(seller.user().id(), order.id());
    }

    @Test
    void immediate_payment_goes_to_escrow_then_released_minus_commission() {
        User buyer = fixtures.user();
        Order order = acceptedOrder(buyer, 2);
        UUID orderId = order.id();
        UUID sellerId = UUID.randomUUID();

        PaymentService.Started started = payments.start(orderId, buyer.id(), buyer.phone().value(), 10_000);

        assertThat(started.status()).isEqualTo("SUCCEEDED");
        assertThat(escrow.escrowed(orderId)).isEqualTo(10_000);
        assertThat(events.stream(PaymentSucceededEvent.class)).singleElement()
                .extracting(PaymentSucceededEvent::orderId).isEqualTo(orderId);

        // Notification en double : idempotente
        payments.handleCallback(started.providerReference());
        assertThat(escrow.escrowed(orderId)).isEqualTo(10_000);
        assertThat(events.stream(PaymentSucceededEvent.class)).hasSize(1);

        escrow.release(orderId, sellerId);
        assertThat(escrow.escrowed(orderId)).isZero();
        assertThat(escrow.sellerBalance(sellerId)).isEqualTo(9_500);
        assertThat(escrow.entries(orderId)).hasSize(5)
                .anySatisfy(e -> assertThat(e.account()).isEqualTo(LedgerAccounts.PLATFORM_COMMISSION));

        // Une seule sortie de séquestre par commande
        assertThatThrownBy(() -> escrow.refund(orderId)).hasMessageContaining("Aucun montant");
    }

    @Test
    void pending_payment_is_confirmed_by_callback_after_checking_the_provider() throws Exception {
        gateway.setAutoSucceed(false);
        User buyer = fixtures.user();
        UUID orderId = acceptedOrder(buyer, 1).id();

        PaymentService.Started started = payments.start(orderId, buyer.id(), buyer.phone().value(), 5_000);
        assertThat(started.status()).isEqualTo("PENDING");

        // Notification reçue alors que l'opérateur n'a pas confirmé : rien ne bouge
        mvc.perform(post("/api/v1/payments/callback/fake").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reference\":\"" + started.providerReference() + "\"}")).andExpect(status().isNoContent());
        assertThat(escrow.escrowed(orderId)).isZero();

        gateway.complete(started.providerReference(), true);
        mvc.perform(post("/api/v1/payments/callback/fake").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reference\":\"" + started.providerReference() + "\"}")).andExpect(status().isNoContent());
        assertThat(escrow.escrowed(orderId)).isEqualTo(5_000);
        assertThat(payments.get(started.paymentId()).status().name()).isEqualTo("SUCCEEDED");
    }

    @Test
    void refused_payment_does_not_touch_escrow() {
        gateway.setAutoSucceed(false);
        User buyer = fixtures.user();
        UUID orderId = acceptedOrder(buyer, 1).id();
        PaymentService.Started started = payments.start(orderId, buyer.id(), buyer.phone().value(), 5_000);

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
