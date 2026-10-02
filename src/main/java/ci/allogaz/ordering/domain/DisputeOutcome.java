package ci.allogaz.ordering.domain;

public enum DisputeOutcome {
    /** Les fonds sont libérés au vendeur (moins la commission). */
    RELEASE_TO_SELLER,
    /** L'acheteur est intégralement remboursé. */
    REFUND_BUYER
}
