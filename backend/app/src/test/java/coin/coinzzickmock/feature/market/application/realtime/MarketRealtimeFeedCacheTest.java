package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.testsupport.TestConnectorProvider;
import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.FundingSchedule;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.common.cache.CoinCacheNames;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.feature.market.application.gateway.MarketDataGateway;
import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @AfterEach
    void resetFundingScheduleMetadata() {
        jdbcTemplate.update("""
                UPDATE market_symbols
                SET funding_interval_hours = 8,
                    funding_anchor_hour = 1,
                    funding_time_zone = 'Asia/Seoul'
                WHERE symbol = 'ETHUSDT'
                """);
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

    @Test
    void appliesPersistedFundingScheduleMetadataWhenMarketsRefresh() {
        int updatedRows = jdbcTemplate.update("""
                UPDATE market_symbols
                SET funding_interval_hours = 4,
                    funding_anchor_hour = 2,
                    funding_time_zone = 'Asia/Seoul'
                WHERE symbol = 'ETHUSDT'
                """);
        assertThat(updatedRows).isGreaterThan(0);

        marketRealtimeFeed.refreshSupportedMarkets();

        Cache cache = localCacheManager.getCache(CoinCacheNames.MARKET_SNAPSHOT_LOCAL_CACHE);
        assertThat(cache).isNotNull();
        MarketSummaryResult eth = cache.get("ETHUSDT", MarketSummaryResult.class);
        assertThat(eth).isNotNull();
        FundingSchedule schedule = new FundingSchedule(4, 2, ZoneId.of("Asia/Seoul"));
        assertThat(eth.fundingIntervalHours()).isEqualTo(4);
        assertThat(eth.nextFundingAt()).isEqualTo(schedule.nextFundingAt(eth.serverTime()));
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
                    return TestConnectorProvider.empty();
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
            };
        }
    }

    static class FakeMarketDataGateway extends coin.coinzzickmock.testsupport.TestMarketDataGateway {
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
