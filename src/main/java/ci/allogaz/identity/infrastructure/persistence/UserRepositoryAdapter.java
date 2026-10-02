package ci.allogaz.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import ci.allogaz.identity.application.port.out.UserRepository;
import ci.allogaz.identity.domain.PhoneNumber;
import ci.allogaz.identity.domain.User;

@Repository
class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository jpa;

    UserRepositoryAdapter(SpringDataUserRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<User> findByPhone(PhoneNumber phone) {
        return jpa.findByPhone(phone.value()).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = jpa.findById(user.id()).orElseGet(() -> new UserJpaEntity(user.id()));
        entity.setPhone(user.phone().value());
        entity.setDisplayName(user.displayName());
        entity.setRoles(user.roles());
        entity.setCreatedAt(user.createdAt());
        return toDomain(jpa.save(entity));
    }

    private static User toDomain(UserJpaEntity e) {
        return new User(e.getId(), new PhoneNumber(e.getPhone()), e.getDisplayName(), e.getRoles(), e.getCreatedAt());
    }
}
