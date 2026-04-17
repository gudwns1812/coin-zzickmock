package coin.coinzzickmock.feature.market.application.service;

import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketRealtimeServiceTest {
    @Test
    void returnsLatestSupportedMarketSnapshotsFromCache() {
        FakeMarketDataGateway marketDataGateway = new FakeMarketDataGateway(List.of(
                snapshot("BTCUSDT", 101000, 100950, 100900, 0.0001, 3.2),
                snapshot("ETHUSDT", 3300, 3295, 3290, 0.00008, 2.1)
        ));
        MarketRealtimeService service = new MarketRealtimeService(new FakeProviders(marketDataGateway));

        service.refreshSupportedMarkets();

        List<MarketSummaryResult> markets = service.getSupportedMarkets();

        assertEquals(2, markets.size());
        assertEquals(101000, service.getMarket("BTCUSDT").lastPrice(), 0.0001);
        assertEquals(100900, service.getMarket("BTCUSDT").indexPrice(), 0.0001);
    }

    @Test
    void publishesUpdatedSnapshotToSubscribersWhenMarketsRefresh() {
        FakeMarketDataGateway marketDataGateway = new FakeMarketDataGateway(List.of(
                snapshot("BTCUSDT", 101000, 100950, 100900, 0.0001, 3.2),
                snapshot("ETHUSDT", 3300, 3295, 3290, 0.00008, 2.1)
        ));
        MarketRealtimeService service = new MarketRealtimeService(new FakeProviders(marketDataGateway));
        List<MarketSummaryResult> events = new CopyOnWriteArrayList<>();

        service.refreshSupportedMarkets();
        service.subscribe("BTCUSDT", events::add);

        marketDataGateway.replaceSnapshots(List.of(
                snapshot("BTCUSDT", 102500, 102450, 102400, 0.00012, 4.0),
                snapshot("ETHUSDT", 3320, 3312, 3308, 0.00009, 2.4)
        ));

        service.refreshSupportedMarkets();

        assertEquals(1, events.size());
        assertEquals(102500, events.get(0).lastPrice(), 0.0001);
        assertEquals(102400, events.get(0).indexPrice(), 0.0001);
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
}
