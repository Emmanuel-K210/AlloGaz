package ci.allogaz.catalog.domain;

/** Politique de transport choisie par le vendeur. */
public enum DeliveryMode {
    /** Livraison gratuite (incluse dans le prix). */
    INCLUDED,
    /** Livraison facturée à un montant fixe. */
    FIXED_FEE,
    /** Pas de livraison : retrait sur place uniquement. */
    PICKUP_ONLY
}
