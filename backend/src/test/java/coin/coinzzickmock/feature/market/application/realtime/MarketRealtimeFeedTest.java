package coin.coinzzickmock.feature.market.application.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.FundingSchedule;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import jakarta.annotation.PostConstruct;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.infrastructure.config.CoinCacheNames;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.scheduling.annotation.Scheduled;

class MarketRealtimeFeedTest {
    @Test
    void doesNotExposeSseSubscriptionLifecycleMethods() {
        List<String> methodNames = Arrays.stream(MarketRealtimeFeed.class.getDeclaredMethods())
                .map(method -> method.getName())
                .toList();

        assertFalse(methodNames.contains("subscribe"));
        assertFalse(methodNames.contains("unsubscribe"));
    }

    @Test
    void doesNotDeclarePostConstructStartupHook() {
        long postConstructMethodCount = Arrays.stream(MarketRealtimeFeed.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostConstruct.class))
                .count();

        assertEquals(0, postConstructMethodCount);
    }

    @Test
    void doesNotDeclareScheduledTrigger() {
        long scheduledMethodCount = Arrays.stream(MarketRealtimeFeed.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Scheduled.class))
                .count();

        assertEquals(0, scheduledMethodCount);
    }

    @Test
    void returnsLatestSupportedMarketSnapshotsFromCache() {
        FakeMarketDataGateway marketDataGateway = new FakeMarketDataGateway(List.of(
                snapshot("BTCUSDT", 101000, 100950, 100900, 0.0001, 3.2),
                snapshot("ETHUSDT", 3300, 3295, 3290, 0.00008, 2.1)
        ));
        RecordingApplicationEventPublisher applicationEventPublisher = new RecordingApplicationEventPublisher();
        MarketRealtimeFeed feed = newFeed(
                marketDataGateway,
                new InMemoryMarketHistoryRepository(),
                applicationEventPublisher
        );

        feed.refreshSupportedMarkets();

        List<MarketSummaryResult> markets = feed.getSupportedMarkets();

        assertEquals(2, markets.size());
        assertEquals(101000, feed.getMarket("BTCUSDT").lastPrice(), 0.0001);
        assertEquals(100900, feed.getMarket("BTCUSDT").indexPrice(), 0.0001);
        assertEquals(1_010_000_000, feed.getMarket("BTCUSDT").turnover24hUsdt(), 0.0001);
        assertEquals(2, applicationEventPublisher.events().size());
        assertInstanceOf(MarketSummaryUpdatedEvent.class, applicationEventPublisher.events().get(0));
    }

    @Test
    void warmsCacheOnReadSideCacheMissWithoutPersistingHistory() {
        FakeMarketDataGateway marketDataGateway = new FakeMarketDataGateway(List.of(
                snapshot("BTCUSDT", 101000, 100950, 100900, 0.0001, 3.2)
        ));
        InMemoryMarketHistoryRepository marketHistoryRepository = new InMemoryMarketHistoryRepository();
        RecordingApplicationEventPublisher applicationEventPublisher = new RecordingApplicationEventPublisher();
        MarketRealtimeFeed feed = newFeed(
                marketDataGateway,
                marketHistoryRepository,
                applicationEventPublisher
        );

        List<MarketSummaryResult> markets = feed.getSupportedMarkets();

        assertEquals(1, markets.size());
        assertEquals(1_010_000_000, markets.get(0).turnover24hUsdt(), 0.0001);
        assertEquals(1, applicationEventPublisher.events().size());
        assertEquals(0, marketHistoryRepository.minuteCandleCount());
        assertEquals(0, marketHistoryRepository.hourlyCandleCount());
    }

    @Test
    void publishesUpdatedSnapshotEventsWhenMarketsRefresh() {
        FakeMarketDataGateway marketDataGateway = new FakeMarketDataGateway(List.of(
                snapshot("BTCUSDT", 101000, 100950, 100900, 0.0001, 3.2),
                snapshot("ETHUSDT", 3300, 3295, 3290, 0.00008, 2.1)
        ));
        RecordingApplicationEventPublisher applicationEventPublisher = new RecordingApplicationEventPublisher();
        MarketRealtimeFeed feed = newFeed(
                marketDataGateway,
                new InMemoryMarketHistoryRepository(),
                applicationEventPublisher
        );

        feed.refreshSupportedMarkets();
        applicationEventPublisher.clear();

        marketDataGateway.replaceSnapshots(List.of(
                snapshot("BTCUSDT", 102500, 102450, 102400, 0.00012, 4.0),
                snapshot("ETHUSDT", 3320, 3312, 3308, 0.00009, 2.4)
        ));

        feed.refreshSupportedMarkets();

        List<MarketSummaryUpdatedEvent> events = applicationEventPublisher.events().stream()
                .map(MarketSummaryUpdatedEvent.class::cast)
                .filter(event -> event.result().symbol().equals("BTCUSDT"))
                .toList();

        assertEquals(1, events.size());
        assertEquals(102500, events.get(0).result().lastPrice(), 0.0001);
        assertEquals(101000, events.get(0).previousLastPrice(), 0.0001);
        assertEquals(MarketPriceMovementDirection.UP, events.get(0).direction());
        assertEquals(102400, events.get(0).result().indexPrice(), 0.0001);
        assertEquals(1_025_000_000, events.get(0).result().turnover24hUsdt(), 0.0001);
    }

    @Test
    void doesNotLoadMinuteCandlesFromTickerRefreshes() {
        FakeMarketDataGateway marketDataGateway = new FakeMarketDataGateway(List.of(
                snapshot("BTCUSDT", 101000, 100950, 100900, 0.0001, 3.2)
        ));
        marketDataGateway.replaceMinuteCandles("BTCUSDT", List.of(
                minuteCandle("2026-04-17T06:00:00Z", 101000, 102500, 100500, 102000, 12.5, 1_275_000)
        ));
        MarketRealtimeFeed feed = newFeed(
                marketDataGateway,
                new InMemoryMarketHistoryRepository(),
                new RecordingApplicationEventPublisher()
        );

        feed.refreshSupportedMarkets();
        feed.refreshSupportedMarkets();
        feed.refreshSupportedMarkets();

        assertEquals(0, marketDataGateway.minuteHistoryCalls());
    }

    @Test
    void recordsClosedProviderMinuteCandlesWithVolumeWithoutPublishingPartialHourlyHistory() {
        FakeMarketDataGateway marketDataGateway = new FakeMarketDataGateway(List.of(
                snapshot("BTCUSDT", 101000, 100950, 100900, 0.0001, 3.2),
                snapshot("ETHUSDT", 3300, 3295, 3290, 0.00008, 2.1)
        ));
        marketDataGateway.replaceMinuteCandles("BTCUSDT", List.of(
                minuteCandle("2026-04-17T06:00:00Z", 101000, 102500, 100500, 102000, 12.5, 1_275_000)
        ));
        marketDataGateway.replaceMinuteCandles("ETHUSDT", List.of(
                minuteCandle("2026-04-17T06:00:00Z", 3300, 3330, 3280, 3310, 45.25, 149_777.5)
        ));
        InMemoryMarketHistoryRepository marketHistoryRepository = new InMemoryMarketHistoryRepository();
        TestMarketRuntime runtime = newRuntime(
                marketDataGateway,
                marketHistoryRepository,
                new RecordingApplicationEventPublisher()
        );

        runtime.feed().refreshSupportedMarkets();
        runtime.listener().onMinuteClosed(new MarketMinuteClosedEvent(
                Instant.parse("2026-04-17T06:00:00Z"),
                Instant.parse("2026-04-17T06:01:00Z")
        ));

        MarketHistoryCandle firstMinute = marketHistoryRepository.minuteCandle(1L, "2026-04-17T06:00:00Z");

        assertEquals(101000, firstMinute.openPrice(), 0.0001);
        assertEquals(102500, firstMinute.highPrice(), 0.0001);
        assertEquals(100500, firstMinute.lowPrice(), 0.0001);
        assertEquals(102000, firstMinute.closePrice(), 0.0001);
        assertEquals(12.5, firstMinute.volume(), 0.0001);
        assertEquals(1_275_000, firstMinute.quoteVolume(), 0.0001);
        assertEquals(0, marketHistoryRepository.hourlyCandleCount());
    }

    @Test
    void retriesClosedMinuteHistoryWhenProviderHasNoCandleYet() {
        FakeMarketDataGateway marketDataGateway = new FakeMarketDataGateway(List.of(
                snapshot("BTCUSDT", 101000, 100950, 100900, 0.0001, 3.2)
        ));
        InMemoryMarketHistoryRepository marketHistoryRepository = new InMemoryMarketHistoryRepository();
        TestMarketRuntime runtime = newRuntime(
                marketDataGateway,
                marketHistoryRepository,
                new RecordingApplicationEventPublisher()
        );

        runtime.feed().refreshSupportedMarkets();
        runtime.listener().onMinuteClosed(new MarketMinuteClosedEvent(
                Instant.parse("2026-04-17T06:00:00Z"),
                Instant.parse("2026-04-17T06:01:00Z")
        ));

        assertEquals(0, marketHistoryRepository.minuteCandleCount());
        assertEquals(1, marketDataGateway.minuteHistoryCalls());
        assertEquals(1, runtime.retryRegistry().pendingRetries().size());

        marketDataGateway.replaceMinuteCandles("BTCUSDT", List.of(
                minuteCandle("2026-04-17T06:00:00Z", 101000, 102500, 100500, 102000, 12.5, 1_275_000)
        ));
        runtime.retryProcessor().retryPending();

        MarketHistoryCandle firstMinute = marketHistoryRepository.minuteCandle(1L, "2026-04-17T06:00:00Z");

        assertEquals(1, marketHistoryRepository.minuteCandleCount());
        assertEquals(2, marketDataGateway.minuteHistoryCalls());
        assertEquals(12.5, firstMinute.volume(), 0.0001);
        assertEquals(0, runtime.retryRegistry().pendingRetries().size());
    }

    @Test
    void keepsRetryPendingWhenProviderCandleCannotBeMatchedToPersistedSymbol() {
        FakeMarketDataGateway marketDataGateway = new FakeMarketDataGateway(List.of(
                snapshot("XRPUSDT", 1.2, 1.19, 1.18, 0.0001, 3.2)
        ));
        marketDataGateway.replaceMinuteCandles("XRPUSDT", List.of(
                minuteCandle("2026-04-17T06:00:00Z", 1.1, 1.3, 1.0, 1.2, 500, 600)
        ));
        InMemoryMarketHistoryRepository marketHistoryRepository = new InMemoryMarketHistoryRepository();
        TestMarketRuntime runtime = newRuntime(
                marketDataGateway,
                marketHistoryRepository,
                new RecordingApplicationEventPublisher()
        );

        runtime.feed().refreshSupportedMarkets();
        runtime.listener().onMinuteClosed(new MarketMinuteClosedEvent(
                Instant.parse("2026-04-17T06:00:00Z"),
                Instant.parse("2026-04-17T06:01:00Z")
        ));

        assertEquals(0, marketHistoryRepository.minuteCandleCount());
        assertEquals(1, runtime.retryRegistry().pendingRetries().size());
    }

    @Test
    void keepsRetryPendingWhenPersistenceFailsAfterProviderReturnsCandle() {
        FakeMarketDataGateway marketDataGateway = new FakeMarketDataGateway(List.of(
                snapshot("BTCUSDT", 101000, 100950, 100900, 0.0001, 3.2)
        ));
        marketDataGateway.replaceMinuteCandles("BTCUSDT", List.of(
                minuteCandle("2026-04-17T06:00:00Z", 101000, 102500, 100500, 102000, 12.5, 1_275_000)
        ));
        TestMarketRuntime runtime = newRuntime(
                marketDataGateway,
                new FailingSaveMarketHistoryRepository(),
                new RecordingApplicationEventPublisher()
        );

        runtime.feed().refreshSupportedMarkets();
        runtime.listener().onMinuteClosed(new MarketMinuteClosedEvent(
                Instant.parse("2026-04-17T06:00:00Z"),
                Instant.parse("2026-04-17T06:01:00Z")
        ));

        assertEquals(1, runtime.retryRegistry().pendingRetries().size());
    }

    @Test
    void skipsPublishAndPersistWhenRefreshReturnsNoMarkets() {
        FakeMarketDataGateway marketDataGateway = new FakeMarketDataGateway(List.of());
        InMemoryMarketHistoryRepository marketHistoryRepository = new InMemoryMarketHistoryRepository();
        RecordingApplicationEventPublisher applicationEventPublisher = new RecordingApplicationEventPublisher();
        MarketRealtimeFeed feed = newFeed(
                marketDataGateway,
                marketHistoryRepository,
                applicationEventPublisher
        );

        feed.refreshSupportedMarkets();

        assertEquals(0, applicationEventPublisher.events().size());
        assertEquals(0, marketHistoryRepository.minuteCandleCount());
        assertEquals(0, marketHistoryRepository.hourlyCandleCount());
    }

    @Test
    void rejectsUnsupportedSymbolWithoutDirectMarketLookup() {
        CountingMarketDataGateway marketDataGateway = new CountingMarketDataGateway(List.of(
                snapshot("BTCUSDT", 101000, 100950, 100900, 0.0001, 3.2)
        ));
        MarketRealtimeFeed feed = newFeed(
                marketDataGateway,
                new InMemoryMarketHistoryRepository(),
                new RecordingApplicationEventPublisher()
        );

        CoreException thrown = assertThrows(CoreException.class, () -> feed.getMarket("ETHUSDT"));

        assertEquals(ErrorCode.MARKET_NOT_FOUND, thrown.errorCode());
        assertEquals(0, marketDataGateway.loadMarketCalls());
    }

    private static MarketSnapshot snapshot(
            String symbol,
            double lastPrice,
            double markPrice,
            double indexPrice,
            double fundingRate,
            double change24h
    ) {
        return new MarketSnapshot(
                symbol,
                symbol + " Perpetual",
                lastPrice,
                markPrice,
                indexPrice,
                fundingRate,
                change24h,
                lastPrice * 10_000
        );
    }

    private static MarketMinuteCandleSnapshot minuteCandle(
            String openTime,
            double openPrice,
            double highPrice,
            double lowPrice,
            double closePrice,
            double volume,
            double quoteVolume
    ) {
        Instant candleOpenTime = Instant.parse(openTime);
        return new MarketMinuteCandleSnapshot(
                candleOpenTime,
                candleOpenTime.plus(1, ChronoUnit.MINUTES),
                openPrice,
                highPrice,
                lowPrice,
                closePrice,
                volume,
                quoteVolume
        );
    }

    private static MarketRealtimeFeed newFeed(
            FakeMarketDataGateway marketDataGateway,
            InMemoryMarketHistoryRepository marketHistoryRepository,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        MarketSnapshotStore marketSnapshotStore = newSnapshotStore();
        return new MarketRealtimeFeed(
                new MarketSupportedMarketRefresher(
                        new FakeProviders(marketDataGateway),
                        marketSnapshotStore,
                        applicationEventPublisher,
                        defaultFundingScheduleLookup()
                ),
                marketSnapshotStore
        );
    }

    private static TestMarketRuntime newRuntime(
            FakeMarketDataGateway marketDataGateway,
            InMemoryMarketHistoryRepository marketHistoryRepository,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        MarketHistoryRecorder marketHistoryRecorder = new MarketHistoryRecorder(marketHistoryRepository);
        MarketSnapshotStore marketSnapshotStore = newSnapshotStore();
        MarketHistoryPersistenceCoordinator marketHistoryPersistenceCoordinator =
                new MarketHistoryPersistenceCoordinator(marketDataGateway, marketHistoryRecorder);
        MarketHistoryRetryRegistry marketHistoryRetryRegistry = new MarketHistoryRetryRegistry();
        MarketRealtimeFeed feed = new MarketRealtimeFeed(
                new MarketSupportedMarketRefresher(
                        new FakeProviders(marketDataGateway),
                        marketSnapshotStore,
                        applicationEventPublisher,
                        defaultFundingScheduleLookup()
                ),
                marketSnapshotStore
        );
        MarketMinuteCandleHistoryListener listener = new MarketMinuteCandleHistoryListener(
                marketSnapshotStore,
                marketHistoryPersistenceCoordinator,
                marketHistoryRetryRegistry
        );
        MarketHistoryRetryProcessor retryProcessor = new MarketHistoryRetryProcessor(
                marketHistoryRetryRegistry,
                marketHistoryPersistenceCoordinator
        );
        return new TestMarketRuntime(feed, listener, retryProcessor, marketHistoryRetryRegistry);
    }

    private record TestMarketRuntime(
            MarketRealtimeFeed feed,
            MarketMinuteCandleHistoryListener listener,
            MarketHistoryRetryProcessor retryProcessor,
            MarketHistoryRetryRegistry retryRegistry
    ) {
    }

    private static MarketSnapshotStore newSnapshotStore() {
        return new MarketSnapshotStore(new ConcurrentMapCacheManager(
                CoinCacheNames.MARKET_SNAPSHOT_LOCAL_CACHE,
                CoinCacheNames.MARKET_SUPPORTED_SYMBOLS_LOCAL_CACHE
        ));
    }

    private static MarketFundingScheduleLookup defaultFundingScheduleLookup() {
        MarketFundingScheduleLookup provider = mock(MarketFundingScheduleLookup.class);
        when(provider.scheduleFor(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(FundingSchedule.defaultUsdtPerpetual());
        return provider;
    }

    private static class FakeMarketDataGateway extends coin.coinzzickmock.testsupport.TestMarketDataGateway {
        private List<MarketSnapshot> supportedMarkets;
        private final Map<String, List<MarketMinuteCandleSnapshot>> minuteCandles = new LinkedHashMap<>();
        private int minuteHistoryCalls;

        private FakeMarketDataGateway(List<MarketSnapshot> supportedMarkets) {
            this.supportedMarkets = new ArrayList<>(supportedMarkets);
        }

        @Override
        public List<MarketSnapshot> loadSupportedMarkets() {
            return List.copyOf(supportedMarkets);
        }

        @Override
        public MarketSnapshot loadMarket(String symbol) {
            return supportedMarkets.stream()
                    .filter(snapshot -> snapshot.symbol().equals(symbol))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<MarketMinuteCandleSnapshot> loadMinuteCandles(String symbol, Instant fromInclusive, Instant toExclusive) {
            minuteHistoryCalls++;
            return minuteCandles.getOrDefault(symbol, List.of()).stream()
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .toList();
        }

        private void replaceSnapshots(List<MarketSnapshot> snapshots) {
            this.supportedMarkets = new ArrayList<>(snapshots);
        }

        private void replaceMinuteCandles(String symbol, List<MarketMinuteCandleSnapshot> candles) {
            minuteCandles.put(symbol, new ArrayList<>(candles));
        }

        private int minuteHistoryCalls() {
            return minuteHistoryCalls;
        }
    }

    private static class CountingMarketDataGateway extends FakeMarketDataGateway {
        private int loadMarketCalls;

        private CountingMarketDataGateway(List<MarketSnapshot> supportedMarkets) {
            super(supportedMarkets);
        }

        @Override
        public MarketSnapshot loadMarket(String symbol) {
            loadMarketCalls++;
            return super.loadMarket(symbol);
        }

        private int loadMarketCalls() {
            return loadMarketCalls;
        }
    }

    private static class FakeProviders implements Providers {
        private final FakeMarketDataGateway marketDataGateway;

        private FakeProviders(FakeMarketDataGateway marketDataGateway) {
            this.marketDataGateway = marketDataGateway;
        }

        @Override
        public AuthProvider auth() {
            return new coin.coinzzickmock.testsupport.TestAuthProvider() {
                @Override
                public Actor currentActor() {
                    return new Actor(1L, "demo-member", "demo@coinzzickmock.dev", "Demo");
                }

                @Override
                public boolean isAuthenticated() {
                    return true;
                }
            };
        }

        @Override
        public ConnectorProvider connector() {
            return () -> marketDataGateway;
        }

        @Override
        public TelemetryProvider telemetry() {
            return new coin.coinzzickmock.testsupport.TestTelemetryProvider() {
                @Override
                public void recordUseCase(String useCaseName) {
                }

                @Override
                public void recordFailure(String useCaseName, String reason) {
                }
            };
        }

        @Override
        public FeatureFlagProvider featureFlags() {
            return key -> true;
        }
    }

    private static class InMemoryMarketHistoryRepository extends coin.coinzzickmock.testsupport.TestMarketHistoryRepository {
        private final Map<String, Long> symbolIds = Map.of("BTCUSDT", 1L, "ETHUSDT", 2L);
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
            return symbolIds.entrySet().stream()
                    .map(entry -> new StartupBackfillCursor(
                            entry.getValue(),
                            entry.getKey(),
                            findLatestMinuteCandleOpenTime(entry.getValue()).orElse(null)
                    ))
                    .toList();
        }

        @Override
        public Optional<MarketHistoryCandle> findMinuteCandle(long symbolId, Instant openTime) {
            return Optional.ofNullable(minuteCandles.get(key(symbolId, openTime)));
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

        private MarketHistoryCandle minuteCandle(long symbolId, String openTime) {
            String key = key(symbolId, Instant.parse(openTime));
            MarketHistoryCandle candle = minuteCandles.get(key);
            if (candle == null) {
                throw new AssertionError("Missing minute candle for key=" + key);
            }
            return candle;
        }

        private String key(long symbolId, Instant openTime) {
            return symbolId + ":" + openTime;
        }

        private int minuteCandleCount() {
            return minuteCandles.size();
        }

        private int hourlyCandleCount() {
            return hourlyCandles.size();
        }
    }

    private static class FailingSaveMarketHistoryRepository extends InMemoryMarketHistoryRepository {
        @Override
        public void saveMinuteCandle(MarketHistoryCandle candle) {
            throw new IllegalStateException("failed to persist minute candle");
        }
    }

    private static class RecordingApplicationEventPublisher implements ApplicationEventPublisher {
        private final List<Object> events = new CopyOnWriteArrayList<>();

        @Override
        public void publishEvent(Object event) {
            events.add(event);
        }

        private List<Object> events() {
            return List.copyOf(events);
        }

        private void clear() {
            events.clear();
        }
    }
}
