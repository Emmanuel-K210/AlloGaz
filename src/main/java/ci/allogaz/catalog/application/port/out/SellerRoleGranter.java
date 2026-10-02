package ci.allogaz.catalog.application.port.out;

import java.util.UUID;

/** Donne le rôle vendeur au compte (module identity). */
public interface SellerRoleGranter {

    void grantSellerRole(UUID userId);
}
