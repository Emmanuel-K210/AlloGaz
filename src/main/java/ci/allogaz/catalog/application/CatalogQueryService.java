package ci.allogaz.catalog.application;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ci.allogaz.catalog.application.CatalogViews.CategoryView;
import ci.allogaz.catalog.application.CatalogViews.ProductView;
import ci.allogaz.catalog.application.port.out.CategoryRepository;
import ci.allogaz.catalog.application.port.out.ProductRepository;
import ci.allogaz.catalog.domain.Product;
import ci.allogaz.catalog.domain.ProductFilter;
import ci.allogaz.shared.domain.DomainException;
import ci.allogaz.shared.domain.NotFoundException;

/** Référentiel produits : lu très souvent, modifié rarement, donc mis en cache (Redis). */
@Service
public class CatalogQueryService {

    public static final String CATEGORIES_CACHE = "catalog-categories";
    public static final String PRODUCTS_CACHE = "catalog-products";

    private final CategoryRepository categories;
    private final ProductRepository products;

    public CatalogQueryService(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    @Cacheable(CATEGORIES_CACHE)
    @Transactional(readOnly = true)
    public List<CategoryView> categories() {
        return categories.findAll().stream().map(CategoryView::from).toList();
    }

    /** Recherche par catégorie, marque, société de provenance, couleur de bouteille et contenance. */
    @Cacheable(cacheNames = PRODUCTS_CACHE,
            key = "{#categorySlug, #brand, #company, #bottleColor, #capacityGrams}")
    @Transactional(readOnly = true)
    public List<ProductView> products(String categorySlug, String brand, String company, String bottleColor,
            Integer capacityGrams) {
        UUID categoryId = categorySlug == null ? null : categories.findBySlug(categorySlug)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Catégorie introuvable.")).id();
        return products.findActive(new ProductFilter(categoryId, brand, company, Product.normalizeColor(bottleColor),
                        capacityGrams))
                .stream().map(ProductView::from).toList();
    }

    @Transactional(readOnly = true)
    public Product product(UUID productId) {
        return products.findById(productId)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Produit introuvable."));
    }

    @CacheEvict(cacheNames = PRODUCTS_CACHE, allEntries = true)
    @Transactional
    public ProductView createProduct(String categorySlug, String name, String brand, String company,
            List<String> bottleColors, String appearance, Integer capacityGrams, Long refillPrice,
            Long purchasePrice) {
        UUID categoryId = categories.findBySlug(categorySlug)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Catégorie introuvable.")).id();
        if (name == null || name.isBlank()) {
            throw new DomainException("INVALID_PRODUCT", "Le nom du produit est obligatoire.");
        }
        return ProductView.from(products.save(new Product(UUID.randomUUID(), categoryId, name.strip(), brand, company,
                bottleColors, appearance, capacityGrams, refillPrice, purchasePrice, true)));
    }

    /**
     * Met à jour le tarif national d'un produit (recharge et/ou achat), par exemple quand le syndicat des
     * gaziers annonce un nouveau prix officiel. Ni le vendeur ni l'acheteur ne peuvent l'influencer :
     * seul un administrateur AlloGaz passe par ici.
     */
    @CacheEvict(cacheNames = PRODUCTS_CACHE, allEntries = true)
    @Transactional
    public ProductView updateProductPrice(UUID productId, Long refillPrice, Long purchasePrice) {
        Product current = product(productId);
        Product updated = new Product(current.id(), current.categoryId(), current.name(), current.brand(),
                current.company(), current.bottleColors(), current.appearance(), current.capacityGrams(), refillPrice,
                purchasePrice, current.active());
        return ProductView.from(products.save(updated));
    }
}
