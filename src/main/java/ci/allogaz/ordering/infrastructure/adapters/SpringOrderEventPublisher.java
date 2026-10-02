package ci.allogaz.ordering.infrastructure.adapters;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import ci.allogaz.ordering.application.port.out.OrderEventPublisher;
import ci.allogaz.ordering.domain.StatusChange;

/** Publie chaque changement de statut une fois la transaction validée (jamais un état annulé). */
@Component
class SpringOrderEventPublisher implements OrderEventPublisher {

    private final ApplicationEventPublisher publisher;

    SpringOrderEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(List<StatusChange> changes) {
        if (changes.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    changes.forEach(publisher::publishEvent);
                }
            });
        } else {
            changes.forEach(publisher::publishEvent);
        }
    }
}
