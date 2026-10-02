package ci.allogaz.ordering.infrastructure.adapters;

import java.util.UUID;

import org.springframework.stereotype.Component;

import ci.allogaz.identity.application.UserService;
import ci.allogaz.identity.application.port.out.SmsSender;
import ci.allogaz.identity.domain.User;
import ci.allogaz.ordering.application.port.out.BuyerNotifier;

@Component
class BuyerNotifierAdapter implements BuyerNotifier {

    private final UserService users;
    private final SmsSender sms;

    BuyerNotifierAdapter(UserService users, SmsSender sms) {
        this.users = users;
        this.sms = sms;
    }

    @Override
    public String phoneOf(UUID userId) {
        return users.get(userId).phone().value();
    }

    @Override
    public void sendDeliveryCode(UUID buyerId, UUID orderId, String code) {
        User buyer = users.get(buyerId);
        sms.send(buyer.phone(), "AlloGaz : paiement reçu. Code de livraison " + code
                + " (commande " + orderId.toString().substring(0, 8) + "). Donnez-le au livreur seulement à la remise.");
    }
}
