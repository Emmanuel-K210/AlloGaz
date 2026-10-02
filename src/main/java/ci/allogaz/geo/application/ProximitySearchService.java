package ci.allogaz.geo.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ci.allogaz.geo.application.port.out.SellerProximityQuery;
import ci.allogaz.geo.application.port.out.SellerProximityQuery.Criteria;
import ci.allogaz.geo.application.port.out.SellerStatsRepository;
import ci.allogaz.geo.domain.RankedSeller;
import ci.allogaz.geo.domain.SellerRanking;
import ci.allogaz.shared.domain.GeoPoint;

@Service
public class ProximitySearchService {

    private final SellerProximityQuery query;
    private final SellerStatsRepository stats;
    private final SearchSettings settings;
    private final SellerRanking ranking;
    private final Clock clock;

    public ProximitySearchService(SellerProximityQuery query, SellerStatsRepository stats, SearchSettings settings,
            Clock clock) {
        this.query = query;
        this.stats = stats;
        this.settings = settings;
        this.ranking = new SellerRanking(settings.weights());
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RankedSeller> search(GeoPoint buyer, Criteria criteria, int limit) {
        int bounded = Math.clamp(limit, 1, settings.candidateLimit());
        return ranking.rank(query.findCandidates(buyer, settings.maxRadiusMeters(), LocalDateTime.now(clock), criteria,
                settings.candidateLimit())).stream().limit(bounded).toList();
    }

    @Transactional
    public void recordDecision(UUID sellerId, boolean accepted) {
        stats.recordDecision(sellerId, accepted);
    }

    @Transactional
    public void recordRating(UUID sellerId, int stars) {
        stats.recordRating(sellerId, stars);
    }
}
