package ci.allogaz.payment.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ci.allogaz.payment.application.port.out.PaymentRepository;
import ci.allogaz.payment.domain.Payment;

interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByProviderReference(String reference);

    List<PaymentJpaEntity> findByOrderIdOrderByCreatedAt(UUID orderId);
}

@Repository
class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpa;

    PaymentRepositoryAdapter(PaymentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpa.findById(id).map(PaymentRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Payment> findByProviderReference(String reference) {
        return jpa.findByProviderReference(reference).map(PaymentRepositoryAdapter::toDomain);
    }

    @Override
    public List<Payment> findByOrderId(UUID orderId) {
        return jpa.findByOrderIdOrderByCreatedAt(orderId).stream().map(PaymentRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Payment save(Payment p) {
        PaymentJpaEntity e = jpa.findById(p.id()).orElseGet(PaymentJpaEntity::new);
        e.id = p.id();
        e.orderId = p.orderId();
        e.payerId = p.payerId();
        e.amount = p.amount();
        e.provider = p.provider();
        e.providerReference = p.providerReference();
        e.status = p.status();
        e.createdAt = p.createdAt();
        e.completedAt = p.completedAt();
        return toDomain(jpa.saveAndFlush(e));
    }

    private static Payment toDomain(PaymentJpaEntity e) {
        return new Payment(e.id, e.orderId, e.payerId, e.amount, e.provider, e.providerReference, e.status,
                e.createdAt, e.completedAt);
    }
}
