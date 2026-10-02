package ci.allogaz.ordering.infrastructure.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ci.allogaz.ordering.application.OrderService;
import ci.allogaz.ordering.application.port.out.PaymentPort;
import ci.allogaz.ordering.infrastructure.web.OrderDtos.CreateOrderRequest;
import ci.allogaz.ordering.infrastructure.web.OrderDtos.DeliveryCodeResponse;
import ci.allogaz.ordering.infrastructure.web.OrderDtos.DisputeRequest;
import ci.allogaz.ordering.infrastructure.web.OrderDtos.OrderResponse;
import ci.allogaz.ordering.infrastructure.web.OrderDtos.RatingRequest;
import ci.allogaz.ordering.infrastructure.web.OrderDtos.ReasonRequest;
import ci.allogaz.shared.infrastructure.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Commandes (acheteur)")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée une commande (brouillon, prix indicatifs) ; submit=true l'envoie directement au vendeur")
    public OrderResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateOrderRequest r) {
        return OrderResponse.from(orders.create(CurrentUser.id(jwt), r.toCommand(), r.submit()));
    }

    @GetMapping
    public List<OrderResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return orders.buyerOrders(CurrentUser.id(jwt)).stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Détail d'une commande (acheteur, vendeur ou administrateur)")
    public OrderResponse get(@AuthenticationPrincipal Jwt jwt, Authentication auth, @PathVariable UUID orderId) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return OrderResponse.from(orders.get(CurrentUser.id(jwt), orderId, admin));
    }

    @PostMapping("/{orderId}/submit")
    @Operation(summary = "Envoie l'intention de commande au vendeur (délai de réponse configurable)")
    public OrderResponse submit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return OrderResponse.from(orders.submit(CurrentUser.id(jwt), orderId));
    }

    @PostMapping("/{orderId}/pay")
    @Operation(summary = "Paie une commande acceptée (Mobile Money) ; l'argent est placé en séquestre")
    public PaymentPort.PaymentStart pay(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return orders.pay(CurrentUser.id(jwt), orderId);
    }

    @PostMapping("/{orderId}/delivery-code")
    @Operation(summary = "Génère un nouveau code de livraison (envoyé aussi par SMS) ; l'ancien n'est plus valable")
    public DeliveryCodeResponse regenerateCode(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return new DeliveryCodeResponse(orders.regenerateDeliveryCode(CurrentUser.id(jwt), orderId));
    }

    @PostMapping("/{orderId}/confirm-receipt")
    @Operation(summary = "L'acheteur confirme la réception : la commande est validée et les fonds libérés")
    public OrderResponse confirmReceipt(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return OrderResponse.from(orders.confirmReceipt(CurrentUser.id(jwt), orderId));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Annule (acheteur : avant paiement ; vendeur : avant expédition, avec remboursement)")
    public OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId,
            @Valid @RequestBody(required = false) ReasonRequest r) {
        return OrderResponse.from(orders.cancel(CurrentUser.id(jwt), orderId, r == null ? null : r.reason()));
    }

    @PostMapping("/{orderId}/dispute")
    @Operation(summary = "Ouvre un litige (acheteur ou vendeur) : les fonds sont gelés")
    public OrderResponse dispute(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId,
            @Valid @RequestBody DisputeRequest r) {
        return OrderResponse.from(orders.openDispute(CurrentUser.id(jwt), orderId, r.reason()));
    }

    @PostMapping("/{orderId}/rating")
    @Operation(summary = "Note le vendeur (1 à 5) une fois la commande validée")
    public OrderResponse rate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId,
            @Valid @RequestBody RatingRequest r) {
        return OrderResponse.from(orders.rate(CurrentUser.id(jwt), orderId, r.stars()));
    }
}
