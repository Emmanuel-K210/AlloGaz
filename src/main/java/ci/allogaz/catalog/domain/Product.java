package ci.allogaz.catalog.domain;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Produit générique d'une catégorie. Pour le gaz, en Côte d'Ivoire, l'acheteur reconnaît sa bouteille
 * à sa société de provenance (le marketeur qui l'a mise en circulation) et à sa couleur ; une recharge
 * suppose d'échanger une bouteille vide de la même société et de la même contenance.
 * Une même société peut avoir plusieurs couleurs en circulation (anciennes et nouvelles flottes) :
 * les couleurs sont donc une liste, normalisée en minuscules ; {@code appearance} décrit les détails
 * (capsule, nuance). La contenance (6 kg, 12,5 kg) est stockée en grammes pour rester en entiers.
 */
public record Product(UUID id, UUID categoryId, String name, String brand, String company,
                      List<String> bottleColors, String appearance, Integer capacityGrams, boolean active) {

    public Product {
        bottleColors = bottleColors == null ? List.of()
                : bottleColors.stream().filter(c -> c != null && !c.isBlank())
                        .map(Product::normalizeColor).distinct().toList();
    }

    public static String normalizeColor(String color) {
        return color == null ? null : color.strip().toLowerCase(Locale.FRENCH);
    }

    public static int kgToGrams(double kg) {
        return (int) Math.round(kg * 1000);
    }
}
