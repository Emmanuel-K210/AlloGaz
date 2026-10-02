package ci.allogaz.shared.domain;

/**
 * Violation d'une règle métier. Le code est stable et exploitable par le client,
 * le message est destiné à l'utilisateur (en français).
 */
public class DomainException extends RuntimeException {

    private final String code;

    public DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
