package ci.allogaz.ordering.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import ci.allogaz.shared.domain.DomainException;
import ci.allogaz.shared.domain.GeoPoint;

/**
 * Agrégat commande. Chaque changement de statut passe par {@link #transitionTo} qui applique la table
 * de {@link OrderStatus} et enregistre un événement {@link StatusChange}. Montants en F CFA entiers.
 */
public class Order {

    private final UUID id;
    private final UUID buyerId;
    private final UUID sellerId;
    private final UUID sellerUserId;
    private OrderStatus status;
    private List<OrderLine> lines;
    private final Fulfillment fulfillment;
    private final String deliveryAddress;
    private final GeoPoint deliveryLocation;
    private long transportFee;
    private boolean pricesFrozen;
    private final Instant createdAt;
    private Instant submittedAt;
    private Instant responseDeadline;
    private Instant acceptedAt;
    private Instant paidAt;
    private Instant deliveredAt;
    private Instant autoValidateAt;
    private Instant validatedAt;
    private Instant closedAt;
    private String deliveryCodeHash;
    private int deliveryCodeAttempts;
    private String statusReason;
    private DisputeOutcome disputeOutcome;
    private Integer buyerRating;
    private long version;

    private final transient List<StatusChange> changes = new ArrayList<>();

    /** Reconstitution depuis la persistance. */
    public Order(UUID id, UUID buyerId, UUID sellerId, UUID sellerUserId, OrderStatus status, List<OrderLine> lines,
            Fulfillment fulfillment, String deliveryAddress, GeoPoint deliveryLocation, long transportFee,
            boolean pricesFrozen, Instant createdAt, Instant submittedAt, Instant responseDeadline, Instant acceptedAt,
            Instant paidAt, Instant deliveredAt, Instant autoValidateAt, Instant validatedAt, Instant closedAt,
            String deliveryCodeHash, int deliveryCodeAttempts, String statusReason, DisputeOutcome disputeOutcome,
            Integer buyerRating, long version) {
        this.id = id;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.sellerUserId = sellerUserId;
        this.status = status;
        this.lines = List.copyOf(lines);
        this.fulfillment = fulfillment;
        this.deliveryAddress = deliveryAddress;
        this.deliveryLocation = deliveryLocation;
        this.transportFee = transportFee;
        this.pricesFrozen = pricesFrozen;
        this.createdAt = createdAt;
        this.submittedAt = submittedAt;
        this.responseDeadline = responseDeadline;
        this.acceptedAt = acceptedAt;
        this.paidAt = paidAt;
        this.deliveredAt = deliveredAt;
        this.autoValidateAt = autoValidateAt;
        this.validatedAt = validatedAt;
        this.closedAt = closedAt;
        this.deliveryCodeHash = deliveryCodeHash;
        this.deliveryCodeAttempts = deliveryCodeAttempts;
        this.statusReason = statusReason;
        this.disputeOutcome = disputeOutcome;
        this.buyerRating = buyerRating;
        this.version = version;
    }

    /** Brouillon : prix indicatifs, transport estimé. */
    public static Order draft(UUID buyerId, UUID sellerId, UUID sellerUserId, List<OrderLine> lines,
            Fulfillment fulfillment, String deliveryAddress, GeoPoint deliveryLocation, long estimatedTransportFee,
            Instant now) {
        if (buyerId.equals(sellerUserId)) {
            throw new DomainException("SELF_ORDER", "Vous ne pouvez pas commander dans votre propre dépôt.");
        }
        if (lines == null || lines.isEmpty()) {
            throw new DomainException("EMPTY_ORDER", "La commande doit contenir au moins un produit.");
        }
        if (lines.stream().map(l -> l.offerId() + ":" + l.saleType()).distinct().count() != lines.size()) {
            throw new DomainException("DUPLICATE_LINE", "Un même produit apparaît deux fois dans la commande.");
        }
        if (fulfillment == Fulfillment.DELIVERY && deliveryLocation == null) {
            throw new DomainException("DELIVERY_LOCATION_REQUIRED", "La position de livraison est obligatoire.");
        }
        return new Order(UUID.randomUUID(), buyerId, sellerId, sellerUserId, OrderStatus.DRAFT, lines, fulfillment,
                deliveryAddress, deliveryLocation, estimatedTransportFee, false, now, null, null, null, null, null,
                null, null, null, null, 0, null, null, null, 0);
    }

