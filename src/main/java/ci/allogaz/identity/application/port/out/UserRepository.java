package ci.allogaz.identity.application.port.out;

import java.util.Optional;
import java.util.UUID;

import ci.allogaz.identity.domain.PhoneNumber;
import ci.allogaz.identity.domain.User;

public interface UserRepository {

    Optional<User> findById(UUID id);

    Optional<User> findByPhone(PhoneNumber phone);

    User save(User user);
}
