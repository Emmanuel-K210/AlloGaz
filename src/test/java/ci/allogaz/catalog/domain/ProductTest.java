package ci.allogaz.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ci.allogaz.shared.domain.DomainException;

class ProductTest {

    private Product product(Long refillPrice, Long purchasePrice) {
        return new Product(UUID.randomUUID(), UUID.randomUUID(), "Bouteille test", "Oryx", "Oryx Energies",
                List.of("Bleue"), null, 12_500, refillPrice, purchasePrice, true);
    }

    @Test
    void accepts_no_price_at_all_a_product_awaiting_a_national_tariff() {
        Product p = product(null, null);
        assertThat(p.refillPrice()).isNull();
        assertThat(p.purchasePrice()).isNull();
    }

    @Test
    void rejects_a_zero_or_negative_price() {
        assertThatThrownBy(() -> product(0L, null)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> product(-100L, null)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> product(null, 0L)).isInstanceOf(DomainException.class);
    }

    @Test
    void accepts_independent_refill_and_purchase_prices() {
        Product p = product(5_200L, 26_000L);
        assertThat(p.refillPrice()).isEqualTo(5_200L);
        assertThat(p.purchasePrice()).isEqualTo(26_000L);
    }
}
