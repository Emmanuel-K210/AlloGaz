package ci.allogaz.ordering.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import ci.allogaz.shared.domain.DomainException;
import ci.allogaz.shared.domain.GeoPoint;

class OrderTest {

    private static final Instant T0 = Instant.parse("2026-10-02T10:00:00Z");
    private static final Duration TIMEOUT = Duration.ofMinutes(10);
    private static final Duration AUTO = Duration.ofHours(24);
    private static final Predicate<String> GOOD = h -> true;
    private static final Predicate<String> BAD = h -> false;

    private final UUID buyer = UUID.randomUUID();
    private final UUID sellerUser = UUID.randomUUID();

    private Order draft(Fulfillment fulfillment) {
        OrderLine line = new OrderLine(UUID.randomUUID(), UUID.randomUUID(), "Oryx 12,5 kg", SaleType.REFILL, 2, 5_000);
        return Order.draft(buyer, UUID.randomUUID(), sellerUser, List.of(line), fulfillment, "Cocody",
                new GeoPoint(5.35, -3.98), 500, T0);
    }

    private Order paid(Fulfillment fulfillment) {
        Order order = draft(fulfillment);
        order.submit(T0, TIMEOUT);
        order.accept(order.lines(), 500, T0.plusSeconds(60));
        order.markPaid("hash", T0.plusSeconds(120));
        return order;
    }

    @Test
    void nominal_flow_records_every_transition() {
        Order order = paid(Fulfillment.DELIVERY);
        order.startPreparation(T0);
        order.dispatch(T0);
        DeliveryCodeResult result = order.submitDeliveryCode("1234", GOOD, 5, T0);
        order.markFundsReleased(T0);

        assertThat(result.validated()).isTrue();
        assertThat(order.status()).isEqualTo(OrderStatus.FUNDS_RELEASED);
        assertThat(order.pullChanges()).extracting(StatusChange::to).containsExactly(
                OrderStatus.INTENT_SENT, OrderStatus.ACCEPTED, OrderStatus.PAID, OrderStatus.IN_PREPARATION,
                OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED, OrderStatus.VALIDATED,
                OrderStatus.FUNDS_RELEASED);
        assertThat(order.pullChanges()).isEmpty();
    }

    @Test
    void acceptance_freezes_final_prices() {
        Order order = draft(Fulfillment.DELIVERY);
        order.submit(T0, TIMEOUT);
        assertThat(order.pricesFrozen()).isFalse();

        order.accept(List.of(order.lines().getFirst().withUnitPrice(5_200)), 700, T0.plusSeconds(30));

        assertThat(order.pricesFrozen()).isTrue();
        assertThat(order.itemsTotal()).isEqualTo(10_400);
        assertThat(order.transportFee()).isEqualTo(700);
        assertThat(order.total()).isEqualTo(11_100);
    }

    @Test
    void pickup_has_no_transport_fee() {
        Order order = draft(Fulfillment.PICKUP);
        order.submit(T0, TIMEOUT);
        order.accept(order.lines(), 700, T0);
        assertThat(order.transportFee()).isZero();
    }

    @Test
    void seller_cannot_accept_after_the_deadline_and_the_order_expires() {
        Order order = draft(Fulfillment.DELIVERY);
        order.submit(T0, TIMEOUT);
        assertThatThrownBy(() -> order.expire(T0.plus(Duration.ofMinutes(9)))).isInstanceOf(DomainException.class);

        Instant late = T0.plus(TIMEOUT);
        assertThatThrownBy(() -> order.accept(order.lines(), 500, late)).hasMessageContaining("expiré");
        order.expire(late);
        assertThat(order.status()).isEqualTo(OrderStatus.EXPIRED);
        assertThatThrownBy(() -> order.accept(order.lines(), 500, late)).isInstanceOf(DomainException.class);
    }

