package ci.allogaz.identity.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ci.allogaz.identity.application.port.out.UserRepository;
import ci.allogaz.identity.domain.Role;
import ci.allogaz.identity.domain.User;
import ci.allogaz.shared.domain.NotFoundException;

@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public User get(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur introuvable."));
    }

    @Transactional
    public User rename(UUID userId, String displayName) {
        User user = get(userId);
        user.rename(displayName);
        return users.save(user);
    }

    @Transactional
    public User grantRole(UUID userId, Role role) {
        User user = get(userId);
        user.grant(role);
        return users.save(user);
    }
}
