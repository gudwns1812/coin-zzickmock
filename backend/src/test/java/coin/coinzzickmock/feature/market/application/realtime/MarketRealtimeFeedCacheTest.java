package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.infrastructure.config.CoinCacheNames;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = {CoinZzickmockApplication.class, MarketRealtimeFeedCacheTest.MarketRealtimeFeedCacheTestConfiguration.class},
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "spring.task.scheduling.enabled=false"
        }
)
@ActiveProfiles("test")
class MarketRealtimeFeedCacheTest {
    @Autowired
    private MarketRealtimeFeed marketRealtimeFeed;

    @Autowired
    private FakeMarketDataGateway marketDataGateway;

    @Autowired
    @Qualifier("localCacheManager")
    private CacheManager localCacheManager;

    @BeforeEach
    void setUp() {
        marketDataGateway.replaceSnapshots(List.of(
                snapshot("BTCUSDT", 101000, 100950, 100900, 0.0001, 3.2),
                snapshot("ETHUSDT", 3300, 3295, 3290, 0.00008, 2.1)
        ));
        Cache cache = localCacheManager.getCache(CoinCacheNames.MARKET_SNAPSHOT_LOCAL_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void storesLatestSnapshotsInLocalSpringCacheWhenMarketsRefresh() {
        marketRealtimeFeed.refreshSupportedMarkets();

        Cache cache = localCacheManager.getCache(CoinCacheNames.MARKET_SNAPSHOT_LOCAL_CACHE);

        assertThat(cache).isNotNull();
        assertThat(cache.get("BTCUSDT", MarketSummaryResult.class)).isNotNull();
        assertThat(cache.get("ETHUSDT", MarketSummaryResult.class)).isNotNull();
        assertThat(cache.get("BTCUSDT", MarketSummaryResult.class).lastPrice()).isEqualTo(101000);
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

    @TestConfiguration
    static class MarketRealtimeFeedCacheTestConfiguration {
        @Bean("localCacheManager")
        @Primary
        CacheManager localCacheManager() {
            return new ConcurrentMapCacheManager(
                    CoinCacheNames.MARKET_SNAPSHOT_LOCAL_CACHE,
                    CoinCacheNames.MARKET_SUPPORTED_SYMBOLS_LOCAL_CACHE
            );
        }

        @Bean
        @Primary
        FakeMarketDataGateway fakeMarketDataGateway() {
            return new FakeMarketDataGateway();
        }

        @Bean
        @Primary
        Providers testProviders(FakeMarketDataGateway fakeMarketDataGateway) {
            return new Providers() {
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
                    return () -> fakeMarketDataGateway;
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
            };
        }
    }

    static class FakeMarketDataGateway implements MarketDataGateway {
        private List<MarketSnapshot> supportedMarkets = new ArrayList<>();

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

        void replaceSnapshots(List<MarketSnapshot> snapshots) {
            supportedMarkets = new ArrayList<>(snapshots);
        }
    }
}
