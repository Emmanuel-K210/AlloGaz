package ci.allogaz.shared.infrastructure.web;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

/** Extraction de l'utilisateur courant depuis le JWT (sujet = identifiant utilisateur). */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
