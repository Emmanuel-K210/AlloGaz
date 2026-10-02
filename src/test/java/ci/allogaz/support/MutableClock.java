package ci.allogaz.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

/** Horloge de test : suit l'heure réelle, décalée de l'avance demandée. */
public class MutableClock extends Clock {

    private final ZoneId zone;
    private final AtomicReference<Duration> offset = new AtomicReference<>(Duration.ZERO);

    public MutableClock(ZoneId zone) {
        this.zone = zone;
    }

    public void advance(Duration duration) {
        offset.updateAndGet(d -> d.plus(duration));
    }

    public void reset() {
        offset.set(Duration.ZERO);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.offset(Clock.system(zone), offset.get());
    }

    @Override
    public Instant instant() {
        return Instant.now().plus(offset.get());
    }
}
