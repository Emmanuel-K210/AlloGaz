package ci.allogaz.identity.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/** Compte unique identifié par son téléphone ; un même compte peut être acheteur et vendeur. */
public class User {

    private final UUID id;
    private final PhoneNumber phone;
    private String displayName;
    private final Set<Role> roles;
    private final Instant createdAt;

    public User(UUID id, PhoneNumber phone, String displayName, Set<Role> roles, Instant createdAt) {
        this.id = id;
        this.phone = phone;
        this.displayName = displayName;
        this.roles = roles.isEmpty() ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(roles);
        this.createdAt = createdAt;
    }

    /** Tout nouvel inscrit est acheteur. */
    public static User register(PhoneNumber phone, Instant now) {
        return new User(UUID.randomUUID(), phone, null, EnumSet.of(Role.BUYER), now);
    }

    public void grant(Role role) {
        roles.add(role);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public void rename(String displayName) {
        this.displayName = displayName == null || displayName.isBlank() ? null : displayName.strip();
    }

    public UUID id() {
        return id;
    }

    public PhoneNumber phone() {
        return phone;
    }

    public String displayName() {
        return displayName;
    }

    public Set<Role> roles() {
        return Collections.unmodifiableSet(roles);
    }

    public Instant createdAt() {
        return createdAt;
    }
}
