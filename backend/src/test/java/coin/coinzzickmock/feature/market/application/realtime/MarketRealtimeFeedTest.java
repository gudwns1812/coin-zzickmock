package coin.coinzzickmock.feature.market.application.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import java.time.Instant;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

class MarketRealtimeFeedTest {
    @Test
    void returnsLatestSupportedMarketSnapshotsFromCache() {
        FakeMarketDataGateway marketDataGateway = new FakeMarketDataGateway(List.of(
                snapshot("BTCUSDT", 101000, 100950, 100900, 0.0001, 3.2),
                snapshot("ETHUSDT", 3300, 3295, 3290, 0.00008, 2.1)
        ));
        MarketRealtimeFeed feed = new MarketRealtimeFeed(
                new FakeProviders(marketDataGateway),
                new MarketHistoryRecorder(new InMemoryMarketHistoryRepository())
        );

        feed.refreshSupportedMarkets();

        List<MarketSummaryResult> markets = feed.getSupportedMarkets();

        assertEquals(2, markets.size());
        assertEquals(101000, feed.getMarket("BTCUSDT").lastPrice(), 0.0001);
        assertEquals(100900, feed.getMarket("BTCUSDT").indexPrice(), 0.0001);
    }

    @Test
    void publishesUpdatedSnapshotToSubscribersWhenMarketsRefresh() {
        FakeMarketDataGateway marketDataGateway = new FakeMarketDataGateway(List.of(
                snapshot("BTCUSDT", 101000, 100950, 100900, 0.0001, 3.2),
                snapshot("ETHUSDT", 3300, 3295, 3290, 0.00008, 2.1)
        ));
        MarketRealtimeFeed feed = new MarketRealtimeFeed(
                new FakeProviders(marketDataGateway),
                new MarketHistoryRecorder(new InMemoryMarketHistoryRepository())
        );
        List<MarketSummaryResult> events = new CopyOnWriteArrayList<>();

        feed.refreshSupportedMarkets();
        feed.subscribe("BTCUSDT", events::add);

        marketDataGateway.replaceSnapshots(List.of(
                snapshot("BTCUSDT", 102500, 102450, 102400, 0.00012, 4.0),
                snapshot("ETHUSDT", 3320, 3312, 3308, 0.00009, 2.4)
        ));

        feed.refreshSupportedMarkets();

        assertEquals(1, events.size());
        assertEquals(102500, events.get(0).lastPrice(), 0.0001);
        assertEquals(102400, events.get(0).indexPrice(), 0.0001);
    }

    @Test
    void aggregatesLatestSnapshotsIntoMinuteAndHourlyHistory() {
        FakeMarketDataGateway marketDataGateway = new FakeMarketDataGateway(List.of(
                snapshot("BTCUSDT", 101000, 100950, 100900, 0.0001, 3.2),
                snapshot("ETHUSDT", 3300, 3295, 3290, 0.00008, 2.1)
        ));
        InMemoryMarketHistoryRepository marketHistoryRepository = new InMemoryMarketHistoryRepository();
        MarketRealtimeFeed feed = new MarketRealtimeFeed(
                new FakeProviders(marketDataGateway),
                new MarketHistoryRecorder(marketHistoryRepository)
        );

        feed.refreshSupportedMarkets(Instant.parse("2026-04-17T06:00:15Z"));

        marketDataGateway.replaceSnapshots(List.of(
                snapshot("BTCUSDT", 102500, 102450, 102400, 0.00012, 4.0),
                snapshot("ETHUSDT", 3320, 3312, 3308, 0.00009, 2.4)
        ));
        feed.refreshSupportedMarkets(Instant.parse("2026-04-17T06:00:45Z"));

        marketDataGateway.replaceSnapshots(List.of(
                snapshot("BTCUSDT", 100500, 100450, 100400, 0.00007, -1.2),
                snapshot("ETHUSDT", 3280, 3275, 3270, 0.00004, -0.6)
        ));
        feed.refreshSupportedMarkets(Instant.parse("2026-04-17T06:01:10Z"));

        MarketHistoryCandle firstMinute = marketHistoryRepository.minuteCandle(1L, "2026-04-17T06:00:00Z");
        MarketHistoryCandle secondMinute = marketHistoryRepository.minuteCandle(1L, "2026-04-17T06:01:00Z");
        HourlyMarketCandle firstHour = marketHistoryRepository.hourlyCandle(1L, "2026-04-17T06:00:00Z");

        assertEquals(101000, firstMinute.openPrice(), 0.0001);
        assertEquals(102500, firstMinute.highPrice(), 0.0001);
        assertEquals(101000, firstMinute.lowPrice(), 0.0001);
        assertEquals(102500, firstMinute.closePrice(), 0.0001);
        assertEquals(100500, secondMinute.openPrice(), 0.0001);
        assertEquals(100500, secondMinute.closePrice(), 0.0001);
        assertEquals(101000, firstHour.openPrice(), 0.0001);
        assertEquals(102500, firstHour.highPrice(), 0.0001);
        assertEquals(100500, firstHour.lowPrice(), 0.0001);
        assertEquals(100500, firstHour.closePrice(), 0.0001);
    }

    private static MarketSnapshot snapshot(
            String symbol,
            double lastPrice,
            double markPrice,
            double indexPrice,
            double fundingRate,
            double change24h
    ) {
        return new MarketSnapshot(symbol, symbol + " Perpetual", lastPrice, markPrice, indexPrice, fundingRate, change24h);
    }

    private static class FakeMarketDataGateway implements MarketDataGateway {
        private List<MarketSnapshot> supportedMarkets;

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

        private void replaceSnapshots(List<MarketSnapshot> snapshots) {
            this.supportedMarkets = new ArrayList<>(snapshots);
        }
    }

    private static class FakeProviders implements Providers {
        private final FakeMarketDataGateway marketDataGateway;

        private FakeProviders(FakeMarketDataGateway marketDataGateway) {
            this.marketDataGateway = marketDataGateway;
        }

        @Override
        public AuthProvider auth() {
            return new AuthProvider() {
                @Override
                public Actor currentActor() {
                    return new Actor("demo-member", "demo@coinzzickmock.dev", "Demo");
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
            return new TelemetryProvider() {
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

    private static class InMemoryMarketHistoryRepository implements MarketHistoryRepository {
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
        public List<MarketHistoryCandle> findMinuteCandles(long symbolId, Instant fromInclusive, Instant toExclusive) {
            return minuteCandles.values().stream()
                    .filter(candle -> candle.symbolId() == symbolId)
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .sorted(java.util.Comparator.comparing(MarketHistoryCandle::openTime))
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
            return minuteCandles.get(key(symbolId, Instant.parse(openTime)));
        }

        private HourlyMarketCandle hourlyCandle(long symbolId, String openTime) {
            return hourlyCandles.get(key(symbolId, Instant.parse(openTime)));
        }

        private String key(long symbolId, Instant openTime) {
            return symbolId + ":" + openTime;
        }
    }
}
