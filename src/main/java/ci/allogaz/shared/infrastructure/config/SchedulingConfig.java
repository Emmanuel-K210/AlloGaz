package ci.allogaz.shared.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Les tâches planifiées peuvent être coupées (tests, instances secondaires). */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "allogaz.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
