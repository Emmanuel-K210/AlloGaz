package ci.allogaz.shared.domain;

/** Identité non prouvée (code OTP ou jeton invalide). */
public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String code, String message) {
        super(code, message);
    }
}
