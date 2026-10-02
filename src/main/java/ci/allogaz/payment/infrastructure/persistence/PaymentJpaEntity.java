package ci.allogaz.payment.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import ci.allogaz.payment.domain.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "payments")
class PaymentJpaEntity {

    @Id
    UUID id;

    @Column(name = "order_id", nullable = false)
    UUID orderId;

    @Column(name = "payer_id", nullable = false)
    UUID payerId;

    @Column(nullable = false)
    long amount;

    @Column(nullable = false)
    String provider;

    @Column(name = "provider_reference", unique = true)
    String providerReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "completed_at")
    Instant completedAt;

    @Version
    long version;
}
