package ci.allogaz.catalog.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import ci.allogaz.catalog.application.port.out.SellerOfferRepository;
import ci.allogaz.catalog.domain.SellerOffer;

@Repository
class SellerOfferRepositoryAdapter implements SellerOfferRepository {

    private final SellerOfferJpaRepository jpa;

    SellerOfferRepositoryAdapter(SellerOfferJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<SellerOffer> findById(UUID id) {
        return jpa.findById(id).map(SellerOfferRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<SellerOffer> findBySellerAndProduct(UUID sellerId, UUID productId) {
        return jpa.findBySellerIdAndProductId(sellerId, productId).map(SellerOfferRepositoryAdapter::toDomain);
    }

    @Override
    public List<SellerOffer> findBySeller(UUID sellerId) {
        return jpa.findBySellerId(sellerId).stream().map(SellerOfferRepositoryAdapter::toDomain).toList();
    }

    /**
     * La version lue par le domaine doit correspondre à celle en base ; Hibernate ajoute ensuite
     * « where version = ? » à l'UPDATE, ce qui détecte aussi une écriture concurrente entre-temps.
     */
    @Override
    public SellerOffer save(SellerOffer o) {
        SellerOfferJpaEntity e = jpa.findById(o.id()).orElse(null);
        if (e == null) {
            e = new SellerOfferJpaEntity();
            e.id = o.id();
            e.sellerId = o.sellerId();
            e.productId = o.productId();
        } else if (e.version != o.version()) {
            throw new ObjectOptimisticLockingFailureException(SellerOfferJpaEntity.class, o.id());
        }
        e.stock = o.stock();
        e.active = o.active();
        return toDomain(jpa.saveAndFlush(e));
    }

    private static SellerOffer toDomain(SellerOfferJpaEntity e) {
        return new SellerOffer(e.id, e.sellerId, e.productId, e.stock, e.active, e.version);
    }
}
