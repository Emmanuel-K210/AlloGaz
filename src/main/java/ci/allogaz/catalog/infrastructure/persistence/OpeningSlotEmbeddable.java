package ci.allogaz.catalog.infrastructure.persistence;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
class OpeningSlotEmbeddable {

    /** 1 = lundi ... 7 = dimanche (ISO-8601). */
    @Column(name = "day_of_week", nullable = false)
    short dayOfWeek;

    @Column(name = "opens_at", nullable = false)
    LocalTime opensAt;

    @Column(name = "closes_at", nullable = false)
    LocalTime closesAt;
}
