package ci.allogaz.catalog.domain;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import ci.allogaz.shared.domain.DomainException;

public record OpeningSlot(DayOfWeek day, LocalTime opensAt, LocalTime closesAt) {

    public OpeningSlot {
        if (day == null || opensAt == null || closesAt == null || !opensAt.isBefore(closesAt)) {
            throw new DomainException("INVALID_OPENING_HOURS", "Créneau d'ouverture invalide.");
        }
    }

    public boolean covers(LocalDateTime at) {
        LocalTime time = at.toLocalTime();
        return at.getDayOfWeek() == day && !time.isBefore(opensAt) && time.isBefore(closesAt);
    }
}
