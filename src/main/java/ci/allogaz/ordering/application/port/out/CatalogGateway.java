package ci.allogaz.ordering.application.port.out;

import java.util.UUID;

import ci.allogaz.ordering.domain.SaleType;
import ci.allogaz.shared.domain.GeoPoint;

/** Lecture du catalogue et décrément du stock (module catalog). */
public interface CatalogGateway {

    SellerInfo seller(UUID sellerId);

    OfferInfo offer(UUID offerId);

    /** Décrément sous verrouillage optimiste ; échoue sur modification concurrente ou stock insuffisant. */
    void decrementStock(UUID offerId, int quantity);

    record SellerInfo(UUID id, UUID userId, boolean canReceiveOrdersNow, GeoPoint location, int deliveryRadiusMeters,
                      boolean offersDelivery, long deliveryFee) {
    }

    record OfferInfo(UUID id, UUID sellerId, UUID productId, String productName, Long refillPrice,
                     Long purchasePrice, int stock, boolean active) {

        public Long priceFor(SaleType type) {
            return active ? (type == SaleType.REFILL ? refillPrice : purchasePrice) : null;
        }
    }
}
