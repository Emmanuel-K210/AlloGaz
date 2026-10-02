package ci.allogaz.ordering.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ci.allogaz.ordering.application.OrderCommands.CreateOrder;
import ci.allogaz.ordering.application.port.out.BuyerNotifier;
import ci.allogaz.ordering.application.port.out.CatalogGateway;
import ci.allogaz.ordering.application.port.out.CatalogGateway.OfferInfo;
import ci.allogaz.ordering.application.port.out.CatalogGateway.SellerInfo;
import ci.allogaz.ordering.application.port.out.EscrowPort;
import ci.allogaz.ordering.application.port.out.OrderEventPublisher;
import ci.allogaz.ordering.application.port.out.OrderRepository;
import ci.allogaz.ordering.application.port.out.PaymentPort;
import ci.allogaz.ordering.application.port.out.SellerReputation;
import ci.allogaz.ordering.domain.DeliveryCodeResult;
import ci.allogaz.ordering.domain.DisputeOutcome;
import ci.allogaz.ordering.domain.Fulfillment;
import ci.allogaz.ordering.domain.Order;
import ci.allogaz.ordering.domain.OrderLine;
import ci.allogaz.ordering.domain.OrderStatus;
import ci.allogaz.ordering.domain.SaleType;
import ci.allogaz.shared.application.SecretHasher;
import ci.allogaz.shared.domain.ConflictException;
import ci.allogaz.shared.domain.DomainException;
import ci.allogaz.shared.domain.ForbiddenException;
import ci.allogaz.shared.domain.NotFoundException;

