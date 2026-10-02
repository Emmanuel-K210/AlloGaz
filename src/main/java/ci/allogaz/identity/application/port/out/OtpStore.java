package ci.allogaz.identity.application.port.out;

import java.time.Duration;
import java.util.Optional;

import ci.allogaz.identity.domain.PhoneNumber;

/** Stockage éphémère des codes OTP et des compteurs anti-abus (Redis). */
public interface OtpStore {

    /** Durée restante avant de pouvoir redemander un code, si un délai est en cours. */
    Optional<Duration> resendCooldown(PhoneNumber phone);

    /** Incrémente le nombre d'envois dans la fenêtre glissante et renvoie la nouvelle valeur. */
    long incrementSendCount(PhoneNumber phone, Duration window);

    void storeCode(PhoneNumber phone, String codeHash, Duration ttl, Duration resendCooldown);

    Optional<String> findCodeHash(PhoneNumber phone);

    /** Incrémente atomiquement le nombre d'essais et renvoie la nouvelle valeur. */
    long incrementAttempts(PhoneNumber phone);

    void deleteCode(PhoneNumber phone);
}
