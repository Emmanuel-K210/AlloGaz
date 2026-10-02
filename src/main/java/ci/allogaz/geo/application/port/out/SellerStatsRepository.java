package ci.allogaz.geo.application.port.out;

import java.util.UUID;

/** Statistiques de réputation agrégées par dépôt (note, taux d'acceptation). */
public interface SellerStatsRepository {

    void recordDecision(UUID sellerId, boolean accepted);

    void recordRating(UUID sellerId, int stars);
}
