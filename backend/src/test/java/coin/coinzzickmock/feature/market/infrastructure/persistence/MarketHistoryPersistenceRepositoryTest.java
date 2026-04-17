package coin.coinzzickmock.feature.market.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import coin.coinzzickmock.providers.featureflag.FeatureFlagProvider;
import coin.coinzzickmock.providers.telemetry.TelemetryProvider;

@SpringBootTest(
        classes = {CoinZzickmockApplication.class, MarketHistoryPersistenceRepositoryTest.MarketHistoryRepositoryTestConfiguration.class},
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "spring.task.scheduling.enabled=false"
        }
)
@ActiveProfiles("test")
class MarketHistoryPersistenceRepositoryTest {
    @Autowired
    private MarketHistoryRepository marketHistoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM market_candles_1h");
        jdbcTemplate.update("DELETE FROM market_candles_1m");
    }

    @Test
    void upsertsMinuteAndHourlyCandlesThroughH2() {
        Instant minuteOpenTime = Instant.parse("2026-04-17T06:00:00Z");
        Instant minuteCloseTime = Instant.parse("2026-04-17T06:01:00Z");
        Instant hourOpenTime = Instant.parse("2026-04-17T06:00:00Z");
        Instant hourCloseTime = Instant.parse("2026-04-17T07:00:00Z");

        marketHistoryRepository.saveMinuteCandle(new MarketHistoryCandle(
                1L,
                minuteOpenTime,
                minuteCloseTime,
                101000,
                101000,
                101000,
                101000,
                0.0,
                0.0,
                0
        ));
        marketHistoryRepository.saveMinuteCandle(new MarketHistoryCandle(
                1L,
                minuteOpenTime,
                minuteCloseTime,
                101000,
                102500,
                101000,
                102500,
                0.0,
                0.0,
                0
        ));

        marketHistoryRepository.saveHourlyCandle(new HourlyMarketCandle(
                1L,
                hourOpenTime,
                hourCloseTime,
                101000,
                102500,
                101000,
                102500,
                0.0,
                0.0,
                0,
                minuteOpenTime,
                minuteCloseTime
        ));

        MarketHistoryCandle storedMinute = marketHistoryRepository.findMinuteCandles(
                1L,
                minuteOpenTime,
                minuteCloseTime
        ).get(0);

        assertEquals(1, count("market_candles_1m"));
        assertEquals(1, count("market_candles_1h"));
        assertEquals(102500, storedMinute.highPrice(), 0.0001);
        assertEquals(102500, storedMinute.closePrice(), 0.0001);
    }

    private int count(String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
        return count == null ? 0 : count;
    }

    @TestConfiguration
    static class MarketHistoryRepositoryTestConfiguration {
        @Bean
        @Primary
        FakeMarketDataGateway fakeMarketDataGateway() {
            return new FakeMarketDataGateway();
        }

        @Bean
        @Primary
        Providers testProviders(FakeMarketDataGateway fakeMarketDataGateway) {
            fakeMarketDataGateway.replaceSnapshots(List.of(
                    new MarketSnapshot("BTCUSDT", "Bitcoin Perpetual", 101000, 100950, 100900, 0.0001, 3.2),
                    new MarketSnapshot("ETHUSDT", "Ethereum Perpetual", 3300, 3295, 3290, 0.00008, 2.1)
            ));
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
            this.supportedMarkets = new ArrayList<>(snapshots);
        }
    }
}
