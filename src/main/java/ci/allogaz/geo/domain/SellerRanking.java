package ci.allogaz.geo.domain;

import java.util.Comparator;
import java.util.List;

/**
 * Classement des dépôts : la distance domine, puis la disponibilité du produit demandé, la note et le taux
 * d'acceptation. Les dépôts lointains ou qui ne livrent pas jusqu'à l'acheteur restent dans la liste, plus bas.
 * Note et taux d'acceptation sont lissés par un a priori (moyenne bayésienne) pour ne pas favoriser
 * un nouveau dépôt avec une seule bonne note, ni pénaliser un dépôt sans historique.
 */
public class SellerRanking {

    static final double PRIOR_RATING = 3.5;
    static final double PRIOR_ACCEPTANCE = 0.8;
    static final double PRIOR_WEIGHT = 5;

    private final RankingWeights weights;

    public SellerRanking(RankingWeights weights) {
        this.weights = weights;
    }

    public List<RankedSeller> rank(List<SellerCandidate> candidates) {
        return candidates.stream()
                .map(this::score)
                .sorted(Comparator.comparingDouble(RankedSeller::score).reversed()
                        .thenComparingDouble(r -> r.seller().distanceMeters()))
                .toList();
    }

    RankedSeller score(SellerCandidate c) {
        double proximity = 1.0 / (1.0 + c.distanceMeters() / weights.halfScoreDistanceMeters());
        if (!c.deliversTo()) {
            proximity *= weights.outOfRadiusFactor();
        }
        double availability = c.hasStock() ? 1.0 : 0.0;
        double rating = (c.ratingSum() + PRIOR_RATING * PRIOR_WEIGHT) / (c.ratingCount() + PRIOR_WEIGHT);
        double acceptance = (c.ordersAccepted() + PRIOR_ACCEPTANCE * PRIOR_WEIGHT) / (c.ordersDecided() + PRIOR_WEIGHT);
        double score = weights.distance() * proximity
                + weights.availability() * availability
                + weights.rating() * (rating / 5.0)
                + weights.acceptance() * acceptance;
        double displayedRating = c.ratingCount() == 0 ? 0 : (double) c.ratingSum() / c.ratingCount();
        double displayedAcceptance = c.ordersDecided() == 0 ? 1 : (double) c.ordersAccepted() / c.ordersDecided();
        return new RankedSeller(c, Math.round(score * 10_000) / 10_000.0, displayedRating, displayedAcceptance);
    }
}
