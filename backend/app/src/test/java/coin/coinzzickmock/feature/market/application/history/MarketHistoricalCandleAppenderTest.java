package coin.coinzzickmock.feature.market.application.history;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketHistoricalCandleAppenderTest {
    @Test
    void directHourlyEmptyPersistedSupplementUsesCurrentClosedBoundary() {
        RecordingHistoricalCandleCache cache = new RecordingHistoricalCandleCache();
        MarketHistoricalCandleAppender appender = new MarketHistoricalCandleAppender(
                cache,
                Clock.fixed(Instant.parse("2026-04-17T07:01:23Z"), ZoneOffset.UTC)
        );

        appender.appendOlderCandles("BTCUSDT", MarketCandleInterval.ONE_HOUR, null, 2, List.of());

        assertThat(cache.toExclusive).isEqualTo(Instant.parse("2026-04-17T07:00:00Z"));
    }

    @Test
    void directHourlyCursorSupplementUsesCursorBoundary() {
        RecordingHistoricalCandleCache cache = new RecordingHistoricalCandleCache();
        MarketHistoricalCandleAppender appender = new MarketHistoricalCandleAppender(
                cache,
                Clock.fixed(Instant.parse("2026-04-17T07:01:23Z"), ZoneOffset.UTC)
        );

        appender.appendOlderCandles(
                "BTCUSDT",
                MarketCandleInterval.ONE_HOUR,
                Instant.parse("2026-04-17T06:00:00Z"),
                2,
                List.of()
        );

        assertThat(cache.toExclusive).isEqualTo(Instant.parse("2026-04-17T06:00:00Z"));
    }

    @Test
    void directHourlyPersistedSupplementUsesOldestPersistedBoundary() {
        RecordingHistoricalCandleCache cache = new RecordingHistoricalCandleCache();
        MarketHistoricalCandleAppender appender = new MarketHistoricalCandleAppender(
                cache,
                Clock.fixed(Instant.parse("2026-04-17T07:01:23Z"), ZoneOffset.UTC)
        );

        appender.appendOlderCandles(
                "BTCUSDT",
                MarketCandleInterval.ONE_HOUR,
                null,
                2,
                List.of(hourlyResult("2026-04-17T06:00:00Z"))
        );

        assertThat(cache.toExclusive).isEqualTo(Instant.parse("2026-04-17T06:00:00Z"));
    }

    private static MarketCandleResult hourlyResult(String openTime) {
        Instant openInstant = Instant.parse(openTime);
        return new MarketCandleResult(
                openInstant,
                openInstant.plusSeconds(3600),
                100,
                101,
                99,
                100.5,
                10
        );
    }

    private static class RecordingHistoricalCandleCache extends MarketHistoricalCandleCache {
        private Instant toExclusive;

        private RecordingHistoricalCandleCache() {
            super(null, null, null);
        }

        @Override
        public List<MarketCandleResult> loadOlderCandles(
                String symbol,
                MarketCandleInterval interval,
                Instant toExclusive,
                int limit
        ) {
            this.toExclusive = toExclusive;
            List<MarketCandleResult> candles = new ArrayList<>();
            Instant cursor = toExclusive.minusSeconds(3600);
            while (candles.size() < limit) {
                candles.add(hourlyResult(cursor.toString()));
                cursor = cursor.minusSeconds(3600);
            }
            return candles;
        }
    }
}
