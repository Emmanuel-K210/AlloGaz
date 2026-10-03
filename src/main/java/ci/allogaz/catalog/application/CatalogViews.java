package ci.allogaz.catalog.application;

import java.io.Serializable;
import java.util.List;
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

    /** Le prix (recharge/achat) est national et réglementé : identique chez tous les dépôts. */
    public record ProductView(UUID id, UUID categoryId, String name, String brand, String company,
                              List<String> bottleColors, String appearance, Integer capacityGrams, Long refillPrice,
                              Long purchasePrice) implements Serializable {

        static ProductView from(Product p) {
            return new ProductView(p.id(), p.categoryId(), p.name(), p.brand(), p.company(), p.bottleColors(),
                    p.appearance(), p.capacityGrams(), p.refillPrice(), p.purchasePrice());
        }
    }
}
