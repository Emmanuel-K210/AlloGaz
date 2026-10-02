package ci.allogaz.shared.domain;

public class TooManyRequestsException extends DomainException {

    public TooManyRequestsException(String code, String message) {
        super(code, message);
    }
}