    // ---------------------------------------------------------------- cycle de vie

    /** Envoie l'intention de commande au vendeur, qui a {@code responseTimeout} pour répondre. */
    public void submit(Instant now, Duration responseTimeout) {
        transitionTo(OrderStatus.INTENT_SENT, now);
        submittedAt = now;
        responseDeadline = now.plus(responseTimeout);
    }

    /** Acceptation : les prix (produits et transport) sont figés à ce moment. */
    public void accept(List<OrderLine> currentPrices, long finalTransportFee, Instant now) {
        requireStatus(OrderStatus.INTENT_SENT);
        if (isResponseOverdue(now)) {
            throw new DomainException("ORDER_EXPIRED", "Le délai de réponse est dépassé : la commande a expiré.");
        }
        if (currentPrices.size() != lines.size()) {
            throw new IllegalArgumentException("Lignes de prix incohérentes");
        }
        if (finalTransportFee < 0) {
            throw new DomainException("INVALID_PRICE", "Frais de transport invalides.");
        }
        lines = List.copyOf(currentPrices);
        transportFee = fulfillment == Fulfillment.PICKUP ? 0 : finalTransportFee;
        pricesFrozen = true;
        acceptedAt = now;
        transitionTo(OrderStatus.ACCEPTED, now);
    }

    public void reject(String reason, Instant now) {
        transitionTo(OrderStatus.REJECTED, now);
        statusReason = reason;
        closedAt = now;
    }

    public boolean isResponseOverdue(Instant now) {
        return status == OrderStatus.INTENT_SENT && responseDeadline != null && !now.isBefore(responseDeadline);
    }

    public void expire(Instant now) {
        if (!isResponseOverdue(now)) {
            throw new DomainException("NOT_EXPIRED", "Le délai de réponse du vendeur n'est pas écoulé.");
        }
        transitionTo(OrderStatus.EXPIRED, now);
        statusReason = "Pas de réponse du vendeur dans le délai imparti.";
        closedAt = now;
    }

    /** Le paiement est en séquestre ; le code de livraison (haché) est attaché à la commande. */
    public void markPaid(String deliveryCodeHash, Instant now) {
        transitionTo(OrderStatus.PAID, now);
        paidAt = now;
        replaceDeliveryCode(deliveryCodeHash);
    }

    /** Nouveau code (perdu par l'acheteur) ; le compteur d'essais n'est pas remis à zéro. */
    public void replaceDeliveryCode(String hash) {
        if (!status.holdsFundsInEscrow() || status == OrderStatus.VALIDATED || status == OrderStatus.DISPUTED) {
            throw new DomainException("INVALID_ORDER_STATUS", "Le code de livraison n'est plus utilisable.");
        }
        deliveryCodeHash = hash;
    }

    public void startPreparation(Instant now) {
        transitionTo(OrderStatus.IN_PREPARATION, now);
    }

    public void dispatch(Instant now) {
        transitionTo(OrderStatus.OUT_FOR_DELIVERY, now);
    }

    /** Le vendeur déclare la livraison sans code : l'acheteur a {@code autoValidationDelay} pour réagir. */
    public void markDelivered(Instant now, Duration autoValidationDelay) {
        transitionTo(OrderStatus.DELIVERED, now);
        deliveredAt = now;
        autoValidateAt = now.plus(autoValidationDelay);
    }

