package ci.allogaz.ordering.application.port.out;

import java.util.UUID;

/** Alimente les statistiques de classement des dépôts (module geo). */
public interface SellerReputation {

    void recordDecision(UUID sellerId, boolean accepted);

    void recordRating(UUID sellerId, int stars);
}
