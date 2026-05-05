package coin.coinzzickmock.feature.market.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = {CoinZzickmockApplication.class,
                MarketHistoryPersistenceRepositoryTest.MarketHistoryRepositoryTestConfiguration.class},
        properties = {
                "spring.datasource.url=jdbc:h2:mem:market_history_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
        Long symbolId = jdbcTemplate.queryForObject(
                "SELECT id FROM market_symbols WHERE symbol = 'BTCUSDT'", Long.class);

        Instant minuteOpenTime = Instant.parse("2026-04-17T06:00:00Z");
        Instant minuteCloseTime = Instant.parse("2026-04-17T06:01:00Z");
        Instant hourOpenTime = Instant.parse("2026-04-17T06:00:00Z");
        Instant hourCloseTime = Instant.parse("2026-04-17T07:00:00Z");

        marketHistoryRepository.saveMinuteCandle(new MarketHistoryCandle(
                symbolId,
                minuteOpenTime,
                minuteCloseTime,
                101000,
                101000,
                101000,
                101000,
                0.0,
                0.0
        ));
        marketHistoryRepository.saveMinuteCandle(new MarketHistoryCandle(
                symbolId,
                minuteOpenTime,
                minuteCloseTime,
                101000,
                102500,
                101000,
                102500,
                0.0,
                0.0
        ));

        marketHistoryRepository.saveHourlyCandle(new HourlyMarketCandle(
                symbolId,
                hourOpenTime,
                hourCloseTime,
                101000,
                102500,
                101000,
                102500,
                0.0,
                0.0,
                minuteOpenTime,
                minuteCloseTime
        ));

        MarketHistoryCandle storedMinute = marketHistoryRepository.findMinuteCandles(
                symbolId,
                minuteOpenTime,
                minuteCloseTime
        ).get(0);

        assertEquals(1, count("market_candles_1m"), "Expected 1 minute candle but found " + count("market_candles_1m"));
        assertEquals(1, count("market_candles_1h"), "Expected 1 hourly candle but found " + count("market_candles_1h"));
        assertEquals(102500, storedMinute.highPrice(), 0.0001);
        assertEquals(102500, storedMinute.closePrice(), 0.0001);
    }

    @Test
    void returnsLatestPersistedMinuteOpenTime() {
        Long symbolId = jdbcTemplate.queryForObject(
                "SELECT id FROM market_symbols WHERE symbol = 'BTCUSDT'", Long.class);

        marketHistoryRepository.saveMinuteCandle(new MarketHistoryCandle(
                symbolId,
                Instant.parse("2026-04-17T06:00:00Z"),
                Instant.parse("2026-04-17T06:01:00Z"),
                101000,
                101200,
                100900,
                101100,
                0.0,
                0.0
        ));
        marketHistoryRepository.saveMinuteCandle(new MarketHistoryCandle(
                symbolId,
                Instant.parse("2026-04-17T06:02:00Z"),
                Instant.parse("2026-04-17T06:03:00Z"),
                101100,
                101500,
                101000,
                101400,
                0.0,
                0.0
        ));

        Instant latestOpenTime = marketHistoryRepository.findLatestMinuteCandleOpenTime(symbolId).orElseThrow();

        assertEquals(Instant.parse("2026-04-17T06:02:00Z"), latestOpenTime);
    }

    @Test
    void completedHourlyCandlesRequireContiguousMinuteCoverage() {
        Long symbolId = jdbcTemplate.queryForObject(
                "SELECT id FROM market_symbols WHERE symbol = 'BTCUSDT'", Long.class);

        Instant completeHour = Instant.parse("2026-04-17T06:00:00Z");
        Instant missingFirstHour = Instant.parse("2026-04-17T07:00:00Z");
        Instant missingMiddleHour = Instant.parse("2026-04-17T08:00:00Z");
        Instant missingLastHour = Instant.parse("2026-04-17T09:00:00Z");
        saveHourWithMinutesExcept(symbolId, completeHour, -1);
        saveHourWithMinutesExcept(symbolId, missingFirstHour, 0);
        saveHourWithMinutesExcept(symbolId, missingMiddleHour, 30);
        saveHourWithMinutesExcept(symbolId, missingLastHour, 59);

        List<HourlyMarketCandle> completedCandles = marketHistoryRepository.findCompletedHourlyCandles(
                symbolId,
                completeHour,
                missingLastHour.plusSeconds(3600)
        );

        assertEquals(List.of(completeHour), completedCandles.stream().map(HourlyMarketCandle::openTime).toList());
        assertEquals(completeHour, marketHistoryRepository.findLatestCompletedHourlyCandleOpenTime(symbolId).orElseThrow());
        assertEquals(completeHour, marketHistoryRepository.findLatestCompletedHourlyCandleOpenTimeBefore(
                symbolId,
                missingLastHour.plusSeconds(1800)
        ).orElseThrow());
    }

    private int count(String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
        return count == null ? 0 : count;
    }

    private void saveHourWithMinutesExcept(Long symbolId, Instant hourOpenTime, int missingMinuteIndex) {
        for (int minute = 0; minute < 60; minute++) {
            if (minute == missingMinuteIndex) {
                continue;
            }
            Instant openTime = hourOpenTime.plusSeconds(minute * 60L);
            marketHistoryRepository.saveMinuteCandle(new MarketHistoryCandle(
                    symbolId,
                    openTime,
                    openTime.plusSeconds(60),
                    101000 + minute,
                    101500 + minute,
                    100500 + minute,
                    101250 + minute,
                    10.0,
                    1012500.0
            ));
        }

        marketHistoryRepository.saveHourlyCandle(new HourlyMarketCandle(
                symbolId,
                hourOpenTime,
                hourOpenTime.plusSeconds(3600),
                101000,
                101500,
                100500,
                101250,
                10.0,
                1012500.0,
                hourOpenTime,
                hourOpenTime.plusSeconds(3600)
        ));
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
            return List.of();
        }

        void replaceSnapshots(List<MarketSnapshot> snapshots) {
            this.supportedMarkets = new ArrayList<>(snapshots);
        }
    }
}
