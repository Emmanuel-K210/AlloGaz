package ci.allogaz.catalog.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ci.allogaz.catalog.domain.SellerOffer;

public interface SellerOfferRepository {

    Optional<SellerOffer> findById(UUID id);

    Optional<SellerOffer> findBySellerAndProduct(UUID sellerId, UUID productId);

    List<SellerOffer> findBySeller(UUID sellerId);

    /** Enregistre en contrôlant la version (verrouillage optimiste). */
    SellerOffer save(SellerOffer offer);
}
