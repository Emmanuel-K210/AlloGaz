package ci.allogaz.ordering.infrastructure.realtime;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Connexions SSE ouvertes sur cette instance, par commande et par utilisateur.
 * Les événements arrivent de Redis : chaque instance ne pousse qu'à ses propres clients.
 */
@Component
public class SseHub {

    static final Duration TIMEOUT = Duration.ofMinutes(30);

    private final Map<UUID, Set<SseEmitter>> byOrder = new ConcurrentHashMap<>();
    private final Map<UUID, Set<SseEmitter>> byUser = new ConcurrentHashMap<>();

    public SseEmitter subscribeOrder(UUID orderId, Object snapshot) {
        return register(byOrder, orderId, snapshot);
    }

    public SseEmitter subscribeUser(UUID userId) {
        return register(byUser, userId, null);
    }

    private SseEmitter register(Map<UUID, Set<SseEmitter>> index, UUID key, Object snapshot) {
        SseEmitter emitter = new SseEmitter(TIMEOUT.toMillis());
        Set<SseEmitter> emitters = index.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
        emitters.add(emitter);
        Runnable remove = () -> emitters.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        try {
            // Réessai côté navigateur après 3 s en cas de coupure (réseau mobile instable).
            emitter.send(SseEmitter.event().reconnectTime(3_000).comment("connecté"));
            if (snapshot != null) {
                emitter.send(SseEmitter.event().name("snapshot").data(snapshot, MediaType.APPLICATION_JSON));
            }
        } catch (IOException e) {
            remove.run();
        }
        return emitter;
    }

    /** Pousse un changement de statut aux abonnés de la commande, à l'acheteur et au vendeur. */
    public void dispatch(OrderEventMessage message) {
        send(byOrder.get(message.orderId()), message);
        send(byUser.get(message.buyerId()), message);
        send(byUser.get(message.sellerUserId()), message);
    }

    private void send(Set<SseEmitter> emitters, OrderEventMessage message) {
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(message.orderId() + ":" + message.at().toEpochMilli())
                        .name("order-status")
                        .data(message.payload(), MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
            }
        }
    }

    /** Commentaire périodique : garde la connexion ouverte à travers proxys et opérateurs mobiles. */
    @Scheduled(fixedRate = 25_000)
    void heartbeat() {
        for (Map<UUID, Set<SseEmitter>> index : List.of(byOrder, byUser)) {
            index.values().forEach(emitters -> emitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException | IllegalStateException e) {
                    emitters.remove(emitter);
                }
            }));
        }
        byOrder.entrySet().removeIf(e -> e.getValue().isEmpty());
        byUser.entrySet().removeIf(e -> e.getValue().isEmpty());
    }
}
