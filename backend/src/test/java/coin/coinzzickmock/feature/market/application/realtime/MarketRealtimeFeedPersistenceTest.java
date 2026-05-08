package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest(
        classes = {CoinZzickmockApplication.class, MarketRealtimeFeedPersistenceTest.MarketRealtimeFeedTestConfiguration.class},
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "spring.task.scheduling.enabled=false",
                "coin.market.closed-minute-persistence-delay-ms=0"
        }
)
@ActiveProfiles("test")
class MarketRealtimeFeedPersistenceTest {
    @Autowired
    private MarketRealtimeFeed marketRealtimeFeed;

    @Autowired
    private MarketMinuteCandleHistoryListener marketMinuteCandleHistoryListener;

    @Autowired
    private MarketSnapshotStore marketSnapshotStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FakeMarketDataGateway marketDataGateway;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM market_candles_1h");
        jdbcTemplate.update("DELETE FROM market_candles_1m");
        marketDataGateway.clearObservedTransactions();
        marketDataGateway.replaceSnapshots(List.of(
                snapshot("BTCUSDT", 101000, 100950, 100900, 0.0001, 3.2),
                snapshot("ETHUSDT", 3300, 3295, 3290, 0.00008, 2.1)
        ));
    }

    @Test
    void persistsMinuteHistoryWithoutPublishingPartialHourlyRowWhenMinuteClosedEventArrives() {
        marketSnapshotStore.putSupportedMarkets(List.of(
                marketSummary("BTCUSDT"),
                marketSummary("ETHUSDT")
        ));
        marketMinuteCandleHistoryListener.onMinuteClosed(new MarketMinuteClosedEvent(
                Instant.parse("2026-04-17T06:00:00Z"),
                Instant.parse("2026-04-17T06:01:00Z")
        ));

        assertThat(count("market_candles_1m")).isEqualTo(2);
        assertThat(count("market_candles_1h")).isZero();
        assertThat(volume("market_candles_1m")).isGreaterThan(0.0);
        assertThat(marketDataGateway.minuteCandleLoadTransactionStates())
                .hasSize(2)
                .containsOnly(false);
    }

    private int count(String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
        return count == null ? 0 : count;
    }

    private double volume(String tableName) {
        Double volume = jdbcTemplate.queryForObject("SELECT SUM(volume) FROM " + tableName, Double.class);
        return volume == null ? 0.0 : volume;
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

    private static MarketSummaryResult marketSummary(String symbol) {
        return new MarketSummaryResult(symbol, symbol + " Perpetual", 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    @TestConfiguration
    static class MarketRealtimeFeedTestConfiguration {
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
                    return () -> fakeMarketDataGateway;
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
        private final List<Boolean> minuteCandleLoadTransactionStates = new CopyOnWriteArrayList<>();

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
        public List<MarketMinuteCandleSnapshot> loadMinuteCandles(
                String symbol,
                Instant fromInclusive,
                Instant toExclusive
        ) {
            minuteCandleLoadTransactionStates.add(TransactionSynchronizationManager.isActualTransactionActive());
            MarketSnapshot snapshot = loadMarket(symbol);
            if (snapshot == null) {
                return List.of();
            }

            return List.of(new MarketMinuteCandleSnapshot(
                    fromInclusive,
                    fromInclusive.plus(1, ChronoUnit.MINUTES),
                    snapshot.lastPrice(),
                    snapshot.lastPrice(),
                    snapshot.lastPrice(),
                    snapshot.lastPrice(),
                    10.0,
                    snapshot.lastPrice() * 10.0
            ));
        }

        void replaceSnapshots(List<MarketSnapshot> snapshots) {
            this.supportedMarkets = new ArrayList<>(snapshots);
        }

        void clearObservedTransactions() {
            minuteCandleLoadTransactionStates.clear();
        }

        List<Boolean> minuteCandleLoadTransactionStates() {
            return List.copyOf(minuteCandleLoadTransactionStates);
        }
    }
}
