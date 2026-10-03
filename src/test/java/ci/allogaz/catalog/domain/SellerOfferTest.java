package ci.allogaz.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import ci.allogaz.shared.domain.ConflictException;
import ci.allogaz.shared.domain.DomainException;

class SellerOfferTest {

    private final SellerOffer offer = SellerOffer.create(UUID.randomUUID(), UUID.randomUUID(), 3);

    @Test
    void decrements_stock_but_never_below_zero() {
        offer.decrementStock(2);
        assertThat(offer.stock()).isEqualTo(1);
        assertThatThrownBy(() -> offer.decrementStock(2)).isInstanceOf(ConflictException.class)
                .hasMessageContaining("Stock insuffisant");
        assertThat(offer.stock()).isEqualTo(1);
    }

    @Test
    void rejects_a_negative_stock() {
        assertThatThrownBy(() -> offer.update(-1, true)).isInstanceOf(DomainException.class);
    }

    @Test
    void can_be_deactivated_without_affecting_stock() {
        offer.update(3, false);
        assertThat(offer.active()).isFalse();
        assertThat(offer.stock()).isEqualTo(3);
    }
}
