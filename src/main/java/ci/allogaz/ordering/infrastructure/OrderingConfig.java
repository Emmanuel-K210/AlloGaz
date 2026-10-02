package ci.allogaz.ordering.infrastructure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ci.allogaz.ordering.application.OrderingSettings;

@Configuration
public class OrderingConfig {

    @ConfigurationProperties(prefix = "allogaz.ordering")
    public record OrderingProperties(Duration sellerResponseTimeout, Duration autoValidationDelay,
                                     int maxDeliveryCodeAttempts, int batchSize) {
    }

    @Bean
    public OrderingSettings orderingSettings(OrderingProperties p) {
        return new OrderingSettings(p.sellerResponseTimeout(), p.autoValidationDelay(), p.maxDeliveryCodeAttempts(),
                p.batchSize());
    }
}
