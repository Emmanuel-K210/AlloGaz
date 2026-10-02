package ci.allogaz.geo.domain;

/**
 * Pondération du score de classement. La distance pèse le plus ; halfScoreDistanceMeters est la distance
 * à laquelle le score de proximité vaut 0,5 ; outOfRadiusFactor pénalise un dépôt qui ne livre pas jusqu'à l'acheteur.
 */
public record RankingWeights(double distance, double availability, double rating, double acceptance,
                             double halfScoreDistanceMeters, double outOfRadiusFactor) {

    public static RankingWeights defaults() {
        return new RankingWeights(0.50, 0.25, 0.15, 0.10, 2_000, 0.6);
    }
}
