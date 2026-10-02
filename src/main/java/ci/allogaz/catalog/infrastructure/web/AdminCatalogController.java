package ci.allogaz.catalog.infrastructure.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ci.allogaz.catalog.application.CatalogQueryService;
import ci.allogaz.catalog.application.CatalogViews.ProductView;
import ci.allogaz.catalog.application.SellerProfileService;
import ci.allogaz.catalog.domain.VerificationStatus;
import ci.allogaz.catalog.infrastructure.web.CatalogDtos.ProductRequest;
import ci.allogaz.catalog.infrastructure.web.CatalogDtos.SellerProfileResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administration - catalogue")
public class AdminCatalogController {

    private final SellerProfileService profiles;
    private final CatalogQueryService catalog;

    public AdminCatalogController(SellerProfileService profiles, CatalogQueryService catalog) {
        this.profiles = profiles;
        this.catalog = catalog;
    }

    @GetMapping("/sellers")
    public List<SellerProfileResponse> sellers(@RequestParam(defaultValue = "PENDING") VerificationStatus status) {
        return profiles.byStatus(status).stream().map(SellerProfileResponse::from).toList();
    }

    @PostMapping("/sellers/{sellerId}/verify")
    public SellerProfileResponse verify(@PathVariable UUID sellerId) {
        return SellerProfileResponse.from(profiles.verify(sellerId));
    }

    @PostMapping("/sellers/{sellerId}/suspend")
    public SellerProfileResponse suspend(@PathVariable UUID sellerId) {
        return SellerProfileResponse.from(profiles.suspend(sellerId));
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductView createProduct(@Valid @RequestBody ProductRequest r) {
        return catalog.createProduct(r.categorySlug(), r.name(), r.brand(), r.company(), r.bottleColors(), r.appearance(),
                r.capacityGrams());
    }
}