/** Cas d'usage de la commande, du brouillon à la libération des fonds. */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final CatalogGateway catalog;
    private final EscrowPort escrow;
    private final PaymentPort payments;
    private final BuyerNotifier notifier;
    private final OrderEventPublisher events;
    private final SellerReputation reputation;
    private final SecretHasher hasher;
    private final OrderingSettings settings;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public OrderService(OrderRepository orders, CatalogGateway catalog, EscrowPort escrow, PaymentPort payments,
            BuyerNotifier notifier, OrderEventPublisher events, SellerReputation reputation, SecretHasher hasher,
            OrderingSettings settings, TransactionTemplate transactions, Clock clock) {
        this.orders = orders;
        this.catalog = catalog;
        this.escrow = escrow;
        this.payments = payments;
        this.notifier = notifier;
        this.events = events;
        this.reputation = reputation;
        this.hasher = hasher;
        this.settings = settings;
        this.transactions = transactions;
        this.clock = clock;
    }

    // ================================================================ acheteur

    @Transactional
    public Order create(UUID buyerId, CreateOrder command, boolean submitNow) {
        SellerInfo seller = catalog.seller(command.sellerId());
        List<OrderLine> lines = new ArrayList<>();
        for (OrderCommands.Line line : command.lines()) {
            OfferInfo offer = catalog.offer(line.offerId());
            if (!offer.sellerId().equals(seller.id())) {
                throw new DomainException("OFFER_NOT_FROM_SELLER", "Cette offre n'appartient pas à ce dépôt.");
            }
            lines.add(new OrderLine(offer.id(), offer.productId(), offer.productName(), line.saleType(),
                    line.quantity(), availablePrice(offer, line.saleType(), line.quantity())));
        }
        long estimatedFee = 0;
        if (command.fulfillment() == Fulfillment.DELIVERY) {
            if (!seller.offersDelivery()) {
                throw new DomainException("DELIVERY_NOT_OFFERED", "Ce dépôt ne livre pas : choisissez le retrait sur place.");
            }
            if (command.deliveryLocation() != null
                    && seller.location().distanceMetersTo(command.deliveryLocation()) > seller.deliveryRadiusMeters()) {
                throw new DomainException("DELIVERY_OUT_OF_RANGE",
                        "Votre adresse est hors de la zone de livraison de ce dépôt : choisissez le retrait sur place.");
            }
            estimatedFee = seller.deliveryFee();
        }
        Order order = Order.draft(buyerId, seller.id(), seller.userId(), lines, command.fulfillment(),
                command.deliveryAddress(), command.deliveryLocation(), estimatedFee, clock.instant());
        if (submitNow) {
            submit(order, seller);
        }
        saveAndPublish(order);
        return order;
    }

    @Transactional
    public Order submit(UUID buyerId, UUID orderId) {
        Order order = forBuyer(buyerId, orderId);
        submit(order, catalog.seller(order.sellerId()));
        saveAndPublish(order);
        return order;
    }

    private void submit(Order order, SellerInfo seller) {
        if (!seller.canReceiveOrdersNow()) {
            throw new DomainException("SELLER_UNAVAILABLE", "Ce dépôt ne prend pas de commandes en ce moment.");
        }
        order.submit(clock.instant(), settings.sellerResponseTimeout());
    }

    /** Lance le paiement ; la commande passe à PAID à la confirmation (immédiate ou par notification). */
    public PaymentPort.PaymentStart pay(UUID buyerId, UUID orderId) {
        Order order = transactions.execute(s -> forBuyer(buyerId, orderId));
        if (order.status() != OrderStatus.ACCEPTED) {
            throw new DomainException("INVALID_ORDER_STATUS", "Seule une commande acceptée par le vendeur peut être payée.");
        }
        return payments.start(orderId, buyerId, notifier.phoneOf(buyerId), order.total());
    }

    @Transactional
    public String regenerateDeliveryCode(UUID buyerId, UUID orderId) {
        Order order = forBuyer(buyerId, orderId);
        String code = newDeliveryCode();
        order.replaceDeliveryCode(hashDeliveryCode(orderId, code));
        orders.save(order);
        notifier.sendDeliveryCode(buyerId, orderId, code);
        return code;
    }

    @Transactional
    public Order confirmReceipt(UUID buyerId, UUID orderId) {
        Order order = forBuyer(buyerId, orderId);
        order.confirmReceipt(clock.instant());
        releaseFunds(order);
        saveAndPublish(order);
        return order;
    }

    @Transactional
    public Order rate(UUID buyerId, UUID orderId, int stars) {
        Order order = forBuyer(buyerId, orderId);
        order.rate(stars);
        orders.save(order);
        reputation.recordRating(order.sellerId(), stars);
        return order;
    }

    // ================================================================ vendeur

    /** Acceptation : prix des produits et du transport recalculés puis figés. */
    @Transactional
    public Order accept(UUID sellerUserId, UUID orderId) {
        Order order = forSeller(sellerUserId, orderId);
        SellerInfo seller = catalog.seller(order.sellerId());
        List<OrderLine> priced = order.lines().stream()
                .map(l -> l.withUnitPrice(availablePrice(catalog.offer(l.offerId()), l.saleType(), l.quantity())))
                .toList();
        long fee = order.fulfillment() == Fulfillment.DELIVERY ? seller.deliveryFee() : 0;
        order.accept(priced, fee, clock.instant());
        saveAndPublish(order);
        reputation.recordDecision(order.sellerId(), true);
        return order;
    }

    @Transactional
    public Order reject(UUID sellerUserId, UUID orderId, String reason) {
        Order order = forSeller(sellerUserId, orderId);
        order.reject(reason, clock.instant());
        saveAndPublish(order);
        reputation.recordDecision(order.sellerId(), false);
        return order;
    }

    @Transactional
    public Order startPreparation(UUID sellerUserId, UUID orderId) {
        return sellerAction(sellerUserId, orderId, o -> o.startPreparation(clock.instant()));
    }

    @Transactional
    public Order dispatch(UUID sellerUserId, UUID orderId) {
        return sellerAction(sellerUserId, orderId, o -> o.dispatch(clock.instant()));
    }

    @Transactional
    public Order markDelivered(UUID sellerUserId, UUID orderId) {
        return sellerAction(sellerUserId, orderId,
                o -> o.markDelivered(clock.instant(), settings.autoValidationDelay()));
    }

    /**
     * Saisie du code donné par l'acheteur. Les erreurs sont enregistrées (la transaction est validée)
     * puis signalées par le résultat, pour que la limite d'essais ne soit pas contournable.
     */
    @Transactional
    public DeliveryCodeResult submitDeliveryCode(UUID sellerUserId, UUID orderId, String code) {
        Order order = forSeller(sellerUserId, orderId);
        DeliveryCodeResult result = order.submitDeliveryCode(code,
                hash -> hasher.matches(orderId + ":" + code, hash), settings.maxDeliveryCodeAttempts(),
                clock.instant());
        if (result.validated()) {
            releaseFunds(order);
        }
        saveAndPublish(order);
        return result;
    }

    // ================================================================ acheteur ou vendeur

    @Transactional
    public Order cancel(UUID userId, UUID orderId, String reason) {
        Order order = forParticipant(userId, orderId);
        Instant now = clock.instant();
        boolean refund = false;
        if (order.isBuyer(userId)) {
            order.cancelByBuyer(reasonOr(reason, "Annulée par l'acheteur."), now);
        } else {
            refund = order.cancelBySeller(reasonOr(reason, "Annulée par le vendeur."), now);
        }
        if (refund) {
            escrow.refundBuyer(orderId);
        }
        saveAndPublish(order);
        return order;
    }

    @Transactional
    public Order openDispute(UUID userId, UUID orderId, String reason) {
        Order order = forParticipant(userId, orderId);
        order.openDispute(reason, clock.instant());
        saveAndPublish(order);
        return order;
    }

    @Transactional(readOnly = true)
    public Order get(UUID userId, UUID orderId, boolean admin) {
        return admin ? load(orderId) : forParticipant(userId, orderId);
    }

    @Transactional(readOnly = true)
    public List<Order> buyerOrders(UUID buyerId) {
        return orders.findByBuyer(buyerId);
    }

    @Transactional(readOnly = true)
    public List<Order> sellerOrders(UUID sellerUserId, OrderStatus status) {
        return orders.findBySellerUser(sellerUserId, status);
    }

    // ================================================================ administration

    @Transactional(readOnly = true)
    public List<Order> disputes() {
        return orders.findByStatus(OrderStatus.DISPUTED);
    }

    @Transactional
    public Order resolveDispute(UUID orderId, DisputeOutcome outcome, String note) {
        Order order = load(orderId);
        if (order.status() != OrderStatus.DISPUTED) {
            throw new DomainException("INVALID_ORDER_STATUS", "Cette commande n'est pas en litige.");
        }
        if (outcome == DisputeOutcome.RELEASE_TO_SELLER) {
            escrow.releaseToSeller(orderId, order.sellerId());
        } else {
            escrow.refundBuyer(orderId);
        }
        order.resolveDispute(outcome, note, clock.instant());
        saveAndPublish(order);
        return order;
    }

    // ================================================================ paiement confirmé (module payment)

    /**
     * Appelé dans la transaction de confirmation du paiement : décrémente le stock (verrouillage optimiste,
     * rejoué par l'appelant en cas de conflit) et génère le code de livraison. Si la commande n'est plus
     * payable ou si le stock manque, l'acheteur est remboursé.
     */
    @Transactional
    public void onPaymentConfirmed(UUID orderId) {
        Order order = load(orderId);
        Instant now = clock.instant();
        if (order.status() != OrderStatus.ACCEPTED) {
            log.warn("Paiement reçu pour la commande {} au statut {} : remboursement", orderId, order.status());
            escrow.refundBuyer(orderId);
            return;
        }
        boolean inStock = order.lines().stream()
                .allMatch(l -> catalog.offer(l.offerId()).stock() >= l.quantity());
        if (!inStock) {
            order.markPaid(null, now);
            order.cancel("Stock épuisé chez le vendeur au moment du paiement : vous êtes remboursé.", now);
            escrow.refundBuyer(orderId);
            saveAndPublish(order);
            return;
        }
        order.lines().forEach(l -> catalog.decrementStock(l.offerId(), l.quantity()));
        String code = newDeliveryCode();
        order.markPaid(hashDeliveryCode(orderId, code), now);
        saveAndPublish(order);
        notifier.sendDeliveryCode(order.buyerId(), orderId, code);
    }

    // ================================================================ tâches planifiées

    /** Expire les intentions restées sans réponse ; chaque commande dans sa propre transaction. */
    public int expireOverdueIntents() {
        return forEachIsolated(orders.findOverdueIntents(clock.instant(), settings.batchSize()), id -> {
            Order order = load(id);
            order.expire(clock.instant());
            saveAndPublish(order);
            reputation.recordDecision(order.sellerId(), false);
        });
    }

    /** Valide les commandes livrées sans réaction de l'acheteur, puis libère les fonds. */
    public int autoValidateDeliveredOrders() {
        return forEachIsolated(orders.findDueForAutoValidation(clock.instant(), settings.batchSize()), id -> {
            Order order = load(id);
            order.autoValidate(clock.instant());
            releaseFunds(order);
            saveAndPublish(order);
        });
    }

    private int forEachIsolated(List<UUID> ids, Consumer<UUID> action) {
        int done = 0;
        for (UUID id : ids) {
            try {
                transactions.executeWithoutResult(s -> action.accept(id));
                done++;
            } catch (OptimisticLockingFailureException | DomainException e) {
                // Commande modifiée entre-temps (ex. acceptée à la dernière seconde) : on passe.
                log.info("Commande {} ignorée par la tâche planifiée : {}", id, e.getMessage());
            }
        }
        return done;
    }

    // ================================================================ outils

    private void releaseFunds(Order order) {
        escrow.releaseToSeller(order.id(), order.sellerId());
        order.markFundsReleased(clock.instant());
    }

    private Order sellerAction(UUID sellerUserId, UUID orderId, Consumer<Order> action) {
        Order order = forSeller(sellerUserId, orderId);
        action.accept(order);
        saveAndPublish(order);
        return order;
    }

    private void saveAndPublish(Order order) {
        orders.save(order);
        events.publish(order.pullChanges());
    }

    private static long availablePrice(OfferInfo offer, SaleType type, int quantity) {
        Long price = offer.priceFor(type);
        if (price == null) {
            throw new DomainException("OFFER_UNAVAILABLE", "Le produit « " + offer.productName()
                    + " » n'est plus proposé en " + (type == SaleType.REFILL
                    ? "recharge" : "achat") + " par ce dépôt.");
        }
        if (offer.stock() < quantity) {
            throw new ConflictException("INSUFFICIENT_STOCK",
                    "Stock insuffisant pour « " + offer.productName() + " » (disponible : " + offer.stock() + ").");
        }
        return price;
    }

    private String newDeliveryCode() {
        return String.format("%04d", random.nextInt(10_000));
    }

    private String hashDeliveryCode(UUID orderId, String code) {
        return hasher.hash(orderId + ":" + code);
    }

    private static String reasonOr(String reason, String fallback) {
        return reason == null || reason.isBlank() ? fallback : reason.strip();
    }

    private Order load(UUID orderId) {
        return orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Commande introuvable."));
    }

    /** Un non-participant reçoit 404 (et non 403) pour ne pas révéler l'existence de la commande. */
    private Order forParticipant(UUID userId, UUID orderId) {
        Order order = load(orderId);
        if (!order.isBuyer(userId) && !order.isSeller(userId)) {
            throw new NotFoundException("ORDER_NOT_FOUND", "Commande introuvable.");
        }
        return order;
    }

    private Order forBuyer(UUID buyerId, UUID orderId) {
        Order order = forParticipant(buyerId, orderId);
        if (!order.isBuyer(buyerId)) {
            throw new ForbiddenException("Action réservée à l'acheteur.");
        }
        return order;
    }

    private Order forSeller(UUID sellerUserId, UUID orderId) {
        Order order = forParticipant(sellerUserId, orderId);
        if (!order.isSeller(sellerUserId)) {
            throw new ForbiddenException("Action réservée au vendeur.");
        }
        return order;
    }
}
