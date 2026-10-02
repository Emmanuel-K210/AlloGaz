package ci.allogaz.ordering.infrastructure.realtime;

import java.time.Instant;
import java.util.UUID;

import ci.allogaz.ordering.domain.StatusChange;

/** Message diffusé aux clients (SSE) et entre instances (Redis pub/sub). */
public record OrderEventMessage(UUID orderId, UUID buyerId, UUID sellerUserId, String from, String to, Instant at) {

    static OrderEventMessage from(StatusChange c) {
        return new OrderEventMessage(c.orderId(), c.buyerId(), c.sellerUserId(), c.from().name(), c.to().name(),
                c.at());
    }

    /** Charge utile envoyée au navigateur (sans les identifiants des participants). */
    public Payload payload() {
        return new Payload(orderId, from, to, at);
    }

    public record Payload(UUID orderId, String from, String to, Instant at) {
    }
}
