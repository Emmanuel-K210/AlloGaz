package ci.allogaz.ordering.application.port.out;

import java.util.List;

import ci.allogaz.ordering.domain.StatusChange;

/** Diffusion des changements de statut (suivi en temps réel), après validation de la transaction. */
public interface OrderEventPublisher {

    void publish(List<StatusChange> changes);
}
