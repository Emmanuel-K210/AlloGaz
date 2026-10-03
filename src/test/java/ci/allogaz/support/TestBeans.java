package ci.allogaz.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class TestBeans {

    @Bean
    public Fixtures fixtures(ci.allogaz.identity.application.port.out.UserRepository users,
            ci.allogaz.catalog.application.SellerProfileService profiles,
            ci.allogaz.catalog.application.SellerOfferService offers,
            ci.allogaz.catalog.application.CatalogQueryService catalog) {
        return new Fixtures(users, profiles, offers, catalog);
    }

    @Bean
    @Primary
    public MutableClock mutableClock() {
        return new MutableClock(java.time.ZoneId.of("Africa/Abidjan"));
    }

    @Bean
    @Primary
    public RecordingSmsSender recordingSmsSender() {
        return new RecordingSmsSender();
    }
}
