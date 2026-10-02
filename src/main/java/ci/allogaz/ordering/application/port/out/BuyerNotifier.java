package ci.allogaz.ordering.application.port.out;

import java.util.UUID;

/** Notifications à l'acheteur (SMS via le module identity). */
public interface BuyerNotifier {

    String phoneOf(UUID userId);

    void sendDeliveryCode(UUID buyerId, UUID orderId, String code);
}
