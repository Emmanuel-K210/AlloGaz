package ci.allogaz.ordering.infrastructure.web;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ci.allogaz.ordering.application.OrderService;
import ci.allogaz.ordering.domain.DeliveryCodeResult;
import ci.allogaz.ordering.domain.OrderStatus;
import ci.allogaz.ordering.infrastructure.web.OrderDtos.DeliveryCodeRequest;
import ci.allogaz.ordering.infrastructure.web.OrderDtos.OrderResponse;
import ci.allogaz.ordering.infrastructure.web.OrderDtos.ReasonRequest;
import ci.allogaz.shared.domain.DomainException;
import ci.allogaz.shared.domain.TooManyRequestsException;
import ci.allogaz.shared.infrastructure.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/seller/orders")
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Commandes (vendeur)")
public class SellerOrderController {

    private final OrderService orders;

    public SellerOrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    public List<OrderResponse> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) OrderStatus status) {
        return orders.sellerOrders(CurrentUser.id(jwt), status).stream().map(OrderResponse::from).toList();
    }

    @PostMapping("/{orderId}/accept")
    @Operation(summary = "Accepte l'intention : le prix final (produits + transport) est figé")
    public OrderResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return OrderResponse.from(orders.accept(CurrentUser.id(jwt), orderId));
    }

    @PostMapping("/{orderId}/reject")
    public OrderResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId,
            @Valid @RequestBody(required = false) ReasonRequest r) {
        return OrderResponse.from(orders.reject(CurrentUser.id(jwt), orderId, r == null ? null : r.reason()));
    }

    @PostMapping("/{orderId}/prepare")
    public OrderResponse prepare(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return OrderResponse.from(orders.startPreparation(CurrentUser.id(jwt), orderId));
    }

    @PostMapping("/{orderId}/dispatch")
    @Operation(summary = "En livraison (ou prête à être retirée pour un retrait sur place)")
    public OrderResponse dispatch(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return OrderResponse.from(orders.dispatch(CurrentUser.id(jwt), orderId));
    }

    @PostMapping("/{orderId}/delivered")
    @Operation(summary = "Déclare la livraison sans code : validation automatique si l'acheteur ne réagit pas")
    public OrderResponse delivered(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return OrderResponse.from(orders.markDelivered(CurrentUser.id(jwt), orderId));
    }

    @PostMapping("/{orderId}/delivery-code")
    @Operation(summary = "Saisit le code à 4 chiffres donné par l'acheteur : le bon code valide la commande "
            + "et libère les fonds")
    public OrderResponse submitCode(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId,
            @Valid @RequestBody DeliveryCodeRequest r) {
        UUID sellerUserId = CurrentUser.id(jwt);
        DeliveryCodeResult result = orders.submitDeliveryCode(sellerUserId, orderId, r.code());
        return switch (result.outcome()) {
            case VALIDATED -> OrderResponse.from(orders.get(sellerUserId, orderId, false));
            case WRONG_CODE -> throw new DomainException("WRONG_DELIVERY_CODE",
                    "Code incorrect. Essais restants : " + result.remainingAttempts() + ".");
            case LOCKED -> throw new TooManyRequestsException("DELIVERY_CODE_LOCKED",
                    "Trop d'essais : le code est bloqué. L'acheteur doit confirmer la réception dans l'application.");
        };
    }
}
