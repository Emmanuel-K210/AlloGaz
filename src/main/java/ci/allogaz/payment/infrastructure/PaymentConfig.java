package ci.allogaz.payment.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ci.allogaz.payment.domain.Commission;

@Configuration
public class PaymentConfig {

    @Bean
    public Commission commission(@Value("${allogaz.payment.commission-rate-bps}") int rateBasisPoints) {
        return new Commission(rateBasisPoints);
    }
}
