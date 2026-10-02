package ci.allogaz.geo.infrastructure.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ci.allogaz.geo.application.SearchSettings;
import ci.allogaz.geo.domain.RankingWeights;

@Configuration
public class GeoConfig {

    @ConfigurationProperties(prefix = "allogaz.search")
    public record SearchProperties(double maxRadiusMeters, int candidateLimit, RankingWeights weights) {
    }

    @Bean
    public SearchSettings searchSettings(SearchProperties p) {
        return new SearchSettings(p.maxRadiusMeters(), p.candidateLimit(),
                p.weights() == null ? RankingWeights.defaults() : p.weights());
    }
}
