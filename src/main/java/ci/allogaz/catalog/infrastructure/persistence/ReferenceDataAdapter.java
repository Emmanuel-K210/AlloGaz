package ci.allogaz.catalog.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import ci.allogaz.catalog.application.port.out.CategoryRepository;
import ci.allogaz.catalog.application.port.out.ProductRepository;
import ci.allogaz.catalog.domain.Category;
import ci.allogaz.catalog.domain.Product;
import ci.allogaz.catalog.domain.ProductFilter;

@Repository
class ReferenceDataAdapter implements CategoryRepository, ProductRepository {

    private final CategoryJpaRepository categories;
    private final ProductJpaRepository products;

    ReferenceDataAdapter(CategoryJpaRepository categories, ProductJpaRepository products) {
        this.categories = categories;
        this.products = products;
    }

    @Override
    public List<Category> findAll() {
        return categories.findAll().stream().map(e -> new Category(e.id, e.slug, e.name)).toList();
    }

    @Override
    public Optional<Category> findBySlug(String slug) {
        return categories.findBySlug(slug).map(e -> new Category(e.id, e.slug, e.name));
    }

    @Override
    public List<Product> findActive(ProductFilter f) {
        return products.search(f.categoryId(), f.brand(), f.company(), f.bottleColor(), f.capacityGrams()).stream()
                .map(ReferenceDataAdapter::toDomain).toList();
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return products.findById(id).map(ReferenceDataAdapter::toDomain);
    }

    @Override
    public Product save(Product p) {
        ProductJpaEntity e = products.findById(p.id()).orElseGet(ProductJpaEntity::new);
        e.id = p.id();
        e.categoryId = p.categoryId();
        e.name = p.name();
        e.brand = p.brand();
        e.company = p.company();
        e.bottleColors = p.bottleColors().toArray(String[]::new);
        e.appearance = p.appearance();
        e.capacityGrams = p.capacityGrams();
        e.refillPrice = p.refillPrice();
        e.purchasePrice = p.purchasePrice();
        e.active = p.active();
        return toDomain(products.save(e));
    }

    private static Product toDomain(ProductJpaEntity e) {
        return new Product(e.id, e.categoryId, e.name, e.brand, e.company, List.of(e.bottleColors), e.appearance,
                e.capacityGrams, e.refillPrice, e.purchasePrice, e.active);
    }
}
