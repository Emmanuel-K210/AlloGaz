package ci.allogaz.support;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import ci.allogaz.catalog.application.SellerOfferService;
import ci.allogaz.catalog.application.SellerProfileService;
import ci.allogaz.catalog.application.SellerProfileService.SellerProfileCommand;
import ci.allogaz.catalog.domain.DeliveryMode;
import ci.allogaz.catalog.domain.DeliveryPolicy;
import ci.allogaz.catalog.domain.SellerOffer;
import ci.allogaz.catalog.domain.SellerProfile;
import ci.allogaz.identity.application.port.out.UserRepository;
import ci.allogaz.identity.domain.PhoneNumber;
import ci.allogaz.identity.domain.Role;
import ci.allogaz.identity.domain.User;
import ci.allogaz.shared.domain.GeoPoint;

/** Jeux de données de test construits via les cas d'usage (pas de SQL à la main). */
public class Fixtures {

    /** Produits du référentiel (migration V3). */
    public static final UUID TOTAL_6KG = UUID.fromString("00000000-0000-0000-0001-000000000001");
    public static final UUID TOTAL_12KG = UUID.fromString("00000000-0000-0000-0001-000000000002");
    public static final UUID ORYX_6KG = UUID.fromString("00000000-0000-0000-0001-000000000003");
    public static final UUID ORYX_12KG = UUID.fromString("00000000-0000-0000-0001-000000000004");
    public static final UUID CORLAY_6KG = UUID.fromString("00000000-0000-0000-0001-000000000007");

    private static final AtomicLong PHONE_SEQUENCE = new AtomicLong(System.nanoTime() % 10_000_000L);

    private final UserRepository users;
    private final SellerProfileService profiles;
    private final SellerOfferService offers;

    public Fixtures(UserRepository users, SellerProfileService profiles, SellerOfferService offers) {
        this.users = users;
        this.profiles = profiles;
        this.offers = offers;
    }

    public static String uniquePhone() {
        return "+22505" + String.format("%08d", PHONE_SEQUENCE.incrementAndGet() % 100_000_000L);
    }

    public User user(Role... roles) {
        EnumSet<Role> set = EnumSet.of(Role.BUYER);
        set.addAll(List.of(roles));
        return users.save(new User(UUID.randomUUID(), new PhoneNumber(uniquePhone()), "Test", set, Instant.now()));
    }

    public record Seller(User user, SellerProfile profile) {

        public UUID id() {
            return profile.id();
        }
    }

    /** Dépôt vérifié, ouvert, sans restriction d'horaires. */
    public Seller verifiedSeller(String name, double lat, double lon, int radiusMeters, DeliveryPolicy policy) {
        User user = user();
        SellerProfile profile = profiles.apply(user.id(),
                new SellerProfileCommand(name, "Abidjan", new GeoPoint(lat, lon), radiusMeters, List.of(), policy, false));
        profiles.verify(profile.id());
        profile = profiles.setAcceptingOrders(user.id(), true);
        return new Seller(users.findById(user.id()).orElseThrow(), profile);
    }

    public Seller verifiedSeller(String name, double lat, double lon) {
        return verifiedSeller(name, lat, lon, 5_000, new DeliveryPolicy(DeliveryMode.FIXED_FEE, 500));
    }

    /** Dépôt vérifié, ouvert, qui reprend une bouteille vide de n'importe quelle société en échange. */
    public Seller universalExchangeSeller(String name, double lat, double lon, int radiusMeters, DeliveryPolicy policy) {
        User user = user();
        SellerProfile profile = profiles.apply(user.id(),
                new SellerProfileCommand(name, "Abidjan", new GeoPoint(lat, lon), radiusMeters, List.of(), policy, true));
        profiles.verify(profile.id());
        profile = profiles.setAcceptingOrders(user.id(), true);
        return new Seller(users.findById(user.id()).orElseThrow(), profile);
    }

    public SellerOffer offer(Seller seller, UUID productId, Long refillPrice, Long purchasePrice, int stock) {
        return offers.upsert(seller.user().id(), productId, refillPrice, purchasePrice, stock, true).offer();
    }
}
