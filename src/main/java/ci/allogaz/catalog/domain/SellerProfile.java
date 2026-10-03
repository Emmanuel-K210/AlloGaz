package ci.allogaz.catalog.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import ci.allogaz.shared.domain.DomainException;
import ci.allogaz.shared.domain.GeoPoint;

/** Dépôt / point de vente. Visible des acheteurs seulement une fois vérifié. */
public class SellerProfile {

    private final UUID id;
    private final UUID userId;
    private String shopName;
    private String address;
    private GeoPoint location;
    private int deliveryRadiusMeters;
    private List<OpeningSlot> openingHours;
    private boolean acceptingOrders;
    private VerificationStatus status;
    private DeliveryPolicy deliveryPolicy;
    /** Point d'échange toutes marques : reprend une bouteille vide d'une autre société en échange. */
    private boolean universalExchange;
    private final Instant createdAt;

    public SellerProfile(UUID id, UUID userId, String shopName, String address, GeoPoint location,
            int deliveryRadiusMeters, List<OpeningSlot> openingHours, boolean acceptingOrders,
            VerificationStatus status, DeliveryPolicy deliveryPolicy, boolean universalExchange, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.createdAt = createdAt;
        this.acceptingOrders = acceptingOrders;
        this.status = status;
        describe(shopName, address, location, deliveryRadiusMeters, openingHours, deliveryPolicy, universalExchange);
    }

    public static SellerProfile apply(UUID userId, String shopName, String address, GeoPoint location,
            int deliveryRadiusMeters, List<OpeningSlot> openingHours, DeliveryPolicy deliveryPolicy,
            boolean universalExchange, Instant now) {
        return new SellerProfile(UUID.randomUUID(), userId, shopName, address, location, deliveryRadiusMeters,
                openingHours, false, VerificationStatus.PENDING, deliveryPolicy, universalExchange, now);
    }

    public final void describe(String shopName, String address, GeoPoint location, int deliveryRadiusMeters,
            List<OpeningSlot> openingHours, DeliveryPolicy deliveryPolicy, boolean universalExchange) {
        if (shopName == null || shopName.isBlank()) {
            throw new DomainException("INVALID_SELLER", "Le nom du dépôt est obligatoire.");
        }
        if (location == null) {
            throw new DomainException("INVALID_SELLER", "La position GPS du dépôt est obligatoire.");
        }
        if (deliveryRadiusMeters < 0 || deliveryRadiusMeters > 50_000) {
            throw new DomainException("INVALID_SELLER", "Le rayon de livraison doit être compris entre 0 et 50 km.");
        }
        this.shopName = shopName.strip();
        this.address = address;
        this.location = location;
        this.deliveryRadiusMeters = deliveryRadiusMeters;
        this.openingHours = List.copyOf(openingHours == null ? List.of() : openingHours);
        this.deliveryPolicy = deliveryPolicy;
        this.universalExchange = universalExchange;
    }

    public void verify() {
        status = VerificationStatus.VERIFIED;
    }

    public void suspend() {
        status = VerificationStatus.SUSPENDED;
        acceptingOrders = false;
    }

    public void open() {
        if (status != VerificationStatus.VERIFIED) {
            throw new DomainException("SELLER_NOT_VERIFIED", "Le dépôt doit être validé avant de recevoir des commandes.");
        }
        acceptingOrders = true;
    }

    public void pause() {
        acceptingOrders = false;
    }

    /** Sans horaires renseignés, le dépôt est considéré ouvert en continu. */
    public boolean isWithinOpeningHours(LocalDateTime at) {
        return openingHours.isEmpty() || openingHours.stream().anyMatch(slot -> slot.covers(at));
    }

    public boolean canReceiveOrders(LocalDateTime at) {
        return status == VerificationStatus.VERIFIED && acceptingOrders && isWithinOpeningHours(at);
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String shopName() {
        return shopName;
    }

    public String address() {
        return address;
    }

    public GeoPoint location() {
        return location;
    }

    public int deliveryRadiusMeters() {
        return deliveryRadiusMeters;
    }

    public List<OpeningSlot> openingHours() {
        return openingHours;
    }

    public boolean acceptingOrders() {
        return acceptingOrders;
    }

    public VerificationStatus status() {
        return status;
    }

    public DeliveryPolicy deliveryPolicy() {
        return deliveryPolicy;
    }

    public boolean universalExchange() {
        return universalExchange;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
