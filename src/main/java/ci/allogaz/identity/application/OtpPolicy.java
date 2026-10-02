package ci.allogaz.identity.application;

import java.time.Duration;

/**
 * Règles de l'OTP : longueur, durée de vie courte, nombre d'essais, délai entre deux envois
 * et plafond d'envois par heure (coût SMS et anti-harcèlement).
 */
public record OtpPolicy(int length, Duration ttl, int maxAttempts, Duration resendCooldown, int maxSendsPerHour) {
}
