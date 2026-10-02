package ci.allogaz.ordering.application;

import java.time.Duration;

/**
 * @param sellerResponseTimeout délai de réponse du vendeur avant expiration (10 min par défaut)
 * @param autoValidationDelay délai après livraison avant validation automatique
 * @param maxDeliveryCodeAttempts essais autorisés pour le code de livraison
 * @param batchSize nombre de commandes traitées par passage des tâches planifiées
 */
public record OrderingSettings(Duration sellerResponseTimeout, Duration autoValidationDelay,
                               int maxDeliveryCodeAttempts, int batchSize) {
}
