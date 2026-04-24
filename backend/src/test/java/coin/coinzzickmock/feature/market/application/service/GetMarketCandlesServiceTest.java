package coin.coinzzickmock.feature.market.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.feature.market.application.query.GetMarketCandlesQuery;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetMarketCandlesServiceTest {
    @Test
    void rollsUpFiveMinuteCandlesFromMinuteHistory() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        repository.saveMinuteCandle(minute(1L, "2026-04-21T00:00:00Z", 100, 101, 99, 100.5, 10));
        repository.saveMinuteCandle(minute(1L, "2026-04-21T00:01:00Z", 100.5, 102, 100, 101.2, 12));
        repository.saveMinuteCandle(minute(1L, "2026-04-21T00:02:00Z", 101.2, 103, 101, 102.8, 15));
        repository.saveMinuteCandle(minute(1L, "2026-04-21T00:03:00Z", 102.8, 104, 102, 103.6, 11));
        repository.saveMinuteCandle(minute(1L, "2026-04-21T00:04:00Z", 103.6, 105, 103, 104.4, 13));

        GetMarketCandlesService service = new GetMarketCandlesService(repository);

        List<MarketCandleResult> results = service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "5m", 1, null));

        assertEquals(1, results.size());
        assertEquals(100.0, results.get(0).openPrice(), 0.0001);
        assertEquals(105.0, results.get(0).highPrice(), 0.0001);
        assertEquals(99.0, results.get(0).lowPrice(), 0.0001);
        assertEquals(104.4, results.get(0).closePrice(), 0.0001);
        assertEquals(61.0, results.get(0).volume(), 0.0001);
    }

    @Test
    void returnsOlderMinuteCandlesBeforeCursor() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        repository.saveMinuteCandle(minute(1L, "2026-04-21T00:00:00Z", 100, 101, 99, 100.5, 10));
        repository.saveMinuteCandle(minute(1L, "2026-04-21T00:01:00Z", 100.5, 102, 100, 101.2, 12));
        repository.saveMinuteCandle(minute(1L, "2026-04-21T00:02:00Z", 101.2, 103, 101, 102.8, 15));
        repository.saveMinuteCandle(minute(1L, "2026-04-21T00:03:00Z", 102.8, 104, 102, 103.6, 11));

        GetMarketCandlesService service = new GetMarketCandlesService(repository);

        List<MarketCandleResult> results = service.getCandles(new GetMarketCandlesQuery(
                "BTCUSDT",
                "1m",
                2,
                Instant.parse("2026-04-21T00:03:00Z")
        ));

        assertEquals(2, results.size());
        assertEquals(Instant.parse("2026-04-21T00:01:00Z"), results.get(0).openTime());
        assertEquals(Instant.parse("2026-04-21T00:02:00Z"), results.get(1).openTime());
    }

    @Test
    void rollsUpOlderFiveMinuteCandlesBeforeCursor() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        for (int minute = 0; minute < 10; minute++) {
            Instant openTime = Instant.parse("2026-04-21T00:00:00Z").plusSeconds(minute * 60L);
            repository.saveMinuteCandle(new MarketHistoryCandle(
                    1L,
                    openTime,
                    openTime.plusSeconds(60),
                    100 + minute,
                    101 + minute,
                    99 + minute,
                    100.5 + minute,
                    10 + minute,
                    (10 + minute) * (100.5 + minute)
            ));
        }

        GetMarketCandlesService service = new GetMarketCandlesService(repository);

        List<MarketCandleResult> results = service.getCandles(new GetMarketCandlesQuery(
                "BTCUSDT",
                "5m",
                1,
                Instant.parse("2026-04-21T00:05:00Z")
        ));

        assertEquals(1, results.size());
        assertEquals(Instant.parse("2026-04-21T00:00:00Z"), results.get(0).openTime());
        assertEquals(100.0, results.get(0).openPrice(), 0.0001);
        assertEquals(105.0, results.get(0).highPrice(), 0.0001);
        assertEquals(99.0, results.get(0).lowPrice(), 0.0001);
        assertEquals(104.5, results.get(0).closePrice(), 0.0001);
    }

    @Test
    void rollsUpWeeklyCandlesOnCalendarBoundary() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        for (int hour = 0; hour < 168; hour++) {
            Instant openTime = Instant.parse("2026-04-20T00:00:00Z").plusSeconds(hour * 3600L);
            repository.saveHourlyCandle(new HourlyMarketCandle(
                    1L,
                    openTime,
                    openTime.plusSeconds(3600),
                    100 + hour,
                    101 + hour,
                    99 + hour,
                    100.5 + hour,
                    10,
                    1000,
                    openTime,
                    openTime.plusSeconds(3600)
            ));
        }
        repository.saveHourlyCandle(hourly(1L, "2026-04-27T00:00:00Z", 300, 301, 299, 300.5, 10));

        GetMarketCandlesService service = new GetMarketCandlesService(repository);

        List<MarketCandleResult> results = service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "1W", 2, null));

        assertEquals(1, results.size());
        assertEquals(Instant.parse("2026-04-20T00:00:00Z"), results.get(0).openTime());
        assertEquals(267.5, results.get(0).closePrice(), 0.0001);
        assertEquals(1680.0, results.get(0).volume(), 0.0001);
    }

    private static MarketHistoryCandle minute(
            long symbolId,
            String openTime,
            double open,
            double high,
            double low,
            double close,
            double volume
    ) {
        Instant openInstant = Instant.parse(openTime);
        return new MarketHistoryCandle(
                symbolId,
                openInstant,
                openInstant.plusSeconds(60),
                open,
                high,
                low,
                close,
                volume,
                volume * close
        );
    }

    private static HourlyMarketCandle hourly(
            long symbolId,
            String openTime,
            double open,
            double high,
            double low,
            double close,
            double volume
    ) {
        Instant openInstant = Instant.parse(openTime);
        return new HourlyMarketCandle(
                symbolId,
                openInstant,
                openInstant.plusSeconds(3600),
                open,
                high,
                low,
                close,
                volume,
                volume * close,
                openInstant,
                openInstant.plusSeconds(3600)
        );
    }

    private static class InMemoryMarketHistoryRepository implements MarketHistoryRepository {
        private final Map<String, Long> symbolIds = Map.of("BTCUSDT", 1L);
        private final Map<String, MarketHistoryCandle> minuteCandles = new LinkedHashMap<>();
        private final Map<String, HourlyMarketCandle> hourlyCandles = new LinkedHashMap<>();

        @Override
        public Map<String, Long> findSymbolIdsBySymbols(List<String> symbols) {
          Map<String, Long> resolved = new LinkedHashMap<>();
          symbols.forEach(symbol -> {
              if (symbolIds.containsKey(symbol)) {
                  resolved.put(symbol, symbolIds.get(symbol));
              }
          });
          return resolved;
        }

        @Override
        public List<StartupBackfillCursor> findStartupBackfillCursors() {
            return List.of();
        }

        @Override
        public Optional<Instant> findLatestMinuteCandleOpenTime(long symbolId) {
            return minuteCandles.values().stream()
                    .filter(candle -> candle.symbolId() == symbolId)
                    .map(MarketHistoryCandle::openTime)
                    .max(Instant::compareTo);
        }

        @Override
        public Optional<Instant> findLatestMinuteCandleOpenTimeBefore(long symbolId, Instant beforeExclusive) {
            return minuteCandles.values().stream()
                    .filter(candle -> candle.symbolId() == symbolId)
                    .map(MarketHistoryCandle::openTime)
                    .filter(openTime -> openTime.isBefore(beforeExclusive))
                    .max(Instant::compareTo);
        }

        @Override
        public Optional<Instant> findLatestHourlyCandleOpenTime(long symbolId) {
            return hourlyCandles.values().stream()
                    .filter(candle -> candle.symbolId() == symbolId)
                    .map(HourlyMarketCandle::openTime)
                    .max(Instant::compareTo);
        }

        @Override
        public Optional<Instant> findLatestHourlyCandleOpenTimeBefore(long symbolId, Instant beforeExclusive) {
            return hourlyCandles.values().stream()
                    .filter(candle -> candle.symbolId() == symbolId)
                    .map(HourlyMarketCandle::openTime)
                    .filter(openTime -> openTime.isBefore(beforeExclusive))
                    .max(Instant::compareTo);
        }

        @Override
        public Optional<MarketHistoryCandle> findMinuteCandle(long symbolId, Instant openTime) {
            return Optional.ofNullable(minuteCandles.get(key(symbolId, openTime)));
        }

        @Override
        public List<MarketHistoryCandle> findMinuteCandles(long symbolId, Instant fromInclusive, Instant toExclusive) {
            return minuteCandles.values().stream()
                    .filter(candle -> candle.symbolId() == symbolId)
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .sorted(java.util.Comparator.comparing(MarketHistoryCandle::openTime))
                    .toList();
        }

        @Override
        public Optional<HourlyMarketCandle> findHourlyCandle(long symbolId, Instant openTime) {
            return Optional.ofNullable(hourlyCandles.get(key(symbolId, openTime)));
        }

        @Override
        public List<HourlyMarketCandle> findHourlyCandles(long symbolId, Instant fromInclusive, Instant toExclusive) {
            return hourlyCandles.values().stream()
                    .filter(candle -> candle.symbolId() == symbolId)
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .sorted(java.util.Comparator.comparing(HourlyMarketCandle::openTime))
                    .toList();
        }

        @Override
        public void saveMinuteCandle(MarketHistoryCandle candle) {
            minuteCandles.put(key(candle.symbolId(), candle.openTime()), candle);
        }

        @Override
        public void saveHourlyCandle(HourlyMarketCandle candle) {
            hourlyCandles.put(key(candle.symbolId(), candle.openTime()), candle);
        }

        private String key(long symbolId, Instant openTime) {
            return symbolId + ":" + openTime;
        }
    }
}
