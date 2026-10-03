package ci.allogaz.catalog.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ci.allogaz.catalog.application.port.out.SellerProfileRepository;
import ci.allogaz.catalog.application.port.out.SellerRoleGranter;
import ci.allogaz.catalog.domain.DeliveryPolicy;
import ci.allogaz.catalog.domain.OpeningSlot;
import ci.allogaz.catalog.domain.SellerProfile;
import ci.allogaz.catalog.domain.VerificationStatus;
import ci.allogaz.shared.domain.ConflictException;
import ci.allogaz.shared.domain.GeoPoint;
import ci.allogaz.shared.domain.NotFoundException;

@Service
public class SellerProfileService {

    private final SellerProfileRepository profiles;
    private final SellerRoleGranter roleGranter;
    private final Clock clock;

    public SellerProfileService(SellerProfileRepository profiles, SellerRoleGranter roleGranter, Clock clock) {
        this.profiles = profiles;
        this.roleGranter = roleGranter;
        this.clock = clock;
    }

    public record SellerProfileCommand(String shopName, String address, GeoPoint location, int deliveryRadiusMeters,
                                       List<OpeningSlot> openingHours, DeliveryPolicy deliveryPolicy,
                                       boolean universalExchange) {
    }

    /** Un acheteur devient aussi vendeur ; le dépôt reste en attente de vérification. */
    @Transactional
    public SellerProfile apply(UUID userId, SellerProfileCommand c) {
        if (profiles.findByUserId(userId).isPresent()) {
            throw new ConflictException("SELLER_PROFILE_EXISTS", "Ce compte possède déjà un dépôt.");
        }
        SellerProfile profile = SellerProfile.apply(userId, c.shopName(), c.address(), c.location(),
                c.deliveryRadiusMeters(), c.openingHours(), c.deliveryPolicy(), c.universalExchange(), clock.instant());
        SellerProfile saved = profiles.save(profile);
        roleGranter.grantSellerRole(userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public SellerProfile mine(UUID userId) {
        return profiles.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("SELLER_PROFILE_NOT_FOUND", "Aucun dépôt pour ce compte."));
    }

    @Transactional(readOnly = true)
    public SellerProfile get(UUID sellerId) {
        return profiles.findById(sellerId)
                .orElseThrow(() -> new NotFoundException("SELLER_NOT_FOUND", "Dépôt introuvable."));
    }

    @Transactional
    public SellerProfile update(UUID userId, SellerProfileCommand c) {
        SellerProfile profile = mine(userId);
        profile.describe(c.shopName(), c.address(), c.location(), c.deliveryRadiusMeters(), c.openingHours(),
                c.deliveryPolicy(), c.universalExchange());
        return profiles.save(profile);
    }

    @Transactional
    public SellerProfile setAcceptingOrders(UUID userId, boolean accepting) {
        SellerProfile profile = mine(userId);
        if (accepting) {
            profile.open();
        } else {
            profile.pause();
        }
        return profiles.save(profile);
    }

    public boolean canReceiveOrdersNow(SellerProfile profile) {
        return profile.canReceiveOrders(LocalDateTime.now(clock));
    }

    // --- Administration

    @Transactional(readOnly = true)
    public List<SellerProfile> byStatus(VerificationStatus status) {
        return profiles.findByStatus(status);
    }

    @Transactional
    public SellerProfile verify(UUID sellerId) {
        SellerProfile profile = get(sellerId);
        profile.verify();
        return profiles.save(profile);
    }

    @Transactional
    public SellerProfile suspend(UUID sellerId) {
        SellerProfile profile = get(sellerId);
        profile.suspend();
        return profiles.save(profile);
    }
}
