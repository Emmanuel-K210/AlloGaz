package ci.allogaz.ordering.infrastructure.realtime;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ci.allogaz.ordering.application.OrderService;
import ci.allogaz.ordering.domain.Order;
import ci.allogaz.shared.infrastructure.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Suivi en temps réel par Server-Sent Events. Depuis le navigateur :
 * {@code new EventSource('/api/v1/orders/{id}/events?access_token=...')} (EventSource n'envoie pas d'en-tête).
 */
@RestController
@Tag(name = "Temps réel", description = "Server-Sent Events : événements « snapshot » et « order-status »")
public class OrderEventsController {

    private final OrderService orders;
    private final SseHub hub;

    public OrderEventsController(OrderService orders, SseHub hub) {
        this.orders = orders;
        this.hub = hub;
    }

    @GetMapping(path = "/api/v1/orders/{orderId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Flux des changements de statut d'une commande (acheteur ou vendeur)")
    public SseEmitter orderEvents(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        Order order = orders.get(CurrentUser.id(jwt), orderId, false);
        return hub.subscribeOrder(orderId, new OrderEventMessage.Payload(order.id(), null, order.status().name(),
                null));
    }

    @GetMapping(path = "/api/v1/me/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Flux de toutes mes commandes (le vendeur y reçoit les nouvelles intentions)")
    public SseEmitter myEvents(@AuthenticationPrincipal Jwt jwt) {
        return hub.subscribeUser(CurrentUser.id(jwt));
    }
}
