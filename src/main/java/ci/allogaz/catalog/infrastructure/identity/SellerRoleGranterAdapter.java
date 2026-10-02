package ci.allogaz.catalog.infrastructure.identity;

import java.util.UUID;

import org.springframework.stereotype.Component;

import ci.allogaz.catalog.application.port.out.SellerRoleGranter;
import ci.allogaz.identity.application.UserService;
import ci.allogaz.identity.domain.Role;

@Component
class SellerRoleGranterAdapter implements SellerRoleGranter {

    private final UserService users;

    SellerRoleGranterAdapter(UserService users) {
        this.users = users;
    }

    @Override
    public void grantSellerRole(UUID userId) {
        users.grantRole(userId, Role.SELLER);
    }
}
