package coin.coinzzickmock.feature.market.application.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.feature.market.domain.FundingSchedule;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.market.job.MarketStartupWarmupReadyEventListener;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import coin.coinzzickmock.providers.infrastructure.config.CoinCacheNames;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationEventPublisher;

class MarketStartupWarmupReadyEventListenerTest {
    @Test
    void warmsCacheAndPublishesSnapshotEventsAfterApplicationReady() {
        FakeMarketDataGateway marketDataGateway = new FakeMarketDataGateway(List.of(
                snapshot("BTCUSDT", 101000, 100950, 100900, 0.0001, 3.2)
        ));
        RecordingApplicationEventPublisher applicationEventPublisher = new RecordingApplicationEventPublisher();
        MarketSnapshotStore marketSnapshotStore = new MarketSnapshotStore(new ConcurrentMapCacheManager(
                CoinCacheNames.MARKET_SNAPSHOT_LOCAL_CACHE,
                CoinCacheNames.MARKET_SUPPORTED_SYMBOLS_LOCAL_CACHE
        ));
        MarketSupportedMarketRefresher marketSupportedMarketRefresher = new MarketSupportedMarketRefresher(
                new FakeProviders(marketDataGateway),
                marketSnapshotStore,
                applicationEventPublisher,
                defaultFundingScheduleLookup()
        );
        MarketStartupWarmupReadyEventListener listener =
                new MarketStartupWarmupReadyEventListener(marketSupportedMarketRefresher);

        listener.warmupSupportedMarketsAfterApplicationReady();

        assertEquals(1, marketDataGateway.supportedMarketLoadCalls());
        assertEquals(1, marketSnapshotStore.getSupportedMarkets().size());
        assertEquals(1, applicationEventPublisher.events().size());
    }

    private static MarketFundingScheduleLookup defaultFundingScheduleLookup() {
        MarketFundingScheduleLookup provider = mock(MarketFundingScheduleLookup.class);
        when(provider.scheduleFor(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(FundingSchedule.defaultUsdtPerpetual());
        return provider;
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

    private static class FakeMarketDataGateway extends coin.coinzzickmock.testsupport.TestMarketDataGateway {
        private final List<MarketSnapshot> supportedMarkets;
        private int supportedMarketLoadCalls;

        private FakeMarketDataGateway(List<MarketSnapshot> supportedMarkets) {
            this.supportedMarkets = new ArrayList<>(supportedMarkets);
        }

        @Override
        public List<MarketSnapshot> loadSupportedMarkets() {
            supportedMarketLoadCalls++;
            return List.copyOf(supportedMarkets);
        }

        @Override
        public MarketSnapshot loadMarket(String symbol) {
            return supportedMarkets.stream()
                    .filter(snapshot -> snapshot.symbol().equals(symbol))
                    .findFirst()
                    .orElse(null);
        }

        private int supportedMarketLoadCalls() {
            return supportedMarketLoadCalls;
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

    private static class RecordingApplicationEventPublisher implements ApplicationEventPublisher {
        private final List<Object> events = new CopyOnWriteArrayList<>();

        @Override
        public void publishEvent(Object event) {
            events.add(event);
        }

        private List<Object> events() {
            return List.copyOf(events);
        }
    }
}