    @Test
    void wrong_delivery_codes_are_counted_then_locked() {
        Order order = paid(Fulfillment.DELIVERY);
        order.startPreparation(T0);
        order.dispatch(T0);

        assertThat(order.submitDeliveryCode("0000", BAD, 3, T0))
                .isEqualTo(new DeliveryCodeResult(DeliveryCodeResult.Outcome.WRONG_CODE, 2));
        assertThat(order.submitDeliveryCode("12a4", GOOD, 3, T0).outcome())
                .as("format invalide compté comme erreur").isEqualTo(DeliveryCodeResult.Outcome.WRONG_CODE);
        assertThat(order.submitDeliveryCode("0000", BAD, 3, T0).outcome()).isEqualTo(DeliveryCodeResult.Outcome.LOCKED);
        assertThat(order.submitDeliveryCode("1234", GOOD, 3, T0).outcome())
                .as("même le bon code est refusé une fois bloqué").isEqualTo(DeliveryCodeResult.Outcome.LOCKED);
        assertThat(order.status()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);

        order.confirmReceipt(T0);
        assertThat(order.status()).isEqualTo(OrderStatus.VALIDATED);
    }

    @Test
    void delivery_code_is_refused_before_dispatch() {
        Order order = paid(Fulfillment.DELIVERY);
        assertThatThrownBy(() -> order.submitDeliveryCode("1234", GOOD, 5, T0)).isInstanceOf(DomainException.class);
    }

    @Test
    void auto_validation_only_after_the_delay() {
        Order order = paid(Fulfillment.DELIVERY);
        order.startPreparation(T0);
        order.dispatch(T0);
        order.markDelivered(T0, AUTO);

        assertThat(order.isDueForAutoValidation(T0.plus(Duration.ofHours(23)))).isFalse();
        assertThatThrownBy(() -> order.autoValidate(T0.plus(Duration.ofHours(23))))
                .isInstanceOf(DomainException.class);
        order.autoValidate(T0.plus(AUTO));
        assertThat(order.status()).isEqualTo(OrderStatus.VALIDATED);
    }

    @Test
    void dispute_freezes_the_order_until_an_admin_decides() {
        Order order = paid(Fulfillment.DELIVERY);
        order.startPreparation(T0);
        order.dispatch(T0);
        order.markDelivered(T0, AUTO);
        order.openDispute("Bouteille qui fuit", T0);

        assertThat(order.isDueForAutoValidation(T0.plus(Duration.ofDays(3)))).isFalse();
        assertThatThrownBy(() -> order.confirmReceipt(T0)).isInstanceOf(DomainException.class);

        order.resolveDispute(DisputeOutcome.REFUND_BUYER, "Remboursement", T0);
        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.disputeOutcome()).isEqualTo(DisputeOutcome.REFUND_BUYER);
    }

    @Test
    void buyer_cannot_cancel_after_payment_but_seller_can_with_refund() {
        Order order = paid(Fulfillment.DELIVERY);
        assertThatThrownBy(() -> order.cancelByBuyer("plus besoin", T0)).hasMessageContaining("litige");
        assertThat(order.cancelBySeller("rupture", T0)).isTrue();
        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void buyer_cannot_order_from_own_shop_and_needs_a_delivery_location() {
        OrderLine line = new OrderLine(UUID.randomUUID(), UUID.randomUUID(), "B6", SaleType.PURCHASE, 1, 14_000);
        assertThatThrownBy(() -> Order.draft(buyer, UUID.randomUUID(), buyer, List.of(line), Fulfillment.PICKUP,
                null, null, 0, T0)).hasMessageContaining("propre dépôt");
        assertThatThrownBy(() -> Order.draft(buyer, UUID.randomUUID(), sellerUser, List.of(line),
                Fulfillment.DELIVERY, null, null, 0, T0)).hasMessageContaining("position de livraison");
    }

    @Test
    void rating_once_after_validation() {
        Order order = paid(Fulfillment.PICKUP);
        assertThatThrownBy(() -> order.rate(5)).isInstanceOf(DomainException.class);
        order.startPreparation(T0);
        order.dispatch(T0);
        order.confirmReceipt(T0);
        order.rate(4);
        assertThatThrownBy(() -> order.rate(5)).hasMessageContaining("déjà");
    }
}
