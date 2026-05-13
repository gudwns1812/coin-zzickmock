package coin.coinzzickmock.feature.account.application.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import org.springframework.stereotype.Component;

@Component
public class AccountRefillDatePolicy {
    private static final ZoneId REFILL_ZONE = ZoneId.of("Asia/Seoul");
    private final Clock clock;

    public AccountRefillDatePolicy() {
        this(Clock.systemUTC());
    }

    AccountRefillDatePolicy(Clock clock) {
        this.clock = clock;
    }

    public Instant now() {
        return Instant.now(clock);
    }

    public LocalDate currentRefillDate() {
        return today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public Instant nextResetAt() {
        return currentRefillDate()
                .plusWeeks(1)
                .atStartOfDay(REFILL_ZONE)
                .toInstant();
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(REFILL_ZONE));
    }
}
