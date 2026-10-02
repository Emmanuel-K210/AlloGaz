package ci.allogaz.ordering.infrastructure.persistence;

import java.util.UUID;

import ci.allogaz.ordering.domain.SaleType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
class OrderLineEmbeddable {

    @Column(name = "offer_id", nullable = false)
    UUID offerId;

    @Column(name = "product_id", nullable = false)
    UUID productId;

    @Column(name = "product_name", nullable = false)
    String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false)
    SaleType saleType;

    @Column(nullable = false)
    int quantity;

    @Column(name = "unit_price", nullable = false)
    long unitPrice;
}
