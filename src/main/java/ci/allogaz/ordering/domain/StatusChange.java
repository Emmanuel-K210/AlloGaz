package ci.allogaz.ordering.domain;

import java.time.Instant;
import java.util.UUID;

/** Événement de domaine : la commande a changé de statut. */
public record StatusChange(UUID orderId, UUID buyerId, UUID sellerUserId, OrderStatus from, OrderStatus to,
                           Instant at) {
}
