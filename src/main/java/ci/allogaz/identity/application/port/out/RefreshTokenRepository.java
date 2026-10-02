package ci.allogaz.identity.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import ci.allogaz.identity.domain.RefreshToken;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByHash(String tokenHash);

    RefreshToken save(RefreshToken token);

    void revokeAllForUser(UUID userId, Instant now);
}
