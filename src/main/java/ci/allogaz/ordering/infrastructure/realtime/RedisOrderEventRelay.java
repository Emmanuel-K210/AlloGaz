package ci.allogaz.ordering.infrastructure.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import ci.allogaz.ordering.domain.StatusChange;
import tools.jackson.databind.json.JsonMapper;

/**
 * Relais Redis pub/sub : un changement validé sur une instance est publié sur un canal,
 * et chaque instance le pousse à ses clients SSE. Permet de lancer plusieurs instances de l'API.
 */
@Configuration
public class RedisOrderEventRelay {

    static final String CHANNEL = "allogaz:order-events";
    private static final Logger log = LoggerFactory.getLogger(RedisOrderEventRelay.class);

    private final StringRedisTemplate redis;
    private final JsonMapper json;

    public RedisOrderEventRelay(StringRedisTemplate redis, JsonMapper json) {
        this.redis = redis;
        this.json = json;
    }

    /** Reçoit les changements publiés après validation de la transaction. */
    @EventListener
    public void on(StatusChange change) {
        try {
            redis.convertAndSend(CHANNEL, json.writeValueAsString(OrderEventMessage.from(change)));
        } catch (RuntimeException e) {
            // Le temps réel est un confort : une panne Redis ne doit pas faire échouer la commande.
            log.warn("Diffusion temps réel impossible pour la commande {} : {}", change.orderId(), e.getMessage());
        }
    }

    @Bean
    RedisMessageListenerContainer orderEventsListenerContainer(RedisConnectionFactory connectionFactory,
            SseHub hub) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            try {
                hub.dispatch(json.readValue(message.getBody(), OrderEventMessage.class));
            } catch (RuntimeException e) {
                log.warn("Message temps réel illisible : {}", e.getMessage());
            }
        }, new ChannelTopic(CHANNEL));
        return container;
    }
}
