package ci.allogaz.catalog.infrastructure.web;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ci.allogaz.catalog.application.CatalogQueryService;
import ci.allogaz.catalog.application.CatalogViews.CategoryView;
import ci.allogaz.catalog.application.CatalogViews.ProductView;
import ci.allogaz.catalog.application.SellerOfferService;
import ci.allogaz.catalog.application.SellerProfileService;
import ci.allogaz.catalog.domain.Product;
import ci.allogaz.catalog.domain.SellerProfile;
import ci.allogaz.catalog.domain.VerificationStatus;
import ci.allogaz.catalog.infrastructure.web.CatalogDtos.OfferResponse;
import ci.allogaz.catalog.infrastructure.web.CatalogDtos.PublicSellerResponse;
import ci.allogaz.catalog.infrastructure.web.CatalogDtos.SellerProfileResponse;
import ci.allogaz.shared.domain.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/catalog")
@Tag(name = "Catalogue", description = "Accès public")
public class CatalogController {

    private final CatalogQueryService catalog;
    private final SellerProfileService profiles;
    private final SellerOfferService offers;

    public CatalogController(CatalogQueryService catalog, SellerProfileService profiles, SellerOfferService offers) {
        this.catalog = catalog;
        this.profiles = profiles;
        this.offers = offers;
    }

    @GetMapping("/categories")
    public List<CategoryView> categories() {
        return catalog.categories();
    }

    @GetMapping("/products")
    @Operation(summary = "Produits actifs, filtrables par catégorie, marque, société de provenance, "
            + "couleur de bouteille et contenance (kg)")
    public List<ProductView> products(@RequestParam(required = false) String category,
            @RequestParam(required = false) String brand, @RequestParam(required = false) String company,
            @RequestParam(required = false) String color, @RequestParam(required = false) Double sizeKg) {
        return catalog.products(category, brand, company, color, sizeKg == null ? null : Product.kgToGrams(sizeKg));
    }

    @GetMapping("/sellers/{sellerId}")
    @Operation(summary = "Fiche publique d'un dépôt vérifié et ses offres actives")
    public PublicSellerResponse seller(@PathVariable UUID sellerId) {
        SellerProfile profile = profiles.get(sellerId);
        if (profile.status() != VerificationStatus.VERIFIED) {
            throw new NotFoundException("SELLER_NOT_FOUND", "Dépôt introuvable.");
        }
        List<OfferResponse> active = offers.ofSeller(sellerId).stream()
                .filter(d -> d.offer().active()).map(OfferResponse::from).toList();
        return new PublicSellerResponse(SellerProfileResponse.from(profile), profiles.canReceiveOrdersNow(profile),
                active);
    }
}
