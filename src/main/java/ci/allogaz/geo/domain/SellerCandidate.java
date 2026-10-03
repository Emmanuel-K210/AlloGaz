package ci.allogaz.geo.domain;

import java.util.List;
import java.util.UUID;

/** Dépôt trouvé autour de l'acheteur, avec ses offres correspondant aux filtres. */
public record SellerCandidate(UUID sellerId, String shopName, String address, double latitude, double longitude,
                              double distanceMeters, int deliveryRadiusMeters, String deliveryMode, long deliveryFee,
                              long ratingSum, long ratingCount, long ordersDecided, long ordersAccepted,
                              List<MatchingOffer> offers, boolean universalExchange) {

    public record MatchingOffer(UUID offerId, UUID productId, String productName, String brand, String company,
                                List<String> bottleColors, String appearance, Integer capacityGrams, Long refillPrice, Long purchasePrice,
                                int stock) {
    }

    public boolean deliversTo() {
        return !"PICKUP_ONLY".equals(deliveryMode) && distanceMeters <= deliveryRadiusMeters;
    }

    public boolean hasStock() {
        return offers.stream().anyMatch(o -> o.stock() > 0);
    }
}
