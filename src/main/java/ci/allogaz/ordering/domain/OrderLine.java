package ci.allogaz.ordering.domain;

import java.util.UUID;

import ci.allogaz.shared.domain.DomainException;

/** Ligne de commande ; le prix unitaire (F CFA) est indicatif jusqu'à l'acceptation, puis figé. */
public record OrderLine(UUID offerId, UUID productId, String productName, SaleType saleType, int quantity,
                        long unitPrice) {

    public OrderLine {
        if (quantity <= 0 || quantity > 20) {
            throw new DomainException("INVALID_QUANTITY", "La quantité doit être comprise entre 1 et 20.");
        }
        if (unitPrice <= 0) {
            throw new DomainException("INVALID_PRICE", "Prix unitaire invalide.");
        }
    }

    public long total() {
        return Math.multiplyExact(unitPrice, quantity);
    }

    public OrderLine withUnitPrice(long price) {
        return new OrderLine(offerId, productId, productName, saleType, quantity, price);
    }
}
