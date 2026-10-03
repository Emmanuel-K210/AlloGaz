package ci.allogaz.catalog.domain;

import java.util.UUID;

import ci.allogaz.shared.domain.ConflictException;
import ci.allogaz.shared.domain.DomainException;

/**
 * Offre d'un vendeur sur un produit : son stock de bouteilles pleines et s'il l'affiche aux acheteurs.
 * Le prix n'est pas ici : il est national et réglementé, porté par {@link Product} (non modifiable par
 * le vendeur). La version sert au verrouillage optimiste lors des décréments concurrents.
 */
public class SellerOffer {

    private final UUID id;
    private final UUID sellerId;
    private final UUID productId;
    private int stock;
    private boolean active;
    private final long version;

    public SellerOffer(UUID id, UUID sellerId, UUID productId, int stock, boolean active, long version) {
        this.id = id;
        this.sellerId = sellerId;
        this.productId = productId;
        this.version = version;
        update(stock, active);
    }

    public static SellerOffer create(UUID sellerId, UUID productId, int stock) {
        return new SellerOffer(UUID.randomUUID(), sellerId, productId, stock, true, 0);
    }

    public final void update(int stock, boolean active) {
        if (stock < 0) {
            throw new DomainException("INVALID_OFFER", "Le stock ne peut pas être négatif.");
        }
        this.stock = stock;
        this.active = active;
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

    public UUID id() {
        return id;
    }

    public UUID sellerId() {
        return sellerId;
    }

    public UUID productId() {
        return productId;
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
