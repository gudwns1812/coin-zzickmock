package coin.coinzzickmock.feature.market.candle.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.testsupport.TestConnectorProvider;
import coin.coinzzickmock.feature.market.history.application.implement.MarketCandleRollupProjector;
import coin.coinzzickmock.feature.market.history.application.implement.MarketHistoricalCandleAppender;
import coin.coinzzickmock.feature.market.history.application.implement.MarketHistoricalCandleCache;
import coin.coinzzickmock.feature.market.history.application.implement.MarketHistoricalCandleSegmentFetcher;
import coin.coinzzickmock.feature.market.history.application.implement.MarketHistoricalCandleSegmentPolicy;
import coin.coinzzickmock.feature.market.history.application.implement.MarketHistoricalCandleSegmentStore;
import coin.coinzzickmock.feature.market.history.application.implement.MarketHistoricalCandleTelemetry;
import coin.coinzzickmock.feature.market.history.application.implement.MarketHistoryLookupTelemetry;
import coin.coinzzickmock.feature.market.history.application.implement.MarketPersistedCandleReader;
import coin.coinzzickmock.feature.market.latestwindow.application.repository.MarketLatestCandleWindowCache;
import coin.coinzzickmock.feature.market.latestwindow.application.dto.MarketLatestCandleWindowCacheRead;
import coin.coinzzickmock.feature.market.latestwindow.application.dto.MarketLatestCandleWindowKey;
import coin.coinzzickmock.feature.market.latestwindow.application.dto.MarketLatestCandleWindowPage;
import coin.coinzzickmock.feature.market.latestwindow.application.implement.MarketLatestCandleWindowPolicy;
import coin.coinzzickmock.feature.market.latestwindow.application.implement.MarketLatestCandleWindowSingleflight;
import coin.coinzzickmock.feature.market.latestwindow.application.implement.RestVisibleCandleBoundaryResolver;
import coin.coinzzickmock.feature.market.candle.application.dto.GetMarketCandlesQuery;
import coin.coinzzickmock.feature.market.history.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.history.application.repository.MarketHistoryStartupBackfillCursor;
import coin.coinzzickmock.feature.market.candle.application.dto.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.CompletedMarketCandle;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoricalCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.AuthProvider;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.feature.market.catalog.application.gateway.MarketDataGateway;
import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import coin.coinzzickmock.common.cache.CoinCacheNames;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import java.time.Duration;
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
        repository.saveCompletedCandle(completed(
                MarketCandleInterval.ONE_DAY,
                "2026-04-22T00:00:00Z",
                "2026-04-23T00:00:00Z",
                100,
                124,
                99,
                123.5,
                240
        ));
        repository.saveHourlyCandle(hourly(1L, "2026-04-23T00:00:00Z", 300, 301, 299, 300.5, 10));

        GetMarketCandlesService service = service(repository);

        List<MarketCandleResult> results = service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "1D", 2, null));

        assertEquals(1, results.size());
        assertEquals(Instant.parse("2026-04-22T00:00:00Z"), results.get(0).openTime());
        assertEquals(123.5, results.get(0).closePrice(), 0.0001);
        assertEquals(240.0, results.get(0).volume(), 0.0001);
    }

    @Test
    void readsWeeklyCandlesOnUtcCalendarBoundary() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        saveDailyRange(
                repository,
                "2026-04-20T00:00:00Z",
                "2026-04-27T00:00:00Z"
        );
        repository.saveHourlyCandle(hourly(1L, "2026-04-27T00:00:00Z", 300, 301, 299, 300.5, 10));

        GetMarketCandlesService service = service(repository);

        List<MarketCandleResult> results = service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "1W", 2, null));

        assertEquals(1, results.size());
        assertEquals(Instant.parse("2026-04-20T00:00:00Z"), results.get(0).openTime());
        assertEquals(100.5, results.get(0).closePrice(), 0.0001);
        assertEquals(1680.0, results.get(0).volume(), 0.0001);
    }

    @Test
    void dailyRollupUsesLatestCompleteBucketBeforeHistoricalFallback() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        repository.saveCompletedCandle(completed(
                MarketCandleInterval.ONE_DAY,
                "2026-04-27T00:00:00Z",
                "2026-04-28T00:00:00Z",
                100,
                101,
                99,
                100.5,
                240
        ));
        repository.saveCompletedCandle(completed(
                MarketCandleInterval.ONE_DAY,
                "2026-04-28T00:00:00Z",
                "2026-04-29T00:00:00Z",
                100,
                101,
                99,
                100.5,
                240
        ));
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
        saveDailyRange(
                repository,
                "2026-04-06T00:00:00Z",
                "2026-04-13T00:00:00Z"
        );
        saveDailyRange(
                repository,
                "2026-04-13T00:00:00Z",
                "2026-04-20T00:00:00Z"
        );
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
        repository.saveCompletedCandle(completed(
                MarketCandleInterval.ONE_MONTH,
                "2026-02-01T00:00:00Z",
                "2026-03-01T00:00:00Z",
                100,
                101,
                99,
                100.5,
                6_720
        ));
        repository.saveCompletedCandle(completed(
                MarketCandleInterval.ONE_MONTH,
                "2026-03-01T00:00:00Z",
                "2026-04-01T00:00:00Z",
                100,
                101,
                99,
                100.5,
                7_440
        ));
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


    @Test
    void cachesLatestWindowAndAvoidsSecondFullMinuteRead() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        Instant start = Instant.parse("2026-04-21T00:00:00Z");
        for (int minute = 0; minute < 120; minute++) {
            Instant openTime = start.plusSeconds(minute * 60L);
            repository.saveMinuteCandle(minute(1L, openTime.toString(), 100, 101, 99, 100.5, 10));
        }
        InMemoryLatestWindowCache latestWindowCache = new InMemoryLatestWindowCache();
        GetMarketCandlesService service = service(repository, FakeMarketDataGateway.empty(), distributedCacheManager(),
                latestWindowCache);

        List<MarketCandleResult> first = service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "1m", 120, null));
        List<MarketCandleResult> second = service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "1m", 120, null));

        assertEquals(first, second);
        assertEquals(1, latestWindowCache.writeCount);
        assertEquals(1, latestWindowCache.hitCount);
        assertEquals(1, repository.minuteRangeReadCount);
    }

    @Test
    void bypassesLatestWindowCacheForCursorRequest() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        for (int minute = 0; minute < 130; minute++) {
            Instant openTime = Instant.parse("2026-04-21T00:00:00Z").plusSeconds(minute * 60L);
            repository.saveMinuteCandle(minute(1L, openTime.toString(), 100, 101, 99, 100.5, 10));
        }
        InMemoryLatestWindowCache latestWindowCache = new InMemoryLatestWindowCache();
        GetMarketCandlesService service = service(repository, FakeMarketDataGateway.empty(), distributedCacheManager(),
                latestWindowCache);

        service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "1m", 120, Instant.parse("2026-04-21T01:00:00Z")));

        assertEquals(0, latestWindowCache.readCount);
        assertEquals(0, latestWindowCache.writeCount);
    }

    @Test
    void bypassesLatestWindowCacheForUnsupportedLimit() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        for (int minute = 0; minute < 121; minute++) {
            Instant openTime = Instant.parse("2026-04-21T00:00:00Z").plusSeconds(minute * 60L);
            repository.saveMinuteCandle(minute(1L, openTime.toString(), 100, 101, 99, 100.5, 10));
        }
        InMemoryLatestWindowCache latestWindowCache = new InMemoryLatestWindowCache();
        GetMarketCandlesService service = service(repository, FakeMarketDataGateway.empty(), distributedCacheManager(),
                latestWindowCache);

        service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "1m", 121, null));

        assertEquals(0, latestWindowCache.readCount);
        assertEquals(0, latestWindowCache.writeCount);
    }

    @Test
    void writesValidPartialLatestWindowWithoutSizeEqualsLimitRequirement() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        for (int minute = 0; minute < 80; minute++) {
            Instant openTime = Instant.parse("2026-04-21T00:00:00Z").plusSeconds(minute * 60L);
            repository.saveMinuteCandle(minute(1L, openTime.toString(), 100, 101, 99, 100.5, 10));
        }
        InMemoryLatestWindowCache latestWindowCache = new InMemoryLatestWindowCache();
        GetMarketCandlesService service = service(repository, FakeMarketDataGateway.empty(), distributedCacheManager(),
                latestWindowCache);

        List<MarketCandleResult> results = service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "1m", 120, null));

        assertEquals(80, results.size());
        assertEquals(1, latestWindowCache.writeCount);
    }

    @Test
    void skipsLatestWindowWriteWhenReturnedLatestBoundaryDiffers() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        for (int minute = 0; minute < 120; minute++) {
            Instant openTime = Instant.parse("2026-04-21T00:00:00Z").plusSeconds(minute * 60L);
            repository.saveMinuteCandle(minute(1L, openTime.toString(), 100, 101, 99, 100.5, 10));
        }
        InMemoryLatestWindowCache latestWindowCache = new InMemoryLatestWindowCache();
        RestVisibleCandleBoundaryResolver resolver = new RestVisibleCandleBoundaryResolver(repository, new MarketCandleRollupProjector()) {
            @Override
            public Optional<coin.coinzzickmock.feature.market.latestwindow.application.dto.RestVisibleCandleBoundary> resolve(
                    long symbolId,
                    MarketCandleInterval interval
            ) {
                return Optional.of(new coin.coinzzickmock.feature.market.latestwindow.application.dto.RestVisibleCandleBoundary(
                        symbolId,
                        interval,
                        Instant.parse("2026-04-21T00:00:00Z")
                ));
            }
        };
        GetMarketCandlesService service = service(repository, FakeMarketDataGateway.empty(), distributedCacheManager(),
                latestWindowCache, resolver);

        List<MarketCandleResult> results = service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "1m", 120, null));

        assertEquals(120, results.size());
        assertEquals(0, latestWindowCache.writeCount);
    }

    @Test
    void skipsLatestWindowWriteWhenCandidateIsDefensivelyOversized() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        for (int minute = 0; minute < 121; minute++) {
            Instant openTime = Instant.parse("2026-04-21T00:00:00Z").plusSeconds(minute * 60L);
            repository.saveMinuteCandle(minute(1L, openTime.toString(), 100, 101, 99, 100.5, 10));
        }
        InMemoryLatestWindowCache latestWindowCache = new InMemoryLatestWindowCache();
        MarketPersistedCandleReader oversizedReader = new MarketPersistedCandleReader(repository, new MarketCandleRollupProjector()) {
            @Override
            public List<MarketCandleResult> read(
                    long symbolId,
                    MarketCandleInterval interval,
                    int limit,
                    Instant beforeOpenTime
            ) {
                return repository.findMinuteCandles(
                                symbolId,
                                Instant.parse("2026-04-21T00:00:00Z"),
                                Instant.parse("2026-04-21T02:01:00Z")
                        )
                        .stream()
                        .map(candle -> new MarketCandleResult(
                                candle.openTime(),
                                candle.closeTime(),
                                candle.openPrice(),
                                candle.highPrice(),
                                candle.lowPrice(),
                                candle.closePrice(),
                                candle.volume()
                        ))
                        .toList();
            }
        };
        GetMarketCandlesService service = service(
                repository,
                FakeMarketDataGateway.empty(),
                distributedCacheManager(),
                latestWindowCache,
                new RestVisibleCandleBoundaryResolver(repository, new MarketCandleRollupProjector()),
                oversizedReader
        );

        List<MarketCandleResult> results = service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "1m", 120, null));

        assertEquals(121, results.size());
        assertEquals(0, latestWindowCache.writeCount);
    }


    @Test
    void recordsLatestWindowUnavailableTelemetryWhenCacheReadFails() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        for (int minute = 0; minute < 120; minute++) {
            Instant openTime = Instant.parse("2026-04-21T00:00:00Z").plusSeconds(minute * 60L);
            repository.saveMinuteCandle(minute(1L, openTime.toString(), 100, 101, 99, 100.5, 10));
        }
        FakeProviders providers = new FakeProviders(FakeMarketDataGateway.empty());
        GetMarketCandlesService service = service(
                repository,
                providers,
                distributedCacheManager(),
                new FailingLatestWindowCache(),
                new RestVisibleCandleBoundaryResolver(repository, new MarketCandleRollupProjector())
        );

        service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "1m", 120, null));

        RecordedEvent unavailable = providers.telemetry().events.stream()
                .filter(event -> "market.history.latest_window.unavailable".equals(event.eventName()))
                .findFirst()
                .orElseThrow();
        assertEquals("redis", unavailable.tags().get("source"));
        assertEquals("unavailable", unavailable.tags().get("result"));
    }

    @Test
    void recordsLatestWindowWriteUnavailableTelemetryWhenCacheWriteFails() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        for (int minute = 0; minute < 120; minute++) {
            Instant openTime = Instant.parse("2026-04-21T00:00:00Z").plusSeconds(minute * 60L);
            repository.saveMinuteCandle(minute(1L, openTime.toString(), 100, 101, 99, 100.5, 10));
        }
        FakeProviders providers = new FakeProviders(FakeMarketDataGateway.empty());
        GetMarketCandlesService service = service(
                repository,
                providers,
                distributedCacheManager(),
                new FailingLatestWindowCache(),
                new RestVisibleCandleBoundaryResolver(repository, new MarketCandleRollupProjector())
        );

        service.getCandles(new GetMarketCandlesQuery("BTCUSDT", "1m", 120, null));

        RecordedEvent unavailable = providers.telemetry().events.stream()
                .filter(event -> "market.history.latest_window.write_unavailable".equals(event.eventName()))
                .findFirst()
                .orElseThrow();
        assertEquals("redis", unavailable.tags().get("source"));
        assertEquals("write_unavailable", unavailable.tags().get("result"));
    }

    private static GetMarketCandlesService service(InMemoryMarketHistoryRepository repository) {
        return service(repository, FakeMarketDataGateway.empty(), distributedCacheManager());
    }

    private static GetMarketCandlesService service(
            InMemoryMarketHistoryRepository repository,
            FakeMarketDataGateway gateway,
            CacheManager cacheManager
    ) {
        return service(repository, gateway, cacheManager, new InMemoryLatestWindowCache());
    }

    private static GetMarketCandlesService service(
            InMemoryMarketHistoryRepository repository,
            FakeMarketDataGateway gateway,
            CacheManager cacheManager,
            MarketLatestCandleWindowCache latestWindowCache
    ) {
        MarketCandleRollupProjector rollupProjector = new MarketCandleRollupProjector();
        return service(
                repository,
                gateway,
                cacheManager,
                latestWindowCache,
                new RestVisibleCandleBoundaryResolver(repository, rollupProjector)
        );
    }

    private static GetMarketCandlesService service(
            InMemoryMarketHistoryRepository repository,
            FakeMarketDataGateway gateway,
            CacheManager cacheManager,
            MarketLatestCandleWindowCache latestWindowCache,
            RestVisibleCandleBoundaryResolver latestWindowBoundaryResolver
    ) {
        return service(repository, new FakeProviders(gateway), cacheManager, latestWindowCache, latestWindowBoundaryResolver);
    }

    private static GetMarketCandlesService service(
            InMemoryMarketHistoryRepository repository,
            FakeMarketDataGateway gateway,
            CacheManager cacheManager,
            MarketLatestCandleWindowCache latestWindowCache,
            RestVisibleCandleBoundaryResolver latestWindowBoundaryResolver,
            MarketPersistedCandleReader persistedCandleReader
    ) {
        return service(repository, new FakeProviders(gateway), cacheManager, latestWindowCache,
                latestWindowBoundaryResolver, persistedCandleReader);
    }

    private static GetMarketCandlesService service(
            InMemoryMarketHistoryRepository repository,
            FakeProviders providers,
            CacheManager cacheManager
    ) {
        return service(repository, providers, cacheManager, new InMemoryLatestWindowCache(),
                new RestVisibleCandleBoundaryResolver(repository, new MarketCandleRollupProjector()));
    }

    private static GetMarketCandlesService service(
            InMemoryMarketHistoryRepository repository,
            FakeProviders providers,
            CacheManager cacheManager,
            MarketLatestCandleWindowCache latestWindowCache,
            RestVisibleCandleBoundaryResolver latestWindowBoundaryResolver
    ) {
        return service(repository, providers, cacheManager, latestWindowCache, latestWindowBoundaryResolver,
                new MarketPersistedCandleReader(repository, new MarketCandleRollupProjector()));
    }

    private static GetMarketCandlesService service(
            InMemoryMarketHistoryRepository repository,
            FakeProviders providers,
            CacheManager cacheManager,
            MarketLatestCandleWindowCache latestWindowCache,
            RestVisibleCandleBoundaryResolver latestWindowBoundaryResolver,
            MarketPersistedCandleReader persistedCandleReader
    ) {
        MarketHistoricalCandleTelemetry telemetry = new MarketHistoricalCandleTelemetry(providers);
        MarketHistoricalCandleCache cache = new MarketHistoricalCandleCache(
                new MarketHistoricalCandleSegmentPolicy(),
                new MarketHistoricalCandleSegmentStore(telemetry, objectProvider(cacheManager)),
                new MarketHistoricalCandleSegmentFetcher(providers.marketDataGateway, telemetry)
        );
        return new GetMarketCandlesService(
                repository,
                persistedCandleReader,
                new MarketHistoricalCandleAppender(cache),
                new MarketHistoryLookupTelemetry(providers),
                latestWindowBoundaryResolver,
                new MarketLatestCandleWindowPolicy(),
                latestWindowCache,
                new MarketLatestCandleWindowSingleflight()
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

    private static CompletedMarketCandle completed(
            MarketCandleInterval interval,
            String openTime,
            String closeTime,
            double open,
            double high,
            double low,
            double close,
            double volume
    ) {
        Instant openInstant = Instant.parse(openTime);
        Instant closeInstant = Instant.parse(closeTime);
        return new CompletedMarketCandle(
                1L,
                interval,
                openInstant,
                closeInstant,
                open,
                high,
                low,
                close,
                volume,
                volume * close
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

    private static void saveDailyRange(
            InMemoryMarketHistoryRepository repository,
            String fromInclusive,
            String toExclusive
    ) {
        Instant cursor = Instant.parse(fromInclusive);
        Instant end = Instant.parse(toExclusive);
        while (cursor.isBefore(end)) {
            repository.saveCompletedCandle(completed(
                    MarketCandleInterval.ONE_DAY,
                    cursor.toString(),
                    cursor.plusSeconds(86_400).toString(),
                    100,
                    101,
                    99,
                    100.5,
                    240
            ));
            cursor = cursor.plusSeconds(86_400);
        }
    }

    private static class InMemoryMarketHistoryRepository extends coin.coinzzickmock.testsupport.TestMarketHistoryRepository {
        private final Map<String, Long> symbolIds = Map.of("BTCUSDT", 1L);
        private final Map<String, MarketHistoryCandle> minuteCandles = new LinkedHashMap<>();
        private final Map<String, HourlyMarketCandle> hourlyCandles = new LinkedHashMap<>();
        private final Map<String, CompletedMarketCandle> completedCandles = new LinkedHashMap<>();
        private int minuteRangeReadCount;

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
        public List<MarketHistoryStartupBackfillCursor> findStartupBackfillCursors() {
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
            minuteRangeReadCount++;
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
        public Optional<Instant> findLatestCompletedCandleOpenTime(long symbolId, MarketCandleInterval interval) {
            return completedCandles.values().stream()
                    .filter(candle -> candle.symbolId() == symbolId)
                    .filter(candle -> candle.interval() == interval)
                    .map(CompletedMarketCandle::openTime)
                    .max(Instant::compareTo);
        }

        @Override
        public Optional<Instant> findLatestCompletedCandleOpenTimeBefore(
                long symbolId,
                MarketCandleInterval interval,
                Instant beforeExclusive
        ) {
            return completedCandles.values().stream()
                    .filter(candle -> candle.symbolId() == symbolId)
                    .filter(candle -> candle.interval() == interval)
                    .map(CompletedMarketCandle::openTime)
                    .filter(openTime -> openTime.isBefore(beforeExclusive))
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
                    .filter(candle -> candle.symbolId() == symbolId)
                    .filter(candle -> candle.interval() == interval)
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .sorted(java.util.Comparator.comparing(CompletedMarketCandle::openTime))
                    .toList();
        }

        @Override
        public void saveMinuteCandle(MarketHistoryCandle candle) {
            minuteCandles.put(key(candle.symbolId(), candle.openTime()), candle);
        }

        @Override
        public void saveHourlyCandle(HourlyMarketCandle candle) {
            hourlyCandles.put(key(candle.symbolId(), candle.openTime()), candle);
            saveCompletedCandle(CompletedMarketCandle.fromHourly(candle));
        }

        @Override
        public void saveCompletedCandle(CompletedMarketCandle candle) {
            completedCandles.put(key(candle.symbolId(), candle.interval(), candle.openTime()), candle);
        }

        private String key(long symbolId, Instant openTime) {
            return symbolId + ":" + openTime;
        }

        private String key(long symbolId, MarketCandleInterval interval, Instant openTime) {
            return symbolId + ":" + interval + ":" + openTime;
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

    private static class InMemoryLatestWindowCache implements MarketLatestCandleWindowCache {
        private final Map<String, MarketLatestCandleWindowPage> pages = new LinkedHashMap<>();
        private int readCount;
        private int hitCount;
        private int writeCount;
        private Duration lastTtl;

        @Override
        public MarketLatestCandleWindowCacheRead read(MarketLatestCandleWindowKey key) {
            readCount++;
            MarketLatestCandleWindowPage page = pages.get(key.cacheKey());
            if (page != null) {
                hitCount++;
                return MarketLatestCandleWindowCacheRead.hit(page);
            }
            return MarketLatestCandleWindowCacheRead.miss();
        }

        @Override
        public boolean write(MarketLatestCandleWindowKey key, MarketLatestCandleWindowPage page, Duration ttl) {
            writeCount++;
            lastTtl = ttl;
            pages.put(key.cacheKey(), page);
            return true;
        }
    }

    private static class FailingLatestWindowCache implements MarketLatestCandleWindowCache {
        @Override
        public MarketLatestCandleWindowCacheRead read(MarketLatestCandleWindowKey key) {
            return MarketLatestCandleWindowCacheRead.unavailable();
        }

        @Override
        public boolean write(MarketLatestCandleWindowKey key, MarketLatestCandleWindowPage page, Duration ttl) {
            return false;
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
