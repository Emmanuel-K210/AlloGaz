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

    @Query(nativeQuery = true, value = """
            select * from products p
            where p.active
              and (cast(:categoryId as uuid) is null or p.category_id = cast(:categoryId as uuid))
              and (cast(:brand as text) is null or lower(p.brand) = lower(cast(:brand as text)))
              and (cast(:company as text) is null or lower(p.company) = lower(cast(:company as text)))
              and (cast(:bottleColor as text) is null or cast(:bottleColor as text) = any (p.bottle_colors))
              and (cast(:capacityGrams as integer) is null or p.capacity_grams = cast(:capacityGrams as integer))
            order by p.company, p.brand, p.capacity_grams
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
