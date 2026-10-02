package ci.allogaz.ordering.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Machine à états de la commande. Les transitions autorisées sont déclarées ici, en un seul endroit ;
 * toute autre transition est refusée par {@link Order}.
 *
 * <pre>
 * DRAFT ─► INTENT_SENT ─► ACCEPTED ─► PAID ─► IN_PREPARATION ─► OUT_FOR_DELIVERY ─► DELIVERED ─► VALIDATED ─► FUNDS_RELEASED
 *              │   │          │         │            │                 │                │
 *              │   ├► REJECTED │         ├────────────┴──► DISPUTED ◄───┴────────────────┘
 *              │   └► EXPIRED  │         │                    │
 *              └──────────────►┴─────────┴──► CANCELLED ◄─────┘ (remboursement)   DISPUTED ─► FUNDS_RELEASED
 * </pre>
 * Pour un retrait sur place, OUT_FOR_DELIVERY signifie « prête à être retirée ».
 */
public enum OrderStatus {
    DRAFT,
    INTENT_SENT,
    ACCEPTED,
    REJECTED,
    EXPIRED,
    PAID,
    IN_PREPARATION,
    OUT_FOR_DELIVERY,
    DELIVERED,
    VALIDATED,
    FUNDS_RELEASED,
    CANCELLED,
    DISPUTED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.ofEntries(
            Map.entry(DRAFT, EnumSet.of(INTENT_SENT, CANCELLED)),
            Map.entry(INTENT_SENT, EnumSet.of(ACCEPTED, REJECTED, EXPIRED, CANCELLED)),
            Map.entry(ACCEPTED, EnumSet.of(PAID, CANCELLED)),
            Map.entry(PAID, EnumSet.of(IN_PREPARATION, CANCELLED, DISPUTED)),
            Map.entry(IN_PREPARATION, EnumSet.of(OUT_FOR_DELIVERY, CANCELLED, DISPUTED)),
            Map.entry(OUT_FOR_DELIVERY, EnumSet.of(DELIVERED, DISPUTED)),
            Map.entry(DELIVERED, EnumSet.of(VALIDATED, DISPUTED)),
            Map.entry(VALIDATED, EnumSet.of(FUNDS_RELEASED)),
            Map.entry(DISPUTED, EnumSet.of(FUNDS_RELEASED, CANCELLED)),
            Map.entry(REJECTED, EnumSet.noneOf(OrderStatus.class)),
            Map.entry(EXPIRED, EnumSet.noneOf(OrderStatus.class)),
            Map.entry(CANCELLED, EnumSet.noneOf(OrderStatus.class)),
            Map.entry(FUNDS_RELEASED, EnumSet.noneOf(OrderStatus.class)));

    /** Statuts où l'argent de l'acheteur est en séquestre. */
    private static final Set<OrderStatus> FUNDS_IN_ESCROW =
            EnumSet.of(PAID, IN_PREPARATION, OUT_FOR_DELIVERY, DELIVERED, VALIDATED, DISPUTED);

    public boolean canTransitionTo(OrderStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    public Set<OrderStatus> allowedTargets() {
        return TRANSITIONS.get(this).isEmpty() ? EnumSet.noneOf(OrderStatus.class) : EnumSet.copyOf(TRANSITIONS.get(this));
    }

    public boolean isTerminal() {
        return TRANSITIONS.get(this).isEmpty();
    }

    public boolean holdsFundsInEscrow() {
        return FUNDS_IN_ESCROW.contains(this);
    }
}
