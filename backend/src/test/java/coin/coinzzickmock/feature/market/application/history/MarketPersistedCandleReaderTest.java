package coin.coinzzickmock.feature.market.application.history;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MarketPersistedCandleReaderTest {
    @Test
    void directHourlyReadUsesCompletedHourlyRowsOnly() {
        TrackingMarketHistoryRepository repository = new TrackingMarketHistoryRepository();
        Instant completedHour = Instant.parse("2026-04-17T06:00:00Z");
        repository.completedHourlyCandles.add(hourly(completedHour));
        repository.latestCompletedHourlyOpenTime = completedHour;
        MarketPersistedCandleReader reader = new MarketPersistedCandleReader(
                repository,
                new MarketCandleRollupProjector()
        );

        List<MarketCandleResult> results = reader.read(1L, MarketCandleInterval.ONE_HOUR, 1, null);

        assertThat(results).singleElement()
                .extracting(MarketCandleResult::openTime)
                .isEqualTo(completedHour);
        assertThat(repository.completedHourlyRangeCalls).isEqualTo(1);
        assertThat(repository.rawHourlyRangeCalls).isZero();
    }

    @Test
    void higherHourlyRollupReadsCompletedHourlyRowsOnly() {
        TrackingMarketHistoryRepository repository = new TrackingMarketHistoryRepository();
        Instant bucketStart = Instant.parse("2026-04-17T04:00:00Z");
        for (int hour = 0; hour < 4; hour++) {
            repository.completedHourlyCandles.add(hourly(bucketStart.plusSeconds(hour * 3600L)));
        }
        repository.latestCompletedHourlyOpenTime = bucketStart.plusSeconds(3 * 3600L);
        MarketPersistedCandleReader reader = new MarketPersistedCandleReader(
                repository,
                new MarketCandleRollupProjector()
        );

        List<MarketCandleResult> results = reader.read(1L, MarketCandleInterval.FOUR_HOURS, 1, null);

        assertThat(results).singleElement()
                .extracting(MarketCandleResult::openTime)
                .isEqualTo(bucketStart);
        assertThat(repository.completedHourlyRangeCalls).isEqualTo(1);
        assertThat(repository.rawHourlyRangeCalls).isZero();
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

    private static class TrackingMarketHistoryRepository implements MarketHistoryRepository {
        private final List<HourlyMarketCandle> completedHourlyCandles = new ArrayList<>();
        private Instant latestCompletedHourlyOpenTime;
        private int completedHourlyRangeCalls;
        private int rawHourlyRangeCalls;

        @Override
        public Map<String, Long> findSymbolIdsBySymbols(List<String> symbols) {
            return Map.of();
        }

        @Override
        public List<StartupBackfillCursor> findStartupBackfillCursors() {
            return List.of();
        }

        @Override
        public Optional<Instant> findLatestMinuteCandleOpenTime(long symbolId) {
            return Optional.empty();
        }

        @Override
        public Optional<Instant> findLatestMinuteCandleOpenTimeBefore(long symbolId, Instant beforeExclusive) {
            return Optional.empty();
        }

        @Override
        public Optional<Instant> findLatestHourlyCandleOpenTime(long symbolId) {
            return Optional.empty();
        }

        @Override
        public Optional<Instant> findLatestHourlyCandleOpenTimeBefore(long symbolId, Instant beforeExclusive) {
            return Optional.empty();
        }

        @Override
        public Optional<Instant> findLatestCompletedHourlyCandleOpenTime(long symbolId) {
            return Optional.ofNullable(latestCompletedHourlyOpenTime);
        }

        @Override
        public Optional<Instant> findLatestCompletedHourlyCandleOpenTimeBefore(
                long symbolId,
                Instant beforeExclusive
        ) {
            return completedHourlyCandles.stream()
                    .map(HourlyMarketCandle::openTime)
                    .filter(openTime -> openTime.isBefore(beforeExclusive))
                    .max(Instant::compareTo);
        }

        @Override
        public Optional<MarketHistoryCandle> findMinuteCandle(long symbolId, Instant openTime) {
            return Optional.empty();
        }

        @Override
        public List<MarketHistoryCandle> findMinuteCandles(long symbolId, Instant fromInclusive, Instant toExclusive) {
            return List.of();
        }

        @Override
        public Optional<HourlyMarketCandle> findHourlyCandle(long symbolId, Instant openTime) {
            return Optional.empty();
        }

        @Override
        public List<HourlyMarketCandle> findHourlyCandles(
                long symbolId,
                Instant fromInclusive,
                Instant toExclusive
        ) {
            rawHourlyRangeCalls++;
            return List.of();
        }

        @Override
        public List<HourlyMarketCandle> findCompletedHourlyCandles(
                long symbolId,
                Instant fromInclusive,
                Instant toExclusive
        ) {
            completedHourlyRangeCalls++;
            return completedHourlyCandles.stream()
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .toList();
        }

        @Override
        public void saveMinuteCandle(MarketHistoryCandle candle) {
        }

        @Override
        public void saveHourlyCandle(HourlyMarketCandle candle) {
        }
    }
}
