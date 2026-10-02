package ci.allogaz.catalog.domain;

import ci.allogaz.shared.domain.DomainException;

public record DeliveryPolicy(DeliveryMode mode, long fixedFee) {

    public DeliveryPolicy {
        if (mode == null) {
            throw new DomainException("INVALID_DELIVERY_POLICY", "Mode de transport obligatoire.");
        }
        if (mode == DeliveryMode.FIXED_FEE && fixedFee <= 0) {
            throw new DomainException("INVALID_DELIVERY_POLICY", "Les frais fixes doivent être positifs.");
        }
        if (mode != DeliveryMode.FIXED_FEE) {
            fixedFee = 0;
        }
    }

    public boolean offersDelivery() {
        return mode != DeliveryMode.PICKUP_ONLY;
    }

    /** Frais facturés pour une livraison (0 si incluse). */
    public long deliveryFee() {
        return mode == DeliveryMode.FIXED_FEE ? fixedFee : 0;
    }
}
