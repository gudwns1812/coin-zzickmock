package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.CoinZzickmockApplication;
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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = {
                CoinZzickmockApplication.class,
                MarketStartupReadyEventIntegrationTest.MarketStartupReadyEventIntegrationTestConfiguration.class
        },
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "spring.task.scheduling.enabled=false",
                "coin.market.startup-warmup.enabled=true",
                "coin.market.startup-backfill.enabled=true",
                "spring.datasource.url=jdbc:h2:mem:market-startup-ready-event-v2-${random.uuid};MODE=MySQL;DB_CLOSE_DELAY=-1"
        }
)
@ActiveProfiles("test")
@DirtiesContext
class MarketStartupReadyEventIntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MarketSnapshotStore marketSnapshotStore;

    @Test
    void warmsCacheAndBackfillsMissingHistoryAfterApplicationReady() {
        assertThat(marketSnapshotStore.getSupportedMarkets()).hasSize(2);
        assertThat(count("market_candles_1m")).isEqualTo(3);
    }

    private int count(String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
        return count == null ? 0 : count;
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
    static class MarketStartupReadyEventIntegrationTestConfiguration {
        @Bean
        ApplicationRunner seedInitialMinuteCandle(JdbcTemplate jdbcTemplate) {
            return args -> jdbcTemplate.update(
                    """
                    INSERT INTO market_candles_1m (
                        symbol_id, open_time, close_time, open_price, high_price, low_price, close_price,
                        volume, quote_volume, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())
                    """,
                    1L,
                    Instant.parse("2026-04-17T06:00:00Z"),
                    Instant.parse("2026-04-17T06:01:00Z"),
                    101000.0,
                    101000.0,
                    101000.0,
                    101000.0,
                    0.0,
                    0.0
            );
        }

        @Bean
        @Primary
        FakeMarketDataGateway fakeMarketDataGateway() {
            FakeMarketDataGateway gateway = new FakeMarketDataGateway(List.of(
                    snapshot("BTCUSDT", 103000, 102950, 102900, 0.00013, 4.4),
                    snapshot("ETHUSDT", 3300, 3295, 3290, 0.00008, 2.1)
            ));
            gateway.replaceMinuteCandles("BTCUSDT", List.of(
                    minuteCandle("2026-04-17T06:01:00Z", 101200, 101500, 101100, 101400, 12.0, 120000.0),
                    minuteCandle("2026-04-17T06:02:00Z", 101400, 102000, 101300, 101900, 14.0, 140000.0)
            ));
            return gateway;
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
                    candleOpenTime.plusSeconds(60),
                    openPrice,
                    highPrice,
                    lowPrice,
                    closePrice,
                    volume,
                    quoteVolume
            );
        }
    }

    static class FakeMarketDataGateway implements MarketDataGateway {
        private List<MarketSnapshot> supportedMarkets;
        private final java.util.Map<String, List<MarketMinuteCandleSnapshot>> minuteCandles = new java.util.LinkedHashMap<>();

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
            return minuteCandles.getOrDefault(symbol, List.of()).stream()
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .toList();
        }

        private void replaceMinuteCandles(String symbol, List<MarketMinuteCandleSnapshot> candles) {
            minuteCandles.put(symbol, new ArrayList<>(candles));
        }
    }
}
