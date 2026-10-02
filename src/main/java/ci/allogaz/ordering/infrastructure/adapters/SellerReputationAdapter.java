package ci.allogaz.ordering.infrastructure.adapters;

import java.util.UUID;

import org.springframework.stereotype.Component;

import ci.allogaz.geo.application.ProximitySearchService;
import ci.allogaz.ordering.application.port.out.SellerReputation;

@Component
class SellerReputationAdapter implements SellerReputation {

    private final ProximitySearchService search;

    SellerReputationAdapter(ProximitySearchService search) {
        this.search = search;
    }

    @Override
    public void recordDecision(UUID sellerId, boolean accepted) {
        search.recordDecision(sellerId, accepted);
    }

    @Override
    public void recordRating(UUID sellerId, int stars) {
        search.recordRating(sellerId, stars);
    }
}
