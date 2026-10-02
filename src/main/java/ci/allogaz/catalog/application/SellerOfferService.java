package ci.allogaz.catalog.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ci.allogaz.catalog.application.port.out.SellerOfferRepository;
import ci.allogaz.catalog.domain.Product;
import ci.allogaz.catalog.domain.SellerOffer;
import ci.allogaz.catalog.domain.SellerProfile;
import ci.allogaz.shared.domain.NotFoundException;

@Service
public class SellerOfferService {

    private final SellerOfferRepository offers;
    private final SellerProfileService profiles;
    private final CatalogQueryService catalog;

    public SellerOfferService(SellerOfferRepository offers, SellerProfileService profiles, CatalogQueryService catalog) {
        this.offers = offers;
        this.profiles = profiles;
        this.catalog = catalog;
    }

    /** Offre enrichie du produit, pour l'affichage et pour les autres modules. */
    public record OfferDetails(SellerOffer offer, Product product) {
    }

    /** Crée ou met à jour l'offre du vendeur connecté sur un produit. */
    @Transactional
    public OfferDetails upsert(UUID userId, UUID productId, Long refillPrice, Long purchasePrice, int stock,
            boolean active) {
        SellerProfile seller = profiles.mine(userId);
        Product product = catalog.product(productId);
        SellerOffer offer = offers.findBySellerAndProduct(seller.id(), productId)
                .map(existing -> {
                    existing.update(refillPrice, purchasePrice, stock, active);
                    return existing;
                })
                .orElseGet(() -> {
                    SellerOffer created = SellerOffer.create(seller.id(), productId, refillPrice, purchasePrice, stock);
                    created.update(refillPrice, purchasePrice, stock, active);
                    return created;
                });
        return new OfferDetails(offers.save(offer), product);
    }

    @Transactional(readOnly = true)
    public List<OfferDetails> mine(UUID userId) {
        return ofSeller(profiles.mine(userId).id());
    }

    @Transactional(readOnly = true)
    public List<OfferDetails> ofSeller(UUID sellerId) {
        return offers.findBySeller(sellerId).stream()
                .map(o -> new OfferDetails(o, catalog.product(o.productId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public OfferDetails get(UUID offerId) {
        SellerOffer offer = offers.findById(offerId)
                .orElseThrow(() -> new NotFoundException("OFFER_NOT_FOUND", "Offre introuvable."));
        return new OfferDetails(offer, catalog.product(offer.productId()));
    }

    /**
     * Décrémente le stock. À appeler dans la transaction de l'appelant : en cas de modification
     * concurrente, la version ne correspond plus et l'enregistrement échoue (verrouillage optimiste).
     */
    @Transactional
    public void decrementStock(UUID offerId, int quantity) {
        SellerOffer offer = offers.findById(offerId)
                .orElseThrow(() -> new NotFoundException("OFFER_NOT_FOUND", "Offre introuvable."));
        offer.decrementStock(quantity);
        offers.save(offer);
    }
}
