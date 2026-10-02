package ci.allogaz.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import ci.allogaz.shared.domain.ConflictException;
import ci.allogaz.shared.domain.DomainException;

class SellerOfferTest {

    private final SellerOffer offer = SellerOffer.create(UUID.randomUUID(), UUID.randomUUID(), 2_000L, null, 3);

    @Test
    void exposes_price_per_offer_type() {
        assertThat(offer.offers(OfferType.REFILL)).isTrue();
        assertThat(offer.priceFor(OfferType.REFILL)).isEqualTo(2_000L);
        assertThat(offer.offers(OfferType.PURCHASE)).isFalse();
        assertThatThrownBy(() -> offer.priceFor(OfferType.PURCHASE)).hasMessageContaining("ne vend pas");
    }

    @Test
    void decrements_stock_but_never_below_zero() {
        offer.decrementStock(2);
        assertThat(offer.stock()).isEqualTo(1);
        assertThatThrownBy(() -> offer.decrementStock(2)).isInstanceOf(ConflictException.class)
                .hasMessageContaining("Stock insuffisant");
        assertThat(offer.stock()).isEqualTo(1);
    }

    @Test
    void requires_at_least_one_positive_price() {
        assertThatThrownBy(() -> SellerOffer.create(UUID.randomUUID(), UUID.randomUUID(), null, null, 1))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> SellerOffer.create(UUID.randomUUID(), UUID.randomUUID(), 0L, null, 1))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void inactive_offer_is_not_sellable() {
        offer.update(2_000L, null, 3, false);
        assertThat(offer.offers(OfferType.REFILL)).isFalse();
    }
}
