package ci.allogaz.identity.application;

import java.time.Instant;

import ci.allogaz.identity.domain.User;

public record AuthTokens(String accessToken, Instant accessTokenExpiresAt,
                         String refreshToken, Instant refreshTokenExpiresAt,
                         User user, boolean newUser) {
}
