package coin.coinzzickmock.feature.account.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

    public LocalDate today() {
        return LocalDate.now(clock.withZone(REFILL_ZONE));
    }

    public Instant nextResetAt() {
        return ZonedDateTime.now(clock.withZone(REFILL_ZONE))
                .toLocalDate()
                .plusDays(1)
                .atStartOfDay(REFILL_ZONE)
                .toInstant();
    }
}
