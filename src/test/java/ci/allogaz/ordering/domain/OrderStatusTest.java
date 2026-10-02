package ci.allogaz.ordering.domain;

import static ci.allogaz.ordering.domain.OrderStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Vérifie la table de transitions complète : chaque couple (source, cible) autorisé ou non. */
class OrderStatusTest {

    private static final Map<OrderStatus, Set<OrderStatus>> EXPECTED = Map.ofEntries(
            Map.entry(DRAFT, EnumSet.of(INTENT_SENT, CANCELLED)),
            Map.entry(INTENT_SENT, EnumSet.of(ACCEPTED, REJECTED, EXPIRED, CANCELLED)),
            Map.entry(ACCEPTED, EnumSet.of(PAID, CANCELLED)),
            Map.entry(PAID, EnumSet.of(IN_PREPARATION, CANCELLED, DISPUTED)),
            Map.entry(IN_PREPARATION, EnumSet.of(OUT_FOR_DELIVERY, CANCELLED, DISPUTED)),
            Map.entry(OUT_FOR_DELIVERY, EnumSet.of(DELIVERED, DISPUTED)),
            Map.entry(DELIVERED, EnumSet.of(VALIDATED, DISPUTED)),
            Map.entry(VALIDATED, EnumSet.of(FUNDS_RELEASED)),
            Map.entry(DISPUTED, EnumSet.of(FUNDS_RELEASED, CANCELLED)),
            Map.entry(REJECTED, EnumSet.noneOf(OrderStatus.class)),
            Map.entry(EXPIRED, EnumSet.noneOf(OrderStatus.class)),
            Map.entry(CANCELLED, EnumSet.noneOf(OrderStatus.class)),
            Map.entry(FUNDS_RELEASED, EnumSet.noneOf(OrderStatus.class)));

    static Stream<Arguments> allPairs() {
        return Stream.of(values()).flatMap(from -> Stream.of(values()).map(to -> Arguments.of(from, to)));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("allPairs")
    void transition_table_matches_specification(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isEqualTo(EXPECTED.get(from).contains(to));
    }

    @Test
    void terminal_states() {
        assertThat(Stream.of(values()).filter(OrderStatus::isTerminal))
                .containsExactlyInAnyOrder(REJECTED, EXPIRED, CANCELLED, FUNDS_RELEASED);
    }

    @Test
    void funds_are_in_escrow_only_between_payment_and_release() {
        assertThat(Stream.of(values()).filter(OrderStatus::holdsFundsInEscrow))
                .containsExactlyInAnyOrder(PAID, IN_PREPARATION, OUT_FOR_DELIVERY, DELIVERED, VALIDATED, DISPUTED);
    }
}
