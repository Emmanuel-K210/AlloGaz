package ci.allogaz.identity.infrastructure.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ci.allogaz.identity.application.port.out.SmsSender;
import ci.allogaz.identity.domain.PhoneNumber;

/** Implémentation de développement : le SMS est écrit dans les logs. */
@Component
class LoggingSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void send(PhoneNumber to, String message) {
        log.info("[SMS -> {}] {}", to.value(), message);
    }
}
