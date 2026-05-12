package coin.coinzzickmock.feature.market.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.testsupport.TestConnectorProvider;
import coin.coinzzickmock.feature.market.application.history.MarketCandleRollupProjector;
import coin.coinzzickmock.feature.market.application.history.MarketHistoricalCandleAppender;
import coin.coinzzickmock.feature.market.application.history.MarketHistoricalCandleCache;
import coin.coinzzickmock.feature.market.application.history.MarketHistoricalCandleSegmentFetcher;
import coin.coinzzickmock.feature.market.application.history.MarketHistoricalCandleSegmentPolicy;
import coin.coinzzickmock.feature.market.application.history.MarketHistoricalCandleSegmentStore;
import coin.coinzzickmock.feature.market.application.history.MarketHistoricalCandleTelemetry;
import coin.coinzzickmock.feature.market.application.history.MarketHistoryLookupTelemetry;
import coin.coinzzickmock.feature.market.application.history.MarketPersistedCandleReader;
import coin.coinzzickmock.feature.market.application.query.GetMarketCandlesQuery;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoricalCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.AuthProvider;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.feature.market.application.gateway.MarketDataGateway;
import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import coin.coinzzickmock.providers.infrastructure.config.CoinCacheNames;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

class GetMarketCandlesServiceTest {
    @Test
    void rollsUpFiveMinuteCandlesFromMinuteHistory() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        repository.saveMinuteCandle(minute(1L, "2026-04-21T00:00:00Z", 100, 101, 99, 100.5, 10));
        repository.saveMinuteCandle(minute(1L, "2026-04-21T00:01:00Z", 100.5, 102, 100, 101.2, 12));
        repository.saveMinuteCandle(minute(1L, "2026-04-21T00:02:00Z", 101.2, 103, 101, 102.8, 15));
        repository.saveMinuteCandle(minute(1L, "2026-04-21T00:03:00Z", 102.8, 104, 102, 103.6, 11));
        repository.saveMinuteCandle(minute(1L, "2026-04-21T00:04:00Z", 103.6, 105, 103, 104.4, 13));

        GetMarketCandlesService service = service(repository);

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

        GetMarketCandlesService service = service(repository);

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

