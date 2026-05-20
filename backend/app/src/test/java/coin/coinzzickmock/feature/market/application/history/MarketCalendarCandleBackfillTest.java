package coin.coinzzickmock.feature.market.application.history;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.application.implement.CompletedCalendarCandleBuilder;
import coin.coinzzickmock.feature.market.domain.CompletedMarketCandle;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MarketCalendarCandleBackfillTest {
    @Test
    void catchUpSkipsSymbolsWithoutExistingCalendarSeed() {
        RecordingMarketHistoryRepository repository = new RecordingMarketHistoryRepository();
        repository.cursors.add(new coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository.StartupBackfillCursor(
                1L,
                "BTCUSDT",
                Instant.parse("2026-04-18T00:00:00Z")
        ));
        MarketCalendarCandleBackfill backfill = new MarketCalendarCandleBackfill(
                repository,
                new CompletedCalendarCandleBuilder()
        );

        int saved = backfill.catchUpPersistedCalendarCandles();

        assertThat(saved).isZero();
        assertThat(repository.savedCompletedCandles).isEmpty();
    }

    @Test
    void catchUpBuildsClosedCalendarCandlesAfterExistingSeed() {
        RecordingMarketHistoryRepository repository = new RecordingMarketHistoryRepository();
        repository.cursors.add(new coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository.StartupBackfillCursor(
                1L,
                "BTCUSDT",
                Instant.parse("2026-04-19T00:00:00Z")
        ));
        repository.seedCompletedCandle(completed(MarketCandleInterval.ONE_DAY,
                Instant.parse("2026-04-16T00:00:00Z"),
                Instant.parse("2026-04-17T00:00:00Z")));
        Instant targetDay = Instant.parse("2026-04-17T00:00:00Z");
        for (int hour = 0; hour < 24; hour++) {
            repository.completedHourlyCandles.add(hourly(targetDay.plusSeconds(hour * 3600L)));
        }
        MarketCalendarCandleBackfill backfill = new MarketCalendarCandleBackfill(
                repository,
                new CompletedCalendarCandleBuilder()
        );

        int saved = backfill.catchUpPersistedCalendarCandles();

        assertThat(saved).isEqualTo(1);
        assertThat(repository.savedCompletedCandles).singleElement()
                .extracting(CompletedMarketCandle::openTime)
                .isEqualTo(targetDay);
    }

    @Test
    void rebuildClosedCalendarCandlesForHourUpsertsOnlyWhenBucketCoverageIsComplete() {
        RecordingMarketHistoryRepository repository = new RecordingMarketHistoryRepository();
        Instant dayStart = Instant.parse("2026-04-17T00:00:00Z");
        for (int hour = 0; hour < 24; hour++) {
            repository.completedHourlyCandles.add(hourly(dayStart.plusSeconds(hour * 3600L)));
        }
        MarketCalendarCandleBackfill backfill = new MarketCalendarCandleBackfill(
                repository,
                new CompletedCalendarCandleBuilder()
        );

        backfill.rebuildClosedCalendarCandlesForHour(1L, Instant.parse("2026-04-17T23:00:00Z"));

        assertThat(repository.savedCompletedCandles).anySatisfy(candle -> {
            assertThat(candle.interval()).isEqualTo(MarketCandleInterval.ONE_DAY);
            assertThat(candle.openTime()).isEqualTo(dayStart);
        });
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
                1000,
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
                1000,
                MarketCandleInterval.ONE_HOUR,
                openTime,
                closeTime,
                (int) java.time.temporal.ChronoUnit.HOURS.between(openTime, closeTime)
        );
    }

    private static class RecordingMarketHistoryRepository extends coin.coinzzickmock.testsupport.TestMarketHistoryRepository {
        private final List<StartupBackfillCursor> cursors = new ArrayList<>();
        private final List<HourlyMarketCandle> completedHourlyCandles = new ArrayList<>();
        private final List<CompletedMarketCandle> completedCandles = new ArrayList<>();
        private final List<CompletedMarketCandle> savedCompletedCandles = new ArrayList<>();

        @Override
        public List<StartupBackfillCursor> findStartupBackfillCursors() {
            return List.copyOf(cursors);
        }

        @Override
        public Optional<Instant> findLatestCompletedHourlyCandleOpenTime(long symbolId) {
            return completedHourlyCandles.stream()
                    .map(HourlyMarketCandle::openTime)
                    .max(Instant::compareTo);
        }

        @Override
        public boolean existsCompletedCandle(long symbolId, MarketCandleInterval interval) {
            return completedCandles.stream().anyMatch(candle -> candle.interval() == interval);
        }

        @Override
        public Optional<Instant> findLatestCompletedCandleOpenTime(long symbolId, MarketCandleInterval interval) {
            return completedCandles.stream()
                    .filter(candle -> candle.interval() == interval)
                    .map(CompletedMarketCandle::openTime)
                    .max(Instant::compareTo);
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
                    .sorted(Comparator.comparing(HourlyMarketCandle::openTime))
                    .toList();
        }

        @Override
        public void saveCompletedCandle(CompletedMarketCandle candle) {
            savedCompletedCandles.add(candle);
            seedCompletedCandle(candle);
        }

        private void seedCompletedCandle(CompletedMarketCandle candle) {
            completedCandles.removeIf(existing -> existing.interval() == candle.interval()
                    && existing.openTime().equals(candle.openTime()));
            completedCandles.add(candle);
        }
    }
}
