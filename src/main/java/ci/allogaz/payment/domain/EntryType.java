package ci.allogaz.payment.domain;

public enum EntryType {
    /** Paiement de l'acheteur placé en séquestre. */
    ESCROW_DEPOSIT,
    /** Libération du séquestre au vendeur. */
    ESCROW_RELEASE,
    /** Part de la plateforme prélevée à la libération. */
    COMMISSION,
    /** Frais de l'agrégateur Mobile Money (encaissement + reversement), à la charge de la plateforme. */
    GATEWAY_FEE,
    /** Remboursement de l'acheteur. */
    REFUND
}
