package ci.allogaz.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ci.allogaz.shared.domain.DomainException;
import ci.allogaz.shared.domain.GeoPoint;

class SellerProfileTest {

    private static final LocalDateTime MONDAY_10H = LocalDateTime.of(2026, 10, 5, 10, 0);

    private SellerProfile profile(List<OpeningSlot> hours) {
        return SellerProfile.apply(UUID.randomUUID(), "Dépôt", "Cocody", new GeoPoint(5.35, -3.98), 3_000, hours,
                new DeliveryPolicy(DeliveryMode.INCLUDED, 0), Instant.now());
    }

    @Test
    void new_seller_is_pending_and_cannot_open() {
        SellerProfile p = profile(List.of());
        assertThat(p.status()).isEqualTo(VerificationStatus.PENDING);
        assertThatThrownBy(p::open).isInstanceOf(DomainException.class);
        assertThat(p.canReceiveOrders(MONDAY_10H)).isFalse();
    }

    @Test
    void verified_and_open_seller_receives_orders_within_hours() {
        SellerProfile p = profile(List.of(new OpeningSlot(DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(19, 0))));
        p.verify();
        p.open();
        assertThat(p.canReceiveOrders(MONDAY_10H)).isTrue();
        assertThat(p.canReceiveOrders(MONDAY_10H.withHour(20))).isFalse();
        assertThat(p.canReceiveOrders(MONDAY_10H.plusDays(1))).isFalse();
    }

    @Test
    void pause_and_suspension_stop_orders() {
        SellerProfile p = profile(List.of());
        p.verify();
        p.open();
        p.pause();
        assertThat(p.canReceiveOrders(MONDAY_10H)).isFalse();
        p.open();
        p.suspend();
        assertThat(p.acceptingOrders()).isFalse();
        assertThat(p.status()).isEqualTo(VerificationStatus.SUSPENDED);
    }

    @Test
    void delivery_policy_fees() {
        assertThat(new DeliveryPolicy(DeliveryMode.INCLUDED, 999).deliveryFee()).isZero();
        assertThat(new DeliveryPolicy(DeliveryMode.FIXED_FEE, 500).deliveryFee()).isEqualTo(500);
        assertThat(new DeliveryPolicy(DeliveryMode.PICKUP_ONLY, 0).offersDelivery()).isFalse();
        assertThatThrownBy(() -> new DeliveryPolicy(DeliveryMode.FIXED_FEE, 0)).isInstanceOf(DomainException.class);
    }
}
