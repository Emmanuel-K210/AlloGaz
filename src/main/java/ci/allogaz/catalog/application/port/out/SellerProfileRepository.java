package ci.allogaz.catalog.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ci.allogaz.catalog.domain.SellerProfile;
import ci.allogaz.catalog.domain.VerificationStatus;

public interface SellerProfileRepository {

    Optional<SellerProfile> findById(UUID id);

    Optional<SellerProfile> findByUserId(UUID userId);

    List<SellerProfile> findByStatus(VerificationStatus status);

    SellerProfile save(SellerProfile profile);
}
