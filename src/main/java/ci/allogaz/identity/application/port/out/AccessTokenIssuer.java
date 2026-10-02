package ci.allogaz.identity.application.port.out;

import java.time.Instant;

import ci.allogaz.identity.domain.User;

public interface AccessTokenIssuer {

    IssuedToken issue(User user);

    record IssuedToken(String value, Instant expiresAt) {
    }
}
