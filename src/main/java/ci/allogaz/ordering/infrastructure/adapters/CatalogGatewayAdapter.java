package ci.allogaz.ordering.infrastructure.adapters;

import java.util.UUID;

import org.springframework.stereotype.Component;

import ci.allogaz.catalog.application.SellerOfferService;
import ci.allogaz.catalog.application.SellerOfferService.OfferDetails;
import ci.allogaz.catalog.application.SellerProfileService;
import ci.allogaz.catalog.domain.SellerProfile;
import ci.allogaz.ordering.application.port.out.CatalogGateway;

@Component
class CatalogGatewayAdapter implements CatalogGateway {

    private final SellerProfileService profiles;
    private final SellerOfferService offers;

    CatalogGatewayAdapter(SellerProfileService profiles, SellerOfferService offers) {
        this.profiles = profiles;
        this.offers = offers;
    }

    @Override
    public SellerInfo seller(UUID sellerId) {
        SellerProfile p = profiles.get(sellerId);
        return new SellerInfo(p.id(), p.userId(), profiles.canReceiveOrdersNow(p), p.location(),
                p.deliveryRadiusMeters(), p.deliveryPolicy().offersDelivery(), p.deliveryPolicy().deliveryFee());
    }

    @Override
    public OfferInfo offer(UUID offerId) {
        OfferDetails d = offers.get(offerId);
        // Le prix est national (porté par le produit), pas fixé par le dépôt ; "active" reste celui de l'offre :
        // un dépôt peut masquer un produit par ailleurs tarifé sans que ça affecte les autres dépôts.
        return new OfferInfo(d.offer().id(), d.offer().sellerId(), d.product().id(), d.product().name(),
                d.product().refillPrice(), d.product().purchasePrice(), d.offer().stock(), d.offer().active());
    }

    @Override
    public void decrementStock(UUID offerId, int quantity) {
        offers.decrementStock(offerId, quantity);
    }
}
