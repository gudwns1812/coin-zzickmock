package coin.coinzzickmock.feature.market.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class FundingScheduleTest {
    private final FundingSchedule schedule = FundingSchedule.defaultUsdtPerpetual();

    @Test
    void computesNextFundingBoundaryBeforeKstOne() {
        assertThat(schedule.nextFundingAt(Instant.parse("2026-04-26T15:59:59Z")))
                .isEqualTo(Instant.parse("2026-04-26T16:00:00Z"));
    }

    @Test
    void movesToNextBoundaryAtExactFundingTime() {
        assertThat(schedule.nextFundingAt(Instant.parse("2026-04-26T16:00:00Z")))
                .isEqualTo(Instant.parse("2026-04-27T00:00:00Z"));
    }

    @Test
    void computesMorningBoundary() {
        assertThat(schedule.nextFundingAt(Instant.parse("2026-04-26T23:59:59Z")))
                .isEqualTo(Instant.parse("2026-04-27T00:00:00Z"));
    }

    @Test
    void computesAfternoonBoundary() {
        assertThat(schedule.nextFundingAt(Instant.parse("2026-04-27T07:59:59Z")))
                .isEqualTo(Instant.parse("2026-04-27T08:00:00Z"));
    }

    @Test
    void wrapsToNextDayAfterKstSeventeen() {
        assertThat(schedule.nextFundingAt(Instant.parse("2026-04-27T08:00:00Z")))
                .isEqualTo(Instant.parse("2026-04-27T16:00:00Z"));
    }

    @Test
    void exposesSecondsUntilFunding() {
        assertThat(schedule.secondsUntilFunding(Instant.parse("2026-04-26T15:59:30Z")))
                .isEqualTo(30);
    }
}
