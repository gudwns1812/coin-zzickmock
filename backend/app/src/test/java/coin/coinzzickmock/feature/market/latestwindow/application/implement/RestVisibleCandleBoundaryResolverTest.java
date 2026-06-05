package coin.coinzzickmock.feature.market.latestwindow.application.implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coin.coinzzickmock.feature.market.latestwindow.application.dto.RestVisibleCandleBoundary;
import coin.coinzzickmock.feature.market.history.application.implement.MarketCandleRollupProjector;
import coin.coinzzickmock.feature.market.domain.CompletedMarketCandle;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import coin.coinzzickmock.testsupport.TestMarketHistoryRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RestVisibleCandleBoundaryResolverTest {
    @Test
    void resolvesLatestFinalizedMinuteBoundary() {
        Repository repository = new Repository();
        repository.saveMinuteCandle(minute("2026-04-21T00:00:00Z"));
        repository.saveMinuteCandle(minute("2026-04-21T00:01:00Z"));

        Optional<RestVisibleCandleBoundary> boundary = resolver(repository).resolve(1L, MarketCandleInterval.ONE_MINUTE);

        assertTrue(boundary.isPresent());
        assertEquals(Instant.parse("2026-04-21T00:01:00Z"), boundary.get().latestOutputOpenTime());
    }

    @Test
    void rollsBackIncompleteMinuteRollupBucketToPreviousCompletedBucket() {
        Repository repository = new Repository();
        for (int minute = 0; minute < 6; minute++) {
            repository.saveMinuteCandle(minute(Instant.parse("2026-04-21T00:00:00Z").plusSeconds(minute * 60L)));
        }

        Optional<RestVisibleCandleBoundary> boundary = resolver(repository).resolve(1L, MarketCandleInterval.FIVE_MINUTES);

        assertTrue(boundary.isPresent());
        assertEquals(Instant.parse("2026-04-21T00:00:00Z"), boundary.get().latestOutputOpenTime());
    }

    @Test
    void doesNotResolveMinuteRollupBoundaryWhenCompletedCandidateHasGap() {
        Repository repository = new Repository();
        repository.saveMinuteCandle(minute("2026-04-21T00:00:00Z"));
        repository.saveMinuteCandle(minute("2026-04-21T00:01:00Z"));
        repository.saveMinuteCandle(minute("2026-04-21T00:03:00Z"));

        Optional<RestVisibleCandleBoundary> boundary = resolver(repository).resolve(1L, MarketCandleInterval.THREE_MINUTES);

        assertTrue(boundary.isEmpty());
    }

    @Test
    void rollsBackIncompleteFourHourBucketToPreviousCompletedBucketOnUtcBoundary() {
        Repository repository = new Repository();
        saveHourlyRange(repository, "2026-04-24T16:00:00Z", "2026-04-24T20:00:00Z");
        repository.saveHourlyCandle(hourly("2026-04-24T20:00:00Z"));

        Optional<RestVisibleCandleBoundary> boundary = resolver(repository).resolve(1L, MarketCandleInterval.FOUR_HOURS);

        assertTrue(boundary.isPresent());
        assertEquals(Instant.parse("2026-04-24T16:00:00Z"), boundary.get().latestOutputOpenTime());
    }

    @Test
    void rollsBackIncompleteTwelveHourBucketToPreviousCompletedBucket() {
        Repository repository = new Repository();
        saveHourlyRange(repository, "2026-04-24T00:00:00Z", "2026-04-24T12:00:00Z");
        repository.saveHourlyCandle(hourly("2026-04-24T12:00:00Z"));

        Optional<RestVisibleCandleBoundary> boundary = resolver(repository).resolve(1L, MarketCandleInterval.TWELVE_HOURS);

        assertTrue(boundary.isPresent());
        assertEquals(Instant.parse("2026-04-24T00:00:00Z"), boundary.get().latestOutputOpenTime());
    }

    @Test
    void rollsBackIncompleteWeekToPreviousCompletedWeek() {
        Repository repository = new Repository();
        saveDailyRange(repository, "2026-04-06T00:00:00Z", "2026-04-13T00:00:00Z");
        repository.saveCompletedCandle(completed(MarketCandleInterval.ONE_DAY, "2026-04-13T00:00:00Z"));

        Optional<RestVisibleCandleBoundary> boundary = resolver(repository).resolve(1L, MarketCandleInterval.ONE_WEEK);

        assertTrue(boundary.isPresent());
        assertEquals(Instant.parse("2026-04-06T00:00:00Z"), boundary.get().latestOutputOpenTime());
    }

    @Test
    void returnsEmptyWhenNoSourceCandleExists() {
        Optional<RestVisibleCandleBoundary> boundary = resolver(new Repository()).resolve(1L, MarketCandleInterval.ONE_WEEK);

        assertTrue(boundary.isEmpty());
    }

    private static RestVisibleCandleBoundaryResolver resolver(Repository repository) {
        return new RestVisibleCandleBoundaryResolver(repository, new MarketCandleRollupProjector());
    }

    private static MarketHistoryCandle minute(String openTime) {
        return minute(Instant.parse(openTime));
    }

    private static MarketHistoryCandle minute(Instant openTime) {
        return new MarketHistoryCandle(1L, openTime, openTime.plusSeconds(60), 100, 101, 99, 100.5, 10, 1005);
    }

    private static HourlyMarketCandle hourly(String openTime) {
        Instant open = Instant.parse(openTime);
        return new HourlyMarketCandle(1L, open, open.plusSeconds(3600), 100, 101, 99, 100.5,
                10, 1005, open, open.plusSeconds(3600));
    }

    private static CompletedMarketCandle completed(MarketCandleInterval interval, String openTime) {
        Instant open = Instant.parse(openTime);
        return new CompletedMarketCandle(1L, interval, open, open.plusSeconds(86_400), 100, 101, 99, 100.5,
                240, 24_120);
    }

    private static void saveHourlyRange(Repository repository, String fromInclusive, String toExclusive) {
        Instant cursor = Instant.parse(fromInclusive);
        Instant end = Instant.parse(toExclusive);
        while (cursor.isBefore(end)) {
            repository.saveHourlyCandle(hourly(cursor.toString()));
            cursor = cursor.plusSeconds(3600);
        }
    }

    private static void saveDailyRange(Repository repository, String fromInclusive, String toExclusive) {
        Instant cursor = Instant.parse(fromInclusive);
        Instant end = Instant.parse(toExclusive);
        while (cursor.isBefore(end)) {
            repository.saveCompletedCandle(completed(MarketCandleInterval.ONE_DAY, cursor.toString()));
            cursor = cursor.plusSeconds(86_400);
        }
    }

    private static class Repository extends TestMarketHistoryRepository {
        private final Map<Instant, MarketHistoryCandle> minuteCandles = new LinkedHashMap<>();
        private final Map<Instant, HourlyMarketCandle> hourlyCandles = new LinkedHashMap<>();
        private final Map<String, CompletedMarketCandle> completedCandles = new LinkedHashMap<>();

        @Override
        public Optional<Instant> findLatestMinuteCandleOpenTime(long symbolId) {
            return minuteCandles.values().stream().map(MarketHistoryCandle::openTime).max(Instant::compareTo);
        }

        @Override
        public List<MarketHistoryCandle> findMinuteCandles(long symbolId, Instant fromInclusive, Instant toExclusive) {
            return minuteCandles.values().stream()
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .sorted(Comparator.comparing(MarketHistoryCandle::openTime))
                    .toList();
        }

        @Override
        public Optional<Instant> findLatestCompletedHourlyCandleOpenTime(long symbolId) {
            return hourlyCandles.values().stream().map(HourlyMarketCandle::openTime).max(Instant::compareTo);
        }

        @Override
        public List<HourlyMarketCandle> findCompletedHourlyCandles(long symbolId, Instant fromInclusive, Instant toExclusive) {
            return hourlyCandles.values().stream()
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .sorted(Comparator.comparing(HourlyMarketCandle::openTime))
                    .toList();
        }

        @Override
        public Optional<Instant> findLatestCompletedCandleOpenTime(long symbolId, MarketCandleInterval interval) {
            return completedCandles.values().stream()
                    .filter(candle -> candle.interval() == interval)
                    .map(CompletedMarketCandle::openTime)
                    .max(Instant::compareTo);
        }

        @Override
        public List<CompletedMarketCandle> findCompletedCandles(
                long symbolId,
                MarketCandleInterval interval,
                Instant fromInclusive,
                Instant toExclusive
        ) {
            return completedCandles.values().stream()
                    .filter(candle -> candle.interval() == interval)
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .sorted(Comparator.comparing(CompletedMarketCandle::openTime))
                    .toList();
        }

        @Override
        public void saveMinuteCandle(MarketHistoryCandle candle) {
            minuteCandles.put(candle.openTime(), candle);
        }

        @Override
        public void saveHourlyCandle(HourlyMarketCandle candle) {
            hourlyCandles.put(candle.openTime(), candle);
            saveCompletedCandle(CompletedMarketCandle.fromHourly(candle));
        }

        @Override
        public void saveCompletedCandle(CompletedMarketCandle candle) {
            completedCandles.put(candle.interval() + ":" + candle.openTime(), candle);
        }
    }
}
