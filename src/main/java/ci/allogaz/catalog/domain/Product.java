package ci.allogaz.catalog.domain;

import java.util.UUID;

/**
 * Produit générique d'une catégorie. Pour le gaz, en Côte d'Ivoire, l'acheteur reconnaît sa bouteille
 * à sa société de provenance (le marketeur qui l'a mise en circulation) et à sa couleur ; une recharge
 * suppose d'échanger une bouteille vide de la même société et de la même contenance.
 * La contenance (6 kg, 12,5 kg) est stockée en grammes pour rester en entiers.
 */
public record Product(UUID id, UUID categoryId, String name, String brand, String company, String bottleColor,
                      Integer capacityGrams, boolean active) {

    public static int kgToGrams(double kg) {
        return (int) Math.round(kg * 1000);
    }
}
