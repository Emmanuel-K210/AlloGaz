package ci.allogaz.shared.application;

/** Hachage des secrets courts (codes OTP, codes de livraison) : jamais stockés en clair. */
public interface SecretHasher {

    String hash(String secret);

    boolean matches(String secret, String hash);
}
