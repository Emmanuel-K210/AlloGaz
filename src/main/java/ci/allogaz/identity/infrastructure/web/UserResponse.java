package ci.allogaz.identity.infrastructure.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ci.allogaz.identity.domain.User;

public record UserResponse(UUID id, String phone, String displayName, List<String> roles, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.phone().value(), user.displayName(),
                user.roles().stream().map(Enum::name).sorted().toList(), user.createdAt());
    }
}
