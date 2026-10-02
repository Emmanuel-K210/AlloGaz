package ci.allogaz.payment.domain;

public enum EntryType {
    /** Paiement de l'acheteur placé en séquestre. */
    ESCROW_DEPOSIT,
    /** Libération du séquestre au vendeur. */
    ESCROW_RELEASE,
    /** Part de la plateforme prélevée à la libération. */
    COMMISSION,
    /** Remboursement de l'acheteur. */
    REFUND
}
