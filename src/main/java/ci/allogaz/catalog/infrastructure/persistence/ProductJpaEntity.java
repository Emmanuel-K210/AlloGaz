package ci.allogaz.catalog.infrastructure.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
class ProductJpaEntity {

    @Id
    UUID id;

    @Column(name = "category_id", nullable = false)
    UUID categoryId;

    @Column(nullable = false)
    String name;

    String brand;

    /** Société de provenance (marketeur). */
    String company;

    @Column(name = "bottle_color")
    String bottleColor;

    @Column(name = "capacity_grams")
    Integer capacityGrams;

    @Column(nullable = false)
    boolean active;
}
