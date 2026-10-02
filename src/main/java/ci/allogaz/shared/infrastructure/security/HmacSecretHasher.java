package ci.allogaz.shared.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ci.allogaz.shared.application.SecretHasher;

/**
 * HMAC-SHA256 avec un « poivre » serveur : un code à 4 ou 6 chiffres haché sans secret
 * se retrouverait par force brute en quelques millisecondes en cas de fuite de la base.
 */
@Component
public class HmacSecretHasher implements SecretHasher {

    private final SecretKeySpec key;

    public HmacSecretHasher(@Value("${allogaz.security.hash-pepper}") String pepper) {
        this.key = new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Override
    public String hash(String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC indisponible", e);
        }
    }

    @Override
    public boolean matches(String secret, String hash) {
        if (secret == null || hash == null) {
            return false;
        }
        return MessageDigest.isEqual(hash(secret).getBytes(StandardCharsets.UTF_8),
                hash.getBytes(StandardCharsets.UTF_8));
    }
}
