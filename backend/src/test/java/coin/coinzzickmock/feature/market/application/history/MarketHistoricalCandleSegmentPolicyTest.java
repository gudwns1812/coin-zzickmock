package coin.coinzzickmock.feature.market.application.history;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class MarketHistoricalCandleSegmentPolicyTest {
    private final MarketHistoricalCandleSegmentPolicy segmentPolicy = new MarketHistoricalCandleSegmentPolicy();

    @Test
    void weeklySegmentsUseUtcMondayCalendarBoundaries() {
        MarketHistoricalCandleSegment segment = segmentPolicy.segmentContainingPreviousCandle(
                "BTCUSDT",
                MarketCandleInterval.ONE_WEEK,
                Instant.parse("2026-04-29T12:00:00Z")
        );

        assertThat(MarketTime.atStorageZone(segment.startInclusive()).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(MarketTime.atStorageZone(segment.startInclusive()).toLocalTime().toString()).isEqualTo("00:00");
        assertThat(ChronoUnit.WEEKS.between(
                MarketTime.atStorageZone(segment.startInclusive()),
                MarketTime.atStorageZone(segment.endExclusive())
        )).isEqualTo(200);
        assertThat(segment.granularity()).isEqualTo("1W");
    }

    @Test
    void monthlySegmentsUseUtcFirstDayCalendarBoundaries() {
        MarketHistoricalCandleSegment segment = segmentPolicy.segmentContainingPreviousCandle(
                "BTCUSDT",
                MarketCandleInterval.ONE_MONTH,
                Instant.parse("2026-04-29T12:00:00Z")
        );

        assertThat(MarketTime.atStorageZone(segment.startInclusive()).getDayOfMonth()).isEqualTo(1);
        assertThat(MarketTime.atStorageZone(segment.startInclusive()).toLocalTime().toString()).isEqualTo("00:00");
        assertThat(ChronoUnit.MONTHS.between(
                MarketTime.atStorageZone(segment.startInclusive()),
                MarketTime.atStorageZone(segment.endExclusive())
        )).isEqualTo(200);
        assertThat(segment.granularity()).isEqualTo("1M");
    }

    @Test
    void hourlySegmentsReuseProviderGranularityMapping() {
        MarketHistoricalCandleSegment segment = segmentPolicy.segmentContainingPreviousCandle(
                "BTCUSDT",
                MarketCandleInterval.ONE_HOUR,
                Instant.parse("2026-03-01T00:40:00Z")
        );

        assertThat(segment.granularity()).isEqualTo("1H");
    }
}
