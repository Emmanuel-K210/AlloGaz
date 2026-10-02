package ci.allogaz.payment.domain;

import ci.allogaz.shared.domain.DomainException;

/** Commission de la plateforme en points de base (500 = 5 %), arrondie au F CFA le plus proche. */
public record Commission(int rateBasisPoints) {

    public Commission {
        if (rateBasisPoints < 0 || rateBasisPoints > 10_000) {
            throw new DomainException("INVALID_COMMISSION", "Taux de commission invalide.");
        }
    }

    public Split split(long total) {
        long commission = (total * rateBasisPoints + 5_000) / 10_000;
        return new Split(total - commission, commission);
    }

    public record Split(long sellerAmount, long commission) {
    }
}
