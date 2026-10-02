package ci.allogaz.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import ci.allogaz.identity.application.port.out.RefreshTokenRepository;
import ci.allogaz.identity.domain.RefreshToken;

@Repository
class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository jpa;

    RefreshTokenRepositoryAdapter(SpringDataRefreshTokenRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<RefreshToken> findByHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(e -> new RefreshToken(e.getId(), e.getUserId(), e.getTokenHash(),
                e.getIssuedAt(), e.getExpiresAt(), e.getRevokedAt()));
    }

    @Override
    public RefreshToken save(RefreshToken t) {
        jpa.save(new RefreshTokenJpaEntity(t.id(), t.userId(), t.tokenHash(), t.issuedAt(), t.expiresAt(), t.revokedAt()));
        return t;
    }

    @Override
    public void revokeAllForUser(UUID userId, Instant now) {
        jpa.revokeAllForUser(userId, now);
    }
}
