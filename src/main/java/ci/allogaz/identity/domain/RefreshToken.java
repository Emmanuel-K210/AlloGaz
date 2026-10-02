package ci.allogaz.identity.domain;

import java.time.Instant;
import java.util.UUID;

/** Jeton de rafraîchissement à usage unique (rotation), stocké haché. */
public class RefreshToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private Instant revokedAt;

    public RefreshToken(UUID id, UUID userId, String tokenHash, Instant issuedAt, Instant expiresAt, Instant revokedAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public static RefreshToken issue(UUID userId, String tokenHash, Instant now, java.time.Duration ttl) {
        return new RefreshToken(UUID.randomUUID(), userId, tokenHash, now, now.plus(ttl), null);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }
}
