package ci.allogaz.shared.domain;

/** L'utilisateur est authentifié mais n'a pas le droit d'agir sur cette ressource. */
public class ForbiddenException extends DomainException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }
}
