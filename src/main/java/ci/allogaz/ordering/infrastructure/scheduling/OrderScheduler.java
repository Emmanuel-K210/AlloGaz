package ci.allogaz.ordering.infrastructure.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ci.allogaz.ordering.application.OrderService;

/**
 * Tâches planifiées. Sûres en multi-instance : chaque commande est traitée dans sa transaction,
 * et le verrouillage optimiste écarte une commande modifiée en parallèle.
 */
@Component
@ConditionalOnProperty(name = "allogaz.scheduling.enabled", havingValue = "true", matchIfMissing = true)
class OrderScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderScheduler.class);

    private final OrderService orders;

    OrderScheduler(OrderService orders) {
        this.orders = orders;
    }

    @Scheduled(fixedDelayString = "${allogaz.ordering.expiration-check-interval:PT30S}")
    void expireOverdueIntents() {
        int expired = orders.expireOverdueIntents();
        if (expired > 0) {
            log.info("{} commande(s) expirée(s) faute de réponse du vendeur", expired);
        }
    }

    @Scheduled(fixedDelayString = "${allogaz.ordering.auto-validation-check-interval:PT5M}")
    void autoValidate() {
        int validated = orders.autoValidateDeliveredOrders();
        if (validated > 0) {
            log.info("{} commande(s) validée(s) automatiquement", validated);
        }
    }
}
