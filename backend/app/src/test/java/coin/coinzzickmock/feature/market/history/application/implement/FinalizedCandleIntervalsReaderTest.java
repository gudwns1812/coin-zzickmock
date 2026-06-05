package coin.coinzzickmock.feature.market.history.application.implement;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.domain.CompletedMarketCandle;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FinalizedCandleIntervalsReaderTest {
    @Test
    void returnsMinuteDerivedIntervalsWhenSymbolHistoryIsNotAvailable() {
        FinalizedCandleIntervalsReader reader = new FinalizedCandleIntervalsReader(
                new RecordingMarketHistoryRepository(Map.of())
        );

        List<MarketCandleInterval> intervals = reader.readAffectedIntervals(
                "BTCUSDT",
                Instant.parse("2026-04-30T04:00:00Z"),
                Instant.parse("2026-04-30T04:01:00Z")
        );

        assertThat(intervals).containsExactly(
                MarketCandleInterval.ONE_MINUTE,
                MarketCandleInterval.THREE_MINUTES,
                MarketCandleInterval.FIVE_MINUTES,
                MarketCandleInterval.FIFTEEN_MINUTES
        );
    }

    @Test
    void includesHourlyDerivedIntervalsOnlyWhenTheirBucketsAreComplete() {
        RecordingMarketHistoryRepository repository = new RecordingMarketHistoryRepository(Map.of("BTCUSDT", 1L));
        repository.completedHourlyCandles.add(hourly(Instant.parse("2026-04-30T04:00:00Z")));
        repository.completedHourlyCandles.add(hourly(Instant.parse("2026-04-30T05:00:00Z")));
        repository.completedHourlyCandles.add(hourly(Instant.parse("2026-04-30T06:00:00Z")));
        repository.completedHourlyCandles.add(hourly(Instant.parse("2026-04-30T07:00:00Z")));
        FinalizedCandleIntervalsReader reader = new FinalizedCandleIntervalsReader(repository);

        List<MarketCandleInterval> intervals = reader.readAffectedIntervals(
                "BTCUSDT",
                Instant.parse("2026-04-30T07:59:00Z"),
                Instant.parse("2026-04-30T08:00:00Z")
        );

        assertThat(intervals).containsExactly(
                MarketCandleInterval.ONE_MINUTE,
                MarketCandleInterval.THREE_MINUTES,
                MarketCandleInterval.FIVE_MINUTES,
                MarketCandleInterval.FIFTEEN_MINUTES,
                MarketCandleInterval.ONE_HOUR,
                MarketCandleInterval.FOUR_HOURS
        );
    }

    @Test
    void calendarIntervalsRequirePersistedCompletedCalendarRows() {
        RecordingMarketHistoryRepository repository = new RecordingMarketHistoryRepository(Map.of("BTCUSDT", 1L));
        Instant dayStart = Instant.parse("2026-04-30T00:00:00Z");
        for (int hour = 0; hour < 24; hour++) {
            repository.completedHourlyCandles.add(hourly(dayStart.plusSeconds(hour * 3600L)));
        }
        FinalizedCandleIntervalsReader reader = new FinalizedCandleIntervalsReader(repository);

        List<MarketCandleInterval> intervalsWithoutPersistedDay = reader.readAffectedIntervals(
                "BTCUSDT",
                Instant.parse("2026-04-30T23:59:00Z"),
                Instant.parse("2026-05-01T00:00:00Z")
        );

        assertThat(intervalsWithoutPersistedDay).doesNotContain(MarketCandleInterval.ONE_DAY);

        repository.completedCandles.add(completed(MarketCandleInterval.ONE_DAY, dayStart, dayStart.plusSeconds(86_400)));
        List<MarketCandleInterval> intervalsWithPersistedDay = reader.readAffectedIntervals(
                "BTCUSDT",
                Instant.parse("2026-04-30T23:59:00Z"),
                Instant.parse("2026-05-01T00:00:00Z")
        );

        assertThat(intervalsWithPersistedDay).contains(MarketCandleInterval.ONE_DAY);
    }

    private static HourlyMarketCandle hourly(Instant openTime) {
        return new HourlyMarketCandle(
                1L,
                openTime,
                openTime.plusSeconds(3600),
                100,
                101,
                99,
                100.5,
                10,
                1005,
                openTime,
                openTime.plusSeconds(3600)
        );
    }

    private static CompletedMarketCandle completed(
            MarketCandleInterval interval,
            Instant openTime,
            Instant closeTime
    ) {
        return new CompletedMarketCandle(
                1L,
                interval,
                openTime,
                closeTime,
                100,
                101,
                99,
                100.5,
                10,
                1005
        );
    }

    private static class RecordingMarketHistoryRepository extends coin.coinzzickmock.testsupport.TestMarketHistoryRepository {
        private final Map<String, Long> symbolIds;
        private final List<HourlyMarketCandle> completedHourlyCandles = new ArrayList<>();
        private final List<CompletedMarketCandle> completedCandles = new ArrayList<>();

        private RecordingMarketHistoryRepository(Map<String, Long> symbolIds) {
            this.symbolIds = symbolIds;
        }

        @Override
        public Map<String, Long> findSymbolIdsBySymbols(List<String> symbols) {
            return symbolIds;
        }

        @Override
        public List<HourlyMarketCandle> findCompletedHourlyCandles(
                long symbolId,
                Instant fromInclusive,
                Instant toExclusive
        ) {
            return completedHourlyCandles.stream()
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .toList();
        }

        @Override
        public List<CompletedMarketCandle> findCompletedCandles(
                long symbolId,
                MarketCandleInterval interval,
                Instant fromInclusive,
                Instant toExclusive
        ) {
            return completedCandles.stream()
                    .filter(candle -> candle.interval() == interval)
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .toList();
        }
    }
}
