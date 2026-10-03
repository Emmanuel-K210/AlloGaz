package ci.allogaz.catalog.infrastructure.persistence;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Repository;

import ci.allogaz.catalog.application.port.out.SellerProfileRepository;
import ci.allogaz.catalog.domain.DeliveryPolicy;
import ci.allogaz.catalog.domain.OpeningSlot;
import ci.allogaz.catalog.domain.SellerProfile;
import ci.allogaz.catalog.domain.VerificationStatus;
import ci.allogaz.shared.domain.GeoPoint;

@Repository
class SellerProfileRepositoryAdapter implements SellerProfileRepository {

    private static final GeometryFactory WGS84 = new GeometryFactory(new PrecisionModel(), 4326);

    private final SellerProfileJpaRepository jpa;

    SellerProfileRepositoryAdapter(SellerProfileJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<SellerProfile> findById(UUID id) {
        return jpa.findById(id).map(SellerProfileRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<SellerProfile> findByUserId(UUID userId) {
        return jpa.findByUserId(userId).map(SellerProfileRepositoryAdapter::toDomain);
    }

    @Override
    public List<SellerProfile> findByStatus(VerificationStatus status) {
        return jpa.findByStatusOrderByCreatedAt(status).stream().map(SellerProfileRepositoryAdapter::toDomain).toList();
    }

    @Override
    public SellerProfile save(SellerProfile p) {
        SellerProfileJpaEntity e = jpa.findById(p.id()).orElseGet(SellerProfileJpaEntity::new);
        e.id = p.id();
        e.userId = p.userId();
        e.shopName = p.shopName();
        e.address = p.address();
        e.location = WGS84.createPoint(new Coordinate(p.location().longitude(), p.location().latitude()));
        e.deliveryRadiusMeters = p.deliveryRadiusMeters();
        e.openingHours.clear();
        p.openingHours().forEach(slot -> {
            OpeningSlotEmbeddable s = new OpeningSlotEmbeddable();
            s.dayOfWeek = (short) slot.day().getValue();
            s.opensAt = slot.opensAt();
            s.closesAt = slot.closesAt();
            e.openingHours.add(s);
        });
        e.acceptingOrders = p.acceptingOrders();
        e.status = p.status();
        e.deliveryMode = p.deliveryPolicy().mode();
        e.deliveryFee = p.deliveryPolicy().fixedFee();
        e.universalExchange = p.universalExchange();
        e.createdAt = p.createdAt();
        return toDomain(jpa.save(e));
    }

    private static SellerProfile toDomain(SellerProfileJpaEntity e) {
        Point location = e.location;
        List<OpeningSlot> slots = e.openingHours.stream()
                .map(s -> new OpeningSlot(DayOfWeek.of(s.dayOfWeek), s.opensAt, s.closesAt)).toList();
        return new SellerProfile(e.id, e.userId, e.shopName, e.address, new GeoPoint(location.getY(), location.getX()),
                e.deliveryRadiusMeters, slots, e.acceptingOrders, e.status,
                new DeliveryPolicy(e.deliveryMode, e.deliveryFee), e.universalExchange, e.createdAt);
    }
}
