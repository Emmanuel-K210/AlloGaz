package ci.allogaz.geo.application.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import ci.allogaz.geo.domain.SellerCandidate;
import ci.allogaz.shared.domain.GeoPoint;

public interface SellerProximityQuery {

    /**
     * Dépôts vérifiés, ouverts (drapeau et horaires à {@code at}) dans le rayon de recherche, triés par distance,
     * ayant au moins une offre active correspondant aux critères.
     */
    List<SellerCandidate> findCandidates(GeoPoint buyer, double maxRadiusMeters, LocalDateTime at, Criteria criteria,
            int limit);

    record Criteria(UUID productId, String brand, String company, String bottleColor, Integer capacityGrams,
                    SaleType saleType) {
    }

    enum SaleType {
        PURCHASE,
        REFILL
    }
}