        GetMarketCandlesService service = service(repository);

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
    void rollsUpFourHourCandlesOnUtcBoundary() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        for (int hour = 0; hour < 4; hour++) {
            Instant openTime = Instant.parse("2026-04-24T16:00:00Z").plusSeconds(hour * 3600L);
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
        repository.saveHourlyCandle(hourly(1L, "2026-04-24T20:00:00Z", 300, 301, 299, 300.5, 10));

        GetMarketCandlesService service = service(repository);

        List<MarketCandleResult> results = service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "4h", 2, null));

        assertEquals(1, results.size());
        assertEquals(Instant.parse("2026-04-24T16:00:00Z"), results.get(0).openTime());
        assertEquals(103.5, results.get(0).closePrice(), 0.0001);
        assertEquals(40.0, results.get(0).volume(), 0.0001);
    }

    @Test
    void rollsUpDailyCandlesOnUtcCalendarBoundary() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        for (int hour = 0; hour < 24; hour++) {
            Instant openTime = Instant.parse("2026-04-22T00:00:00Z").plusSeconds(hour * 3600L);
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
        repository.saveHourlyCandle(hourly(1L, "2026-04-23T00:00:00Z", 300, 301, 299, 300.5, 10));

        GetMarketCandlesService service = service(repository);

        List<MarketCandleResult> results = service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "1D", 2, null));

        assertEquals(1, results.size());
        assertEquals(Instant.parse("2026-04-22T00:00:00Z"), results.get(0).openTime());
        assertEquals(123.5, results.get(0).closePrice(), 0.0001);
        assertEquals(240.0, results.get(0).volume(), 0.0001);
    }

    @Test
    void rollsUpWeeklyCandlesOnUtcCalendarBoundary() {
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

        GetMarketCandlesService service = service(repository);

        List<MarketCandleResult> results = service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "1W", 2, null));

        assertEquals(1, results.size());
        assertEquals(Instant.parse("2026-04-20T00:00:00Z"), results.get(0).openTime());
        assertEquals(267.5, results.get(0).closePrice(), 0.0001);
        assertEquals(1680.0, results.get(0).volume(), 0.0001);
    }

    @Test
    void dailyRollupUsesLatestCompleteBucketBeforeHistoricalFallback() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        saveHourlyRange(repository, "2026-04-27T00:00:00Z", "2026-04-29T00:00:00Z");
        saveHourlyRange(repository, "2026-04-29T00:00:00Z", "2026-04-29T19:00:00Z");
        FakeMarketDataGateway gateway = FakeMarketDataGateway.withHistoricalCandles();
        GetMarketCandlesService service = service(repository, gateway, distributedCacheManager());

        List<MarketCandleResult> results = service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "1D", 2, null));

        assertEquals(2, results.size());
        assertEquals(Instant.parse("2026-04-27T00:00:00Z"), results.get(0).openTime());
        assertEquals(Instant.parse("2026-04-28T00:00:00Z"), results.get(1).openTime());
        assertEquals(0, gateway.historicalCallCount);
    }

    @Test
    void weeklyRollupUsesLatestCompleteBucketBeforeHistoricalFallback() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        saveHourlyRange(repository, "2026-04-06T00:00:00Z", "2026-04-20T00:00:00Z");
        saveHourlyRange(repository, "2026-04-20T00:00:00Z", "2026-04-22T00:00:00Z");
        FakeMarketDataGateway gateway = FakeMarketDataGateway.withHistoricalCandles();
        GetMarketCandlesService service = service(repository, gateway, distributedCacheManager());

        List<MarketCandleResult> results = service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "1W", 2, null));

        assertEquals(2, results.size());
        assertEquals(Instant.parse("2026-04-06T00:00:00Z"), results.get(0).openTime());
        assertEquals(Instant.parse("2026-04-13T00:00:00Z"), results.get(1).openTime());
        assertEquals(0, gateway.historicalCallCount);
    }

    @Test
    void monthlyRollupUsesLatestCompleteBucketBeforeHistoricalFallback() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        saveHourlyRange(repository, "2026-02-01T00:00:00Z", "2026-04-01T00:00:00Z");
        saveHourlyRange(repository, "2026-04-01T00:00:00Z", "2026-04-15T00:00:00Z");
        FakeMarketDataGateway gateway = FakeMarketDataGateway.withHistoricalCandles();
        GetMarketCandlesService service = service(repository, gateway, distributedCacheManager());

        List<MarketCandleResult> results = service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "1M", 2, null));

        assertEquals(2, results.size());
        assertEquals(Instant.parse("2026-02-01T00:00:00Z"), results.get(0).openTime());
        assertEquals(Instant.parse("2026-03-01T00:00:00Z"), results.get(1).openTime());
        assertEquals(0, gateway.historicalCallCount);
    }

    @Test
    void supplementsPartialMinutePageFromHistoricalCacheWithoutSavingToRepository() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        Instant dbStart = Instant.parse("2026-03-01T00:00:00Z");
        for (int minute = 0; minute < 80; minute++) {
            Instant openTime = dbStart.plusSeconds(minute * 60L);
            repository.saveMinuteCandle(minute(
                    1L,
                    openTime.toString(),
                    200 + minute,
                    201 + minute,
                    199 + minute,
                    200.5 + minute,
                    10
            ));
        }
        FakeMarketDataGateway gateway = FakeMarketDataGateway.withHistoricalCandles();
        GetMarketCandlesService service = service(repository, gateway, distributedCacheManager());

        List<MarketCandleResult> results = service.getCandles(new GetMarketCandlesQuery(
                "BTCUSDT",
                "1m",
                200,
                Instant.parse("2026-03-01T01:20:00Z")
        ));

        assertEquals(200, results.size());
        assertEquals(1, gateway.historicalCallCount);
        assertEquals(80, repository.findMinuteCandles(
                1L,
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-03-01T01:20:00Z")
        ).size());
        assertEquals(dbStart, results.get(120).openTime());
    }

    @Test
    void reusesAlignedRedisSegmentForSlidingOlderMinuteRequests() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        FakeMarketDataGateway gateway = FakeMarketDataGateway.withHistoricalCandles();
        GetMarketCandlesService service = service(repository, gateway, distributedCacheManager());

        service.getCandles(new GetMarketCandlesQuery(
                "BTCUSDT",
                "1m",
                20,
                Instant.parse("2026-03-01T00:40:00Z")
        ));
        service.getCandles(new GetMarketCandlesQuery(
                "BTCUSDT",
                "1m",
                20,
                Instant.parse("2026-03-01T00:35:00Z")
        ));

        assertEquals(1, gateway.historicalCallCount);
    }

    @Test
    void doesNotCacheFutureEndedHistoricalSegmentsAsComplete() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        FakeMarketDataGateway gateway = FakeMarketDataGateway.withHistoricalCandles();
        GetMarketCandlesService service = service(repository, gateway, distributedCacheManager());

        GetMarketCandlesQuery query = new GetMarketCandlesQuery(
                "BTCUSDT",
                "1W",
                20,
                Instant.parse("2099-01-01T00:00:00Z")
        );
        service.getCandles(query);
        service.getCandles(query);

        assertEquals(2, gateway.historicalCallCount);
    }

    @Test
    void fallsBackToBitgetWhenDistributedCacheFails() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        FakeMarketDataGateway gateway = FakeMarketDataGateway.withHistoricalCandles();
        GetMarketCandlesService service = service(repository, gateway, new FailingCacheManager());

        List<MarketCandleResult> results = service.getCandles(new GetMarketCandlesQuery(
                "BTCUSDT",
                "1m",
                20,
                Instant.parse("2026-03-01T00:40:00Z")
        ));

        assertEquals(20, results.size());
        assertEquals(1, gateway.historicalCallCount);
    }

    @Test
    void recordsDbLookupTelemetryBeforeHistoricalSupplement() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        repository.saveMinuteCandle(minute(1L, "2026-04-21T00:00:00Z", 100, 101, 99, 100.5, 10));
        FakeProviders providers = new FakeProviders(FakeMarketDataGateway.empty());
        GetMarketCandlesService service = service(repository, providers, distributedCacheManager());

        service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "1m", 2, null));

        RecordedEvent dbLookup = providers.telemetry().events.stream()
                .filter(event -> "db".equals(event.tags().get("source")))
                .findFirst()
                .orElseThrow();
        assertEquals("market.history.db.miss", dbLookup.eventName());
        assertEquals("BTCUSDT", dbLookup.tags().get("symbol"));
        assertEquals("1m", dbLookup.tags().get("interval"));
        assertEquals("2026-04", dbLookup.tags().get("range_bucket"));
        assertEquals("partial", dbLookup.tags().get("result"));
    }

    private static GetMarketCandlesService service(InMemoryMarketHistoryRepository repository) {
        return service(repository, FakeMarketDataGateway.empty(), distributedCacheManager());
    }

    private static GetMarketCandlesService service(
            InMemoryMarketHistoryRepository repository,
            FakeMarketDataGateway gateway,
            CacheManager cacheManager
    ) {
        return service(repository, new FakeProviders(gateway), cacheManager);
    }

    private static GetMarketCandlesService service(
            InMemoryMarketHistoryRepository repository,
            FakeProviders providers,
            CacheManager cacheManager
    ) {
        MarketHistoricalCandleTelemetry telemetry = new MarketHistoricalCandleTelemetry(providers);
        MarketHistoricalCandleCache cache = new MarketHistoricalCandleCache(
                new MarketHistoricalCandleSegmentPolicy(),
                new MarketHistoricalCandleSegmentStore(telemetry, objectProvider(cacheManager)),
                new MarketHistoricalCandleSegmentFetcher(providers.marketDataGateway, telemetry)
        );
        MarketCandleRollupProjector rollupProjector = new MarketCandleRollupProjector();
        return new GetMarketCandlesService(
                repository,
                new MarketPersistedCandleReader(repository, rollupProjector),
                new MarketHistoricalCandleAppender(cache),
                new MarketHistoryLookupTelemetry(providers)
        );
    }

    private static CacheManager distributedCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                CoinCacheNames.MARKET_HISTORICAL_CANDLES_DISTRIBUTED_CACHE
        );
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

    private static ObjectProvider<CacheManager> objectProvider(CacheManager cacheManager) {
        return new ObjectProvider<>() {
            @Override
            public CacheManager getObject(Object... args) {
                return cacheManager;
            }

            @Override
            public CacheManager getIfAvailable() {
                return cacheManager;
            }

            @Override
            public CacheManager getIfUnique() {
                return cacheManager;
            }

            @Override
            public CacheManager getObject() {
                return cacheManager;
            }

            @Override
            public Stream<CacheManager> stream() {
                return Stream.of(cacheManager);
            }

            @Override
            public Stream<CacheManager> orderedStream() {
                return Stream.of(cacheManager);
            }
        };
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

    private static void saveHourlyRange(
            InMemoryMarketHistoryRepository repository,
            String fromInclusive,
            String toExclusive
    ) {
        Instant cursor = Instant.parse(fromInclusive);
        Instant end = Instant.parse(toExclusive);
        while (cursor.isBefore(end)) {
            repository.saveHourlyCandle(hourly(1L, cursor.toString(), 100, 101, 99, 100.5, 10));
            cursor = cursor.plusSeconds(3600);
        }
    }

    private static class InMemoryMarketHistoryRepository extends coin.coinzzickmock.testsupport.TestMarketHistoryRepository {
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

    private static class FakeProviders implements Providers {
        private final FakeMarketDataGateway marketDataGateway;
        private final RecordingTelemetryProvider telemetry = new RecordingTelemetryProvider();

        private FakeProviders(FakeMarketDataGateway marketDataGateway) {
            this.marketDataGateway = marketDataGateway;
        }

        @Override
        public AuthProvider auth() {
            return null;
        }

        @Override
        public ConnectorProvider connector() {
            return TestConnectorProvider.empty();
        }

        @Override
        public RecordingTelemetryProvider telemetry() {
            return telemetry;
        }

        @Override
        public FeatureFlagProvider featureFlags() {
            return null;
        }
    }

    private static class RecordingTelemetryProvider extends coin.coinzzickmock.testsupport.TestTelemetryProvider {
        private final List<RecordedEvent> events = new ArrayList<>();

        @Override
        public void recordUseCase(String useCaseName) {
        }

        @Override
        public void recordFailure(String useCaseName, String reason) {
        }

        @Override
        public void recordEvent(String eventName, Map<String, String> tags) {
            events.add(new RecordedEvent(eventName, tags));
        }
    }

    private record RecordedEvent(String eventName, Map<String, String> tags) {
    }

    private static class FakeMarketDataGateway extends coin.coinzzickmock.testsupport.TestMarketDataGateway {
        private final boolean hasHistoricalCandles;
        private int historicalCallCount;

        private FakeMarketDataGateway(boolean hasHistoricalCandles) {
            this.hasHistoricalCandles = hasHistoricalCandles;
        }

        static FakeMarketDataGateway empty() {
            return new FakeMarketDataGateway(false);
        }

        static FakeMarketDataGateway withHistoricalCandles() {
            return new FakeMarketDataGateway(true);
        }

        @Override
        public List<coin.coinzzickmock.feature.market.domain.MarketSnapshot> loadSupportedMarkets() {
            return List.of();
        }

        @Override
        public coin.coinzzickmock.feature.market.domain.MarketSnapshot loadMarket(String symbol) {
            return null;
        }

        @Override
        public List<MarketHistoricalCandleSnapshot> loadHistoricalCandles(
                String symbol,
                MarketCandleInterval interval,
                Instant fromInclusive,
                Instant toExclusive,
                int limit
        ) {
            historicalCallCount++;
            if (!hasHistoricalCandles) {
                return List.of();
            }

            List<MarketHistoricalCandleSnapshot> candles = new ArrayList<>();
            Instant cursor = fromInclusive;
            while (cursor.isBefore(toExclusive) && candles.size() < limit) {
                candles.add(new MarketHistoricalCandleSnapshot(
                        cursor,
                        cursor.plusSeconds(60),
                        100,
                        101,
                        99,
                        100.5,
                        10,
                        1005
                ));
                cursor = cursor.plusSeconds(60);
            }
            return candles;
        }
    }

    private static class FailingCacheManager implements CacheManager {
        @Override
        public Cache getCache(String name) {
            return new Cache() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public Object getNativeCache() {
                    return this;
                }

                @Override
                public ValueWrapper get(Object key) {
                    throw new IllegalStateException("cache read failed");
                }

                @Override
                public <T> T get(Object key, Class<T> type) {
                    throw new IllegalStateException("cache read failed");
                }

                @Override
                public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
                    throw new IllegalStateException("cache read failed");
                }

                @Override
                public void put(Object key, Object value) {
                    throw new IllegalStateException("cache write failed");
                }

                @Override
                public ValueWrapper putIfAbsent(Object key, Object value) {
                    throw new IllegalStateException("cache write failed");
                }

                @Override
                public void evict(Object key) {
                }

                @Override
                public boolean evictIfPresent(Object key) {
                    return false;
                }

                @Override
                public void clear() {
                }

                @Override
                public boolean invalidate() {
                    return false;
                }
            };
        }

        @Override
        public java.util.Collection<String> getCacheNames() {
            return List.of(CoinCacheNames.MARKET_HISTORICAL_CANDLES_DISTRIBUTED_CACHE);
        }
    }
}
