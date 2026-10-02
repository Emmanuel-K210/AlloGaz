package ci.allogaz.catalog.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ci.allogaz.catalog.domain.VerificationStatus;

interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {

    Optional<CategoryJpaEntity> findBySlug(String slug);
}

interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {

    @Query("""
            select p from ProductJpaEntity p
            where p.active = true
              and (:categoryId is null or p.categoryId = :categoryId)
              and (:brand is null or lower(p.brand) = lower(cast(:brand as string)))
              and (:company is null or lower(p.company) = lower(cast(:company as string)))
              and (:bottleColor is null or lower(p.bottleColor) = lower(cast(:bottleColor as string)))
              and (:capacityGrams is null or p.capacityGrams = :capacityGrams)
            order by p.company, p.brand, p.capacityGrams
            """)
    List<ProductJpaEntity> search(UUID categoryId, String brand, String company, String bottleColor,
            Integer capacityGrams);
}

interface SellerProfileJpaRepository extends JpaRepository<SellerProfileJpaEntity, UUID> {

    Optional<SellerProfileJpaEntity> findByUserId(UUID userId);

    List<SellerProfileJpaEntity> findByStatusOrderByCreatedAt(VerificationStatus status);
}

interface SellerOfferJpaRepository extends JpaRepository<SellerOfferJpaEntity, UUID> {

    Optional<SellerOfferJpaEntity> findBySellerIdAndProductId(UUID sellerId, UUID productId);

    List<SellerOfferJpaEntity> findBySellerId(UUID sellerId);
}