    /**
     * Le vendeur saisit le code donné par l'acheteur à la remise. Le bon code valide la commande ;
     * au-delà de {@code maxAttempts} erreurs, le code est bloqué (l'acheteur peut encore confirmer dans l'application).
     */
    public DeliveryCodeResult submitDeliveryCode(String code, Predicate<String> matchesHash, int maxAttempts,
            Instant now) {
        if (status != OrderStatus.OUT_FOR_DELIVERY && status != OrderStatus.DELIVERED) {
            throw invalidStatus();
        }
        if (deliveryCodeAttempts >= maxAttempts) {
            return new DeliveryCodeResult(DeliveryCodeResult.Outcome.LOCKED, 0);
        }
        if (code != null && code.matches("\\d{4}") && matchesHash.test(deliveryCodeHash)) {
            if (status == OrderStatus.OUT_FOR_DELIVERY) {
                transitionTo(OrderStatus.DELIVERED, now);
                deliveredAt = now;
            }
            validate(now);
            return new DeliveryCodeResult(DeliveryCodeResult.Outcome.VALIDATED, maxAttempts - deliveryCodeAttempts);
        }
        deliveryCodeAttempts++;
        int remaining = maxAttempts - deliveryCodeAttempts;
        return new DeliveryCodeResult(
                remaining <= 0 ? DeliveryCodeResult.Outcome.LOCKED : DeliveryCodeResult.Outcome.WRONG_CODE, remaining);
    }

    /** L'acheteur confirme la réception depuis l'application. */
    public void confirmReceipt(Instant now) {
        if (status == OrderStatus.OUT_FOR_DELIVERY) {
            transitionTo(OrderStatus.DELIVERED, now);
            deliveredAt = now;
        }
        validate(now);
    }

    public boolean isDueForAutoValidation(Instant now) {
        return status == OrderStatus.DELIVERED && autoValidateAt != null && !now.isBefore(autoValidateAt);
    }

    /** Validation automatique faute de réaction de l'acheteur après la livraison. */
    public void autoValidate(Instant now) {
        if (!isDueForAutoValidation(now)) {
            throw new DomainException("NOT_DUE", "La validation automatique n'est pas encore due.");
        }
        validate(now);
    }

    private void validate(Instant now) {
        transitionTo(OrderStatus.VALIDATED, now);
        validatedAt = now;
        deliveryCodeHash = null;
    }

    public void markFundsReleased(Instant now) {
        transitionTo(OrderStatus.FUNDS_RELEASED, now);
        closedAt = now;
    }

    /** Litige : les fonds restent gelés en séquestre jusqu'à la décision d'un administrateur. */
    public void openDispute(String reason, Instant now) {
        if (reason == null || reason.isBlank()) {
            throw new DomainException("REASON_REQUIRED", "Merci de décrire le problème.");
        }
        transitionTo(OrderStatus.DISPUTED, now);
        statusReason = reason.strip();
    }

    public void resolveDispute(DisputeOutcome outcome, String note, Instant now) {
        requireStatus(OrderStatus.DISPUTED);
        disputeOutcome = outcome;
        statusReason = note;
        if (outcome == DisputeOutcome.RELEASE_TO_SELLER) {
            markFundsReleased(now);
        } else {
            transitionTo(OrderStatus.CANCELLED, now);
            closedAt = now;
        }
    }

    /** Annulation par l'acheteur, possible tant qu'il n'a pas payé. */
    public void cancelByBuyer(String reason, Instant now) {
        if (status != OrderStatus.DRAFT && status != OrderStatus.INTENT_SENT && status != OrderStatus.ACCEPTED) {
            throw new DomainException("CANNOT_CANCEL",
                    "La commande est déjà payée : ouvrez un litige si le vendeur ne livre pas.");
        }
        cancel(reason, now);
    }

    /** Annulation par le vendeur (après acceptation, avant expédition) ; renvoie vrai s'il faut rembourser. */
    public boolean cancelBySeller(String reason, Instant now) {
        if (status != OrderStatus.ACCEPTED && status != OrderStatus.PAID && status != OrderStatus.IN_PREPARATION) {
            throw invalidStatus();
        }
        return cancel(reason, now);
    }

