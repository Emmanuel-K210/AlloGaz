package ci.allogaz.ordering.domain;

/** Résultat de la saisie du code de livraison par le vendeur. */
public record DeliveryCodeResult(Outcome outcome, int remainingAttempts) {

    public enum Outcome {
        VALIDATED,
        WRONG_CODE,
        LOCKED
    }

    public boolean validated() {
        return outcome == Outcome.VALIDATED;
    }
}
