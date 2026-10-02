package ci.allogaz.identity.infrastructure.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "allogaz.identity")
public record IdentityProperties(Jwt jwt, Otp otp) {

    public record Jwt(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {
    }

    public record Otp(int length, Duration ttl, int maxAttempts, Duration resendCooldown, int maxSendsPerHour) {
    }
}
