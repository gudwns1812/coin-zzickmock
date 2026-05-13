package coin.coinzzickmock.feature.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AccountRefillDatePolicyTest {
    @Test
    void currentRefillDateUsesPreviousMondayBeforeWeeklyReset() {
        AccountRefillDatePolicy policy = policyAt("2026-05-10T14:59:59Z");

        assertThat(policy.currentRefillDate()).isEqualTo(LocalDate.of(2026, 5, 4));
        assertThat(policy.nextResetAt()).isEqualTo(Instant.parse("2026-05-10T15:00:00Z"));
    }

    @Test
    void currentRefillDateUsesSameDayAtMondayWeeklyReset() {
        AccountRefillDatePolicy policy = policyAt("2026-05-10T15:00:00Z");

        assertThat(policy.currentRefillDate()).isEqualTo(LocalDate.of(2026, 5, 11));
        assertThat(policy.nextResetAt()).isEqualTo(Instant.parse("2026-05-17T15:00:00Z"));
    }

    private AccountRefillDatePolicy policyAt(String instant) {
        return new AccountRefillDatePolicy(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }
}
