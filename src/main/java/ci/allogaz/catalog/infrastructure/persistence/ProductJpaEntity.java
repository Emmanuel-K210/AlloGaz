package ci.allogaz.catalog.infrastructure.persistence;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    /** Couleurs en circulation, en minuscules. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "bottle_colors", nullable = false, columnDefinition = "text[]")
    String[] bottleColors = new String[0];

    /** Détails visuels libres (capsule, nuance, séries). */
    String appearance;

    @Column(name = "capacity_grams")
    Integer capacityGrams;

    @Column(nullable = false)
    boolean active;
}
