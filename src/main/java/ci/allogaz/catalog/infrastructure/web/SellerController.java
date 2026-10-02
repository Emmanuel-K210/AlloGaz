package ci.allogaz.catalog.infrastructure.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ci.allogaz.catalog.application.SellerOfferService;
import ci.allogaz.catalog.application.SellerProfileService;
import ci.allogaz.catalog.infrastructure.web.CatalogDtos.OfferRequest;
import ci.allogaz.catalog.infrastructure.web.CatalogDtos.OfferResponse;
import ci.allogaz.catalog.infrastructure.web.CatalogDtos.SellerProfileRequest;
import ci.allogaz.catalog.infrastructure.web.CatalogDtos.SellerProfileResponse;
import ci.allogaz.shared.infrastructure.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/seller")
@Tag(name = "Espace vendeur")
public class SellerController {

    private final SellerProfileService profiles;
    private final SellerOfferService offers;

    public SellerController(SellerProfileService profiles, SellerOfferService offers) {
        this.profiles = profiles;
        this.offers = offers;
    }

    @PostMapping("/profile")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ouvre un dépôt (en attente de vérification) ; le compte reçoit le rôle SELLER. "
            + "Rafraîchir le jeton pour obtenir le nouveau rôle.")
    public SellerProfileResponse apply(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SellerProfileRequest r) {
        return SellerProfileResponse.from(profiles.apply(CurrentUser.id(jwt), r.toCommand()));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('SELLER')")
    public SellerProfileResponse profile(@AuthenticationPrincipal Jwt jwt) {
        return SellerProfileResponse.from(profiles.mine(CurrentUser.id(jwt)));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('SELLER')")
    public SellerProfileResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SellerProfileRequest r) {
        return SellerProfileResponse.from(profiles.update(CurrentUser.id(jwt), r.toCommand()));
    }

    @PostMapping("/profile/open")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Ouvre le dépôt aux commandes")
    public SellerProfileResponse open(@AuthenticationPrincipal Jwt jwt) {
        return SellerProfileResponse.from(profiles.setAcceptingOrders(CurrentUser.id(jwt), true));
    }

    @PostMapping("/profile/pause")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Met le dépôt en pause")
    public SellerProfileResponse pause(@AuthenticationPrincipal Jwt jwt) {
        return SellerProfileResponse.from(profiles.setAcceptingOrders(CurrentUser.id(jwt), false));
    }

    @GetMapping("/offers")
    @PreAuthorize("hasRole('SELLER')")
    public List<OfferResponse> myOffers(@AuthenticationPrincipal Jwt jwt) {
        return offers.mine(CurrentUser.id(jwt)).stream().map(OfferResponse::from).toList();
    }

    @PutMapping("/offers/{productId}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Crée ou met à jour l'offre du dépôt sur un produit (prix en F CFA, stock)")
    public OfferResponse upsertOffer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId,
            @Valid @RequestBody OfferRequest r) {
        return OfferResponse.from(offers.upsert(CurrentUser.id(jwt), productId, r.refillPrice(), r.purchasePrice(),
                r.stock(), r.active() == null || r.active()));
    }
}
