package ci.allogaz.ordering.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ci.allogaz.ordering.domain.Order;
import ci.allogaz.ordering.domain.OrderStatus;

public interface OrderRepository {

    Optional<Order> findById(UUID id);

    /** Enregistre la commande et son historique de statuts (verrouillage optimiste sur la version). */
    void save(Order order);

    List<Order> findByBuyer(UUID buyerId);

    List<Order> findBySellerUser(UUID sellerUserId, OrderStatus status);

    List<Order> findByStatus(OrderStatus status);

    List<UUID> findOverdueIntents(Instant now, int limit);

    List<UUID> findDueForAutoValidation(Instant now, int limit);
}
