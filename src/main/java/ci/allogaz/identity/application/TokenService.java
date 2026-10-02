package ci.allogaz.identity.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ci.allogaz.identity.application.port.out.AccessTokenIssuer;
import ci.allogaz.identity.application.port.out.RefreshTokenRepository;
import ci.allogaz.identity.application.port.out.UserRepository;
import ci.allogaz.identity.domain.RefreshToken;
import ci.allogaz.identity.domain.User;
import ci.allogaz.shared.domain.UnauthorizedException;

/**
 * Jeton d'accès court (JWT) + refresh token opaque à rotation. Réutiliser un refresh token
 * déjà consommé révoque toute la famille de jetons de l'utilisateur (vol présumé).
 */
@Service
public class TokenService {

    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenRepository refreshTokens;
    private final UserRepository users;
    private final Duration refreshTokenTtl;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public TokenService(AccessTokenIssuer accessTokenIssuer, RefreshTokenRepository refreshTokens,
            UserRepository users, RefreshTokenTtl refreshTokenTtl, Clock clock) {
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokens = refreshTokens;
        this.users = users;
        this.refreshTokenTtl = refreshTokenTtl.value();
        this.clock = clock;
    }

    @Transactional
    public AuthTokens issueFor(User user, boolean newUser) {
        Instant now = clock.instant();
        String raw = randomToken();
        RefreshToken refresh = refreshTokens.save(RefreshToken.issue(user.id(), sha256(raw), now, refreshTokenTtl));
        AccessTokenIssuer.IssuedToken access = accessTokenIssuer.issue(user);
        return new AuthTokens(access.value(), access.expiresAt(), raw, refresh.expiresAt(), user, newUser);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthTokens refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshToken current = refreshTokens.findByHash(sha256(rawRefreshToken))
                .orElseThrow(TokenService::invalid);
        if (current.isRevoked()) {
            refreshTokens.revokeAllForUser(current.userId(), now);
            throw new UnauthorizedException("REFRESH_TOKEN_REUSED",
                    "Session invalidée par sécurité. Veuillez vous reconnecter.");
        }
        if (current.isExpired(now)) {
            throw invalid();
        }
        current.revoke(now);
        refreshTokens.save(current);
        User user = users.findById(current.userId()).orElseThrow(TokenService::invalid);
        return issueFor(user, false);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.findByHash(sha256(rawRefreshToken)).ifPresent(token -> {
            token.revoke(clock.instant());
            refreshTokens.save(token);
        });
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static UnauthorizedException invalid() {
        return new UnauthorizedException("INVALID_REFRESH_TOKEN", "Session expirée. Veuillez vous reconnecter.");
    }

    /** Durée de vie du refresh token (enveloppe typée pour l'injection). */
    public record RefreshTokenTtl(Duration value) {
    }
}
