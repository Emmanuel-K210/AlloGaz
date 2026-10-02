package ci.allogaz.catalog.application;

import java.io.Serializable;
import java.util.UUID;

import ci.allogaz.catalog.domain.Category;
import ci.allogaz.catalog.domain.Product;

/** Vues en lecture, sérialisables pour le cache Redis. */
public final class CatalogViews {

    private CatalogViews() {
    }

    public record CategoryView(UUID id, String slug, String name) implements Serializable {

        static CategoryView from(Category c) {
            return new CategoryView(c.id(), c.slug(), c.name());
        }
    }

    public record ProductView(UUID id, UUID categoryId, String name, String brand, String company,
                              String bottleColor, Integer capacityGrams)
            implements Serializable {

        static ProductView from(Product p) {
            return new ProductView(p.id(), p.categoryId(), p.name(), p.brand(), p.company(), p.bottleColor(),
                    p.capacityGrams());
        }
    }
}
