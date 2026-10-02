package ci.allogaz.ordering.infrastructure.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.locationtech.jts.geom.Point;

import ci.allogaz.ordering.domain.DisputeOutcome;
import ci.allogaz.ordering.domain.Fulfillment;
import ci.allogaz.ordering.domain.OrderStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "orders")
class OrderJpaEntity {

    @Id
    UUID id;

    @Column(name = "buyer_id", nullable = false)
    UUID buyerId;

    @Column(name = "seller_id", nullable = false)
    UUID sellerId;

    @Column(name = "seller_user_id", nullable = false)
    UUID sellerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    OrderStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_lines", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "line_no")
    List<OrderLineEmbeddable> lines = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Fulfillment fulfillment;

    @Column(name = "delivery_address")
    String deliveryAddress;

    @Column(name = "delivery_location", columnDefinition = "geometry(Point,4326)")
    Point deliveryLocation;

    @Column(name = "items_total", nullable = false)
    long itemsTotal;

    @Column(name = "transport_fee", nullable = false)
    long transportFee;

    @Column(nullable = false)
    long total;

    @Column(name = "prices_frozen", nullable = false)
    boolean pricesFrozen;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "submitted_at")
    Instant submittedAt;

    @Column(name = "response_deadline")
    Instant responseDeadline;

    @Column(name = "accepted_at")
    Instant acceptedAt;

    @Column(name = "paid_at")
    Instant paidAt;

    @Column(name = "delivered_at")
    Instant deliveredAt;

    @Column(name = "auto_validate_at")
    Instant autoValidateAt;

    @Column(name = "validated_at")
    Instant validatedAt;

    @Column(name = "closed_at")
    Instant closedAt;

    @Column(name = "delivery_code_hash", length = 64)
    String deliveryCodeHash;

    @Column(name = "delivery_code_attempts", nullable = false)
    int deliveryCodeAttempts;

    @Column(name = "status_reason")
    String statusReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispute_outcome")
    DisputeOutcome disputeOutcome;

    @Column(name = "buyer_rating")
    Integer buyerRating;

    @Version
    long version;
}
