package ci.allogaz.catalog.domain;

import java.util.UUID;

/** Critères de recherche de produits ; un critère nul est ignoré (comparaisons insensibles à la casse). */
public record ProductFilter(UUID categoryId, String brand, String company, String bottleColor, Integer capacityGrams) {

    public static ProductFilter all() {
        return new ProductFilter(null, null, null, null, null);
    }
}
