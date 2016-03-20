package buckerooo.library;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

import static java.time.Instant.now;
import static java.time.ZoneId.systemDefault;

class MoveableClock extends Clock {
    private Clock clock;

    public MoveableClock() {
        this.clock = Clock.fixed(now(), systemDefault());
    }

    @Override
    public ZoneId getZone() {
        return clock.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return clock.withZone(zone);
    }

    @Override
    public Instant instant() {
        return clock.instant();
    }

    public void moveForward(long amount, TemporalUnit temporalUnit) {
        Instant newInstant = clock.instant().plus(amount, temporalUnit);
        this.clock = Clock.fixed(newInstant, systemDefault());
    }
}
