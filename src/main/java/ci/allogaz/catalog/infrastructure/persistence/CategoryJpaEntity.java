package ci.allogaz.catalog.infrastructure.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
class CategoryJpaEntity {

    @Id
    UUID id;

    @Column(nullable = false, unique = true)
    String slug;

    @Column(nullable = false)
    String name;
}
