package ci.allogaz.catalog.domain;

import java.util.UUID;

import ci.allogaz.shared.domain.ConflictException;
import ci.allogaz.shared.domain.DomainException;

/**
 * Offre d'un vendeur sur un produit : prix de recharge et/ou prix d'achat (F CFA entiers) et stock
 * de bouteilles pleines. La version sert au verrouillage optimiste lors des décréments concurrents.
 */
public class SellerOffer {

    private final UUID id;
    private final UUID sellerId;
    private final UUID productId;
    private Long refillPrice;
    private Long purchasePrice;
    private int stock;
    private boolean active;
    private final long version;

    public SellerOffer(UUID id, UUID sellerId, UUID productId, Long refillPrice, Long purchasePrice, int stock,
            boolean active, long version) {
        this.id = id;
        this.sellerId = sellerId;
        this.productId = productId;
        this.version = version;
        this.active = active;
        update(refillPrice, purchasePrice, stock, active);
    }

    public static SellerOffer create(UUID sellerId, UUID productId, Long refillPrice, Long purchasePrice, int stock) {
        return new SellerOffer(UUID.randomUUID(), sellerId, productId, refillPrice, purchasePrice, stock, true, 0);
    }

    public final void update(Long refillPrice, Long purchasePrice, int stock, boolean active) {
        if (refillPrice == null && purchasePrice == null) {
            throw new DomainException("INVALID_OFFER", "Indiquez au moins un prix (recharge ou achat).");
        }
        if ((refillPrice != null && refillPrice <= 0) || (purchasePrice != null && purchasePrice <= 0)) {
            throw new DomainException("INVALID_OFFER", "Les prix doivent être des montants positifs en F CFA.");
        }
        if (stock < 0) {
            throw new DomainException("INVALID_OFFER", "Le stock ne peut pas être négatif.");
        }
        this.refillPrice = refillPrice;
        this.purchasePrice = purchasePrice;
        this.stock = stock;
        this.active = active;
    }

    public boolean offers(OfferType type) {
        return active && priceOrNull(type) != null;
    }

    public long priceFor(OfferType type) {
        Long price = active ? priceOrNull(type) : null;
        if (price == null) {
            throw new DomainException("OFFER_UNAVAILABLE",
                    type == OfferType.REFILL ? "Ce dépôt ne propose pas la recharge de ce produit."
                            : "Ce dépôt ne vend pas ce produit.");
        }
        return price;
    }

    public void decrementStock(int quantity) {
        if (quantity <= 0) {
            throw new DomainException("INVALID_QUANTITY", "Quantité invalide.");
        }
        if (stock < quantity) {
            throw new ConflictException("INSUFFICIENT_STOCK", "Stock insuffisant chez le vendeur.");
        }
        stock -= quantity;
    }

    private Long priceOrNull(OfferType type) {
        return type == OfferType.REFILL ? refillPrice : purchasePrice;
    }

    public UUID id() {
        return id;
    }

    public UUID sellerId() {
        return sellerId;
    }

    public UUID productId() {
        return productId;
    }

    public Long refillPrice() {
        return refillPrice;
    }

    public Long purchasePrice() {
        return purchasePrice;
    }

    public int stock() {
        return stock;
    }

    public boolean active() {
        return active;
    }

    public long version() {
        return version;
    }
}
