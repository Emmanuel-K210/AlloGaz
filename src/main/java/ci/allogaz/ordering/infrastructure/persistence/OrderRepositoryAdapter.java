package ci.allogaz.ordering.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import ci.allogaz.ordering.application.port.out.OrderRepository;
import ci.allogaz.ordering.domain.Order;
import ci.allogaz.ordering.domain.OrderLine;
import ci.allogaz.ordering.domain.OrderStatus;
import ci.allogaz.ordering.domain.StatusChange;
import ci.allogaz.shared.domain.GeoPoint;

interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    List<OrderJpaEntity> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId);

    List<OrderJpaEntity> findBySellerUserIdOrderByCreatedAtDesc(UUID sellerUserId);

    List<OrderJpaEntity> findBySellerUserIdAndStatusOrderByCreatedAtDesc(UUID sellerUserId, OrderStatus status);

    List<OrderJpaEntity> findByStatusOrderByCreatedAt(OrderStatus status);

    @Query("select o.id from OrderJpaEntity o where o.status = ci.allogaz.ordering.domain.OrderStatus.INTENT_SENT "
            + "and o.responseDeadline <= :now order by o.responseDeadline")
    List<UUID> findOverdueIntents(Instant now, Limit limit);

    @Query("select o.id from OrderJpaEntity o where o.status = ci.allogaz.ordering.domain.OrderStatus.DELIVERED "
            + "and o.autoValidateAt <= :now order by o.autoValidateAt")
    List<UUID> findDueForAutoValidation(Instant now, Limit limit);
}

@Repository
class OrderRepositoryAdapter implements OrderRepository {

    private static final GeometryFactory WGS84 = new GeometryFactory(new PrecisionModel(), 4326);

    private final OrderJpaRepository jpa;
    private final JdbcClient jdbc;

    OrderRepositoryAdapter(OrderJpaRepository jpa, JdbcClient jdbc) {
        this.jpa = jpa;
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpa.findById(id).map(OrderRepositoryAdapter::toDomain);
    }

    @Override
    public void save(Order o) {
        OrderJpaEntity e = jpa.findById(o.id()).orElse(null);
        if (e == null) {
            e = new OrderJpaEntity();
            e.id = o.id();
        } else if (e.version != o.version()) {
            throw new ObjectOptimisticLockingFailureException(OrderJpaEntity.class, o.id());
        }
        e.buyerId = o.buyerId();
        e.sellerId = o.sellerId();
        e.sellerUserId = o.sellerUserId();
        e.status = o.status();
        e.lines.clear();
        for (OrderLine l : o.lines()) {
            OrderLineEmbeddable line = new OrderLineEmbeddable();
            line.offerId = l.offerId();
            line.productId = l.productId();
            line.productName = l.productName();
            line.saleType = l.saleType();
            line.quantity = l.quantity();
            line.unitPrice = l.unitPrice();
            e.lines.add(line);
        }
        e.fulfillment = o.fulfillment();
        e.deliveryAddress = o.deliveryAddress();
        e.deliveryLocation = o.deliveryLocation() == null ? null
                : WGS84.createPoint(new Coordinate(o.deliveryLocation().longitude(), o.deliveryLocation().latitude()));
        e.itemsTotal = o.itemsTotal();
        e.transportFee = o.transportFee();
        e.total = o.total();
        e.pricesFrozen = o.pricesFrozen();
        e.createdAt = o.createdAt();
        e.submittedAt = o.submittedAt();
        e.responseDeadline = o.responseDeadline();
        e.acceptedAt = o.acceptedAt();
        e.paidAt = o.paidAt();
        e.deliveredAt = o.deliveredAt();
        e.autoValidateAt = o.autoValidateAt();
        e.validatedAt = o.validatedAt();
        e.closedAt = o.closedAt();
        e.deliveryCodeHash = o.deliveryCodeHash();
        e.deliveryCodeAttempts = o.deliveryCodeAttempts();
        e.statusReason = o.statusReason();
        e.disputeOutcome = o.disputeOutcome();
        e.buyerRating = o.buyerRating();
        OrderJpaEntity saved = jpa.saveAndFlush(e);
        o.assignVersion(saved.version);
        for (StatusChange c : o.pendingChanges()) {
            jdbc.sql("""
                    INSERT INTO order_status_history (order_id, from_status, to_status, changed_at)
                    VALUES (:orderId, :from, :to, :at)
                    """).param("orderId", c.orderId()).param("from", c.from().name()).param("to", c.to().name())
                    .param("at", Timestamp.from(c.at())).update();
        }
    }

    @Override
    public List<Order> findByBuyer(UUID buyerId) {
        return jpa.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream().map(OrderRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Order> findBySellerUser(UUID sellerUserId, OrderStatus status) {
        List<OrderJpaEntity> found = status == null ? jpa.findBySellerUserIdOrderByCreatedAtDesc(sellerUserId)
                : jpa.findBySellerUserIdAndStatusOrderByCreatedAtDesc(sellerUserId, status);
        return found.stream().map(OrderRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return jpa.findByStatusOrderByCreatedAt(status).stream().map(OrderRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<UUID> findOverdueIntents(Instant now, int limit) {
        return jpa.findOverdueIntents(now, Limit.of(limit));
    }

    @Override
    public List<UUID> findDueForAutoValidation(Instant now, int limit) {
        return jpa.findDueForAutoValidation(now, Limit.of(limit));
    }

    private static Order toDomain(OrderJpaEntity e) {
        List<OrderLine> lines = e.lines.stream().map(l -> new OrderLine(l.offerId, l.productId, l.productName,
                l.saleType, l.quantity, l.unitPrice)).toList();
        GeoPoint location = e.deliveryLocation == null ? null
                : new GeoPoint(e.deliveryLocation.getY(), e.deliveryLocation.getX());
        return new Order(e.id, e.buyerId, e.sellerId, e.sellerUserId, e.status, lines, e.fulfillment,
                e.deliveryAddress, location, e.transportFee, e.pricesFrozen, e.createdAt, e.submittedAt,
                e.responseDeadline, e.acceptedAt, e.paidAt, e.deliveredAt, e.autoValidateAt, e.validatedAt,
                e.closedAt, e.deliveryCodeHash, e.deliveryCodeAttempts, e.statusReason, e.disputeOutcome,
                e.buyerRating, e.version);
    }
}
