package ci.allogaz.geo.infrastructure.web;

import java.util.List;
import java.util.UUID;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ci.allogaz.geo.application.ProximitySearchService;
import ci.allogaz.geo.application.port.out.SellerProximityQuery.Criteria;
import ci.allogaz.geo.application.port.out.SellerProximityQuery.SaleType;
import ci.allogaz.geo.domain.RankedSeller;
import ci.allogaz.geo.domain.SellerCandidate.MatchingOffer;
import ci.allogaz.shared.domain.GeoPoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

@RestController
@Validated
@RequestMapping("/api/v1/search")
@Tag(name = "Recherche", description = "Dépôts autour de l'acheteur")
public class SearchController {

    private final ProximitySearchService search;

    public SearchController(ProximitySearchService search) {
        this.search = search;
    }

    @GetMapping("/sellers")
    @Operation(summary = "Dépôts vérifiés et ouverts, classés par score (distance, disponibilité, note, "
            + "taux d'acceptation). Les dépôts lointains restent listés mais plus bas.")
    public List<SellerResult> sellers(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double lon,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) Double sizeKg,
            @RequestParam(required = false) SaleType type,
            @RequestParam(defaultValue = "20") int limit) {
        Criteria criteria = new Criteria(productId, blankToNull(brand), blankToNull(company), blankToNull(color),
                sizeKg == null ? null : (int) Math.round(sizeKg * 1000), type);
        return search.search(new GeoPoint(lat, lon), criteria, limit).stream().map(SellerResult::from).toList();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    public record SellerResult(UUID sellerId, String shopName, String address, double latitude, double longitude,
                               long distanceMeters, boolean deliversToYou, String deliveryMode, long deliveryFee,
                               double rating, long ratingCount, double acceptanceRate, boolean available,
                               double score, List<MatchingOffer> offers) {

        static SellerResult from(RankedSeller r) {
            var s = r.seller();
            return new SellerResult(s.sellerId(), s.shopName(), s.address(), s.latitude(), s.longitude(),
                    Math.round(s.distanceMeters()), s.deliversTo(), s.deliveryMode(), s.deliveryFee(),
                    Math.round(r.averageRating() * 10) / 10.0, s.ratingCount(),
                    Math.round(r.acceptanceRate() * 100) / 100.0, s.hasStock(), r.score(), s.offers());
        }
    }
}
