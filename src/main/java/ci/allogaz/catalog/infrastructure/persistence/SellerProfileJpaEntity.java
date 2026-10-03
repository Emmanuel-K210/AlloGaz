package ci.allogaz.catalog.infrastructure.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.locationtech.jts.geom.Point;

import ci.allogaz.catalog.domain.DeliveryMode;
import ci.allogaz.catalog.domain.VerificationStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "seller_profiles")
class SellerProfileJpaEntity {

    @Id
    UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    UUID userId;

    @Column(name = "shop_name", nullable = false)
    String shopName;

    String address;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    Point location;

    @Column(name = "delivery_radius_m", nullable = false)
    int deliveryRadiusMeters;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "seller_opening_hours", joinColumns = @JoinColumn(name = "seller_id"))
    List<OpeningSlotEmbeddable> openingHours = new ArrayList<>();

    @Column(name = "accepting_orders", nullable = false)
    boolean acceptingOrders;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    VerificationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false)
    DeliveryMode deliveryMode;

    @Column(name = "delivery_fee", nullable = false)
    long deliveryFee;

    @Column(name = "universal_exchange", nullable = false)
    boolean universalExchange;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Version
    long version;
}
