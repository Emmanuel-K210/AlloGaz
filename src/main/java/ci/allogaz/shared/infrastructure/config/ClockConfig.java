package ci.allogaz.shared.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ci.allogaz.AlloGazApplication;

@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(AlloGazApplication.ZONE));
    }
}
