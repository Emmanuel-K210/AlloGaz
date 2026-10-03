package ci.allogaz.payment.domain;

import java.util.UUID;

/** Comptes du grand livre. */
public final class LedgerAccounts {

    /** Argent reçu de l'opérateur Mobile Money (ou rendu à l'acheteur). */
    public static final String MOBILE_MONEY = "EXTERNAL:MOBILE_MONEY";
    /** Argent bloqué en séquestre en attendant la validation de la commande. */
    public static final String ESCROW = "ESCROW";
    /** Commissions de la plateforme (nette des frais de passerelle). */
    public static final String PLATFORM_COMMISSION = "PLATFORM:COMMISSION";
    /** Frais prélevés par l'agrégateur Mobile Money (encaissement et reversement), à la charge de la plateforme. */
    public static final String PLATFORM_GATEWAY_FEES = "PLATFORM:GATEWAY_FEES";

    private LedgerAccounts() {
    }

    /** Solde dû au vendeur (à reverser par Mobile Money). */
    public static String seller(UUID sellerId) {
        return "SELLER:" + sellerId;
    }
}
