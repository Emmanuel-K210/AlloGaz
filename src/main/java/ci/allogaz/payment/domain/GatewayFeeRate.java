package ci.allogaz.payment.domain;

import ci.allogaz.shared.domain.DomainException;

/**
 * Taux estimé des frais prélevés par l'agrégateur Mobile Money (encaissement à la commande, reversement
 * au vendeur), en points de base. Ni l'acheteur ni le vendeur ne les paient : ils sont prélevés sur la
 * commission de la plateforme, pour que la marge nette réelle reste visible dans le grand livre.
 * Valeur de dev à ajuster avec les tarifs réels une fois CinetPay/PayDunya branché.
 */
public record GatewayFeeRate(int rateBasisPoints) {

    public GatewayFeeRate {
        if (rateBasisPoints < 0 || rateBasisPoints > 10_000) {
            throw new DomainException("INVALID_GATEWAY_FEE", "Taux de frais de passerelle invalide.");
        }
    }

    public long amountFor(long total) {
        return (total * rateBasisPoints + 5_000) / 10_000;
    }
}