    /** Annulation système (ex. stock épuisé au moment du paiement) ; renvoie vrai s'il faut rembourser. */
    public boolean cancel(String reason, Instant now) {
        boolean refund = status.holdsFundsInEscrow();
        transitionTo(OrderStatus.CANCELLED, now);
        statusReason = reason;
        closedAt = now;
        return refund;
    }

    public void rate(int stars) {
        if (status != OrderStatus.VALIDATED && status != OrderStatus.FUNDS_RELEASED) {
            throw new DomainException("CANNOT_RATE", "Vous pourrez noter le vendeur une fois la commande validée.");
        }
        if (buyerRating != null) {
            throw new DomainException("ALREADY_RATED", "Vous avez déjà noté cette commande.");
        }
        if (stars < 1 || stars > 5) {
            throw new DomainException("INVALID_RATING", "La note doit être comprise entre 1 et 5.");
        }
        buyerRating = stars;
    }

    // ---------------------------------------------------------------- mécanique

    private void transitionTo(OrderStatus target, Instant now) {
        if (!status.canTransitionTo(target)) {
            throw invalidStatus();
        }
        changes.add(new StatusChange(id, buyerId, sellerUserId, status, target, now));
        status = target;
    }

    private void requireStatus(OrderStatus expected) {
        if (status != expected) {
            throw invalidStatus();
        }
    }

    private DomainException invalidStatus() {
        return new DomainException("INVALID_ORDER_STATUS",
                "Action impossible : la commande est au statut " + status + ".");
    }

    /** Événements non encore publiés ; la liste est vidée. */
    public List<StatusChange> pullChanges() {
        List<StatusChange> copy = List.copyOf(changes);
        changes.clear();
        return copy;
    }

    public List<StatusChange> pendingChanges() {
        return List.copyOf(changes);
    }

    /** Point d'accroche de la persistance : version après enregistrement (verrouillage optimiste). */
    public void assignVersion(long version) {
        this.version = version;
    }

    public long itemsTotal() {
        return lines.stream().mapToLong(OrderLine::total).sum();
    }

    public long total() {
        return itemsTotal() + transportFee;
    }

    public boolean isBuyer(UUID userId) {
        return buyerId.equals(userId);
    }

    public boolean isSeller(UUID userId) {
        return sellerUserId.equals(userId);
    }

    // ---------------------------------------------------------------- accesseurs

    public UUID id() { return id; }
    public UUID buyerId() { return buyerId; }
    public UUID sellerId() { return sellerId; }
    public UUID sellerUserId() { return sellerUserId; }
    public OrderStatus status() { return status; }
    public List<OrderLine> lines() { return lines; }
    public Fulfillment fulfillment() { return fulfillment; }
    public String deliveryAddress() { return deliveryAddress; }
    public GeoPoint deliveryLocation() { return deliveryLocation; }
    public long transportFee() { return transportFee; }
    public boolean pricesFrozen() { return pricesFrozen; }
    public Instant createdAt() { return createdAt; }
    public Instant submittedAt() { return submittedAt; }
    public Instant responseDeadline() { return responseDeadline; }
    public Instant acceptedAt() { return acceptedAt; }
    public Instant paidAt() { return paidAt; }
    public Instant deliveredAt() { return deliveredAt; }
    public Instant autoValidateAt() { return autoValidateAt; }
    public Instant validatedAt() { return validatedAt; }
    public Instant closedAt() { return closedAt; }
    public String deliveryCodeHash() { return deliveryCodeHash; }
    public int deliveryCodeAttempts() { return deliveryCodeAttempts; }
    public String statusReason() { return statusReason; }
    public DisputeOutcome disputeOutcome() { return disputeOutcome; }
    public Integer buyerRating() { return buyerRating; }
    public long version() { return version; }
}
