package ci.allogaz.catalog.infrastructure.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "seller_offers")
class SellerOfferJpaEntity {

    @Id
    UUID id;

    @Column(name = "seller_id", nullable = false)
    UUID sellerId;

    @Column(name = "product_id", nullable = false)
    UUID productId;

    @Column(name = "refill_price")
    Long refillPrice;

    @Column(name = "purchase_price")
    Long purchasePrice;

    @Column(nullable = false)
    int stock;

    @Column(nullable = false)
    boolean active;

    @Version
    long version;
}
