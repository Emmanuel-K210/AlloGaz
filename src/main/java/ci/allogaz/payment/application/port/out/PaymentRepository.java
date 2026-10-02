package ci.allogaz.payment.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ci.allogaz.payment.domain.Payment;

public interface PaymentRepository {

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByProviderReference(String reference);

    List<Payment> findByOrderId(UUID orderId);

    Payment save(Payment payment);
}
