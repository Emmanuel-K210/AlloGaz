package ci.allogaz.identity.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ci.allogaz.identity.application.port.out.OtpStore;
import ci.allogaz.identity.application.port.out.SmsSender;
import ci.allogaz.identity.application.port.out.UserRepository;
import ci.allogaz.identity.domain.PhoneNumber;
import ci.allogaz.identity.domain.User;
import ci.allogaz.shared.application.SecretHasher;
import ci.allogaz.shared.domain.TooManyRequestsException;
import ci.allogaz.shared.domain.UnauthorizedException;

/** Inscription et connexion sans mot de passe : un code OTP envoyé par SMS. */
@Service
public class OtpAuthenticationService {

    private static final Duration SEND_WINDOW = Duration.ofHours(1);

    private final OtpStore otpStore;
    private final SmsSender smsSender;
    private final UserRepository users;
    private final SecretHasher hasher;
    private final TokenService tokens;
    private final OtpPolicy policy;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public OtpAuthenticationService(OtpStore otpStore, SmsSender smsSender, UserRepository users,
            SecretHasher hasher, TokenService tokens, OtpPolicy policy, Clock clock) {
        this.otpStore = otpStore;
        this.smsSender = smsSender;
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
        this.policy = policy;
        this.clock = clock;
    }

    /** Envoie un code ; renvoie la durée de validité. */
    public Duration requestCode(String rawPhone) {
        PhoneNumber phone = PhoneNumber.parse(rawPhone);
        Optional<Duration> cooldown = otpStore.resendCooldown(phone);
        if (cooldown.isPresent()) {
            throw new TooManyRequestsException("OTP_COOLDOWN",
                    "Veuillez patienter " + Math.max(1, cooldown.get().toSeconds()) + " s avant de redemander un code.");
        }
        if (otpStore.incrementSendCount(phone, SEND_WINDOW) > policy.maxSendsPerHour()) {
            throw new TooManyRequestsException("OTP_SEND_LIMIT",
                    "Trop de codes demandés pour ce numéro. Réessayez dans une heure.");
        }
        String code = generateCode();
        otpStore.storeCode(phone, hasher.hash(phone.value() + ":" + code), policy.ttl(), policy.resendCooldown());
        smsSender.send(phone, "AlloGaz : votre code de connexion est " + code
                + ". Il expire dans " + policy.ttl().toMinutes() + " min. Ne le partagez avec personne.");
        return policy.ttl();
    }

    /** Vérifie le code ; crée le compte au premier passage. */
    @Transactional
    public AuthTokens verifyCode(String rawPhone, String code) {
        PhoneNumber phone = PhoneNumber.parse(rawPhone);
        String expectedHash = otpStore.findCodeHash(phone)
                .orElseThrow(() -> new UnauthorizedException("OTP_EXPIRED",
                        "Code expiré ou inexistant. Demandez un nouveau code."));
        long attempts = otpStore.incrementAttempts(phone);
        if (attempts > policy.maxAttempts()) {
            otpStore.deleteCode(phone);
            throw new TooManyRequestsException("OTP_TOO_MANY_ATTEMPTS",
                    "Trop d'essais. Demandez un nouveau code.");
        }
        if (!hasher.matches(phone.value() + ":" + code, expectedHash)) {
            long remaining = policy.maxAttempts() - attempts;
            if (remaining <= 0) {
                otpStore.deleteCode(phone);
                throw new TooManyRequestsException("OTP_TOO_MANY_ATTEMPTS",
                        "Trop d'essais. Demandez un nouveau code.");
            }
            throw new UnauthorizedException("OTP_INVALID",
                    "Code incorrect. Essais restants : " + remaining + ".");
        }
        otpStore.deleteCode(phone);

        Optional<User> existing = users.findByPhone(phone);
        User user = existing.orElseGet(() -> users.save(User.register(phone, clock.instant())));
        return tokens.issueFor(user, existing.isEmpty());
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, policy.length());
        return String.format("%0" + policy.length() + "d", random.nextInt(bound));
    }
}
