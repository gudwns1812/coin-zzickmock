package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoricalCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CurrentMarketCandleBootstrapperTest {
    private static final Instant NOW = Instant.parse("2026-04-30T04:05:23Z");

    @Test
    void bootstrapsMinuteSourceForMinuteDerivedIntervals() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        RealtimeMarketCandleProjector projector = new RealtimeMarketCandleProjector(store);
        RecordingMarketDataGateway gateway = new RecordingMarketDataGateway();
        gateway.candles.add(candle("2026-04-30T04:05:00Z", "2026-04-30T04:06:00Z", 100, 102));
        CurrentMarketCandleBootstrapper bootstrapper = bootstrapper(gateway, store, projector);

        boolean bootstrapped = bootstrapper.bootstrapIfNeeded("BTCUSDT", MarketCandleInterval.THREE_MINUTES);

        assertThat(bootstrapped).isTrue();
        assertThat(projector.latest("BTCUSDT", MarketCandleInterval.THREE_MINUTES)).isPresent();
        assertThat(gateway.calls()).containsExactly("1m:2026-04-30T04:05:00Z:2026-04-30T04:06:00Z");
    }

    @Test
    void hourlyBootstrapPrefersMinuteDerivedSeedBeforeProviderHourFallback() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        RealtimeMarketCandleProjector projector = new RealtimeMarketCandleProjector(store);
        RecordingMarketDataGateway gateway = new RecordingMarketDataGateway();
        gateway.candles.add(candle("2026-04-30T04:00:00Z", "2026-04-30T04:01:00Z", 100, 101));
        gateway.candles.add(candle("2026-04-30T04:01:00Z", "2026-04-30T04:02:00Z", 101, 103));
        CurrentMarketCandleBootstrapper bootstrapper = bootstrapper(gateway, store, projector);

        boolean bootstrapped = bootstrapper.bootstrapIfNeeded("BTCUSDT", MarketCandleInterval.FOUR_HOURS);

        assertThat(bootstrapped).isTrue();
        assertThat(projector.latest("BTCUSDT", MarketCandleInterval.ONE_HOUR)).isPresent()
                .get()
                .extracting(result -> result.closePrice())
                .isEqualTo(103.0);
        assertThat(gateway.calls()).containsExactly("1m:2026-04-30T04:00:00Z:2026-04-30T05:00:00Z");
    }

    @Test
    void hourlyBootstrapFallsBackToLiveOnlyProviderHour() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        RealtimeMarketCandleProjector projector = new RealtimeMarketCandleProjector(store);
        RecordingMarketDataGateway gateway = new RecordingMarketDataGateway();
        gateway.hourlyCandles.add(candle("2026-04-30T04:00:00Z", "2026-04-30T05:00:00Z", 100, 105));
        CurrentMarketCandleBootstrapper bootstrapper = bootstrapper(gateway, store, projector);

        boolean bootstrapped = bootstrapper.bootstrapIfNeeded("BTCUSDT", MarketCandleInterval.ONE_HOUR);

        assertThat(bootstrapped).isTrue();
        assertThat(projector.latest("BTCUSDT", MarketCandleInterval.ONE_HOUR)).isPresent()
                .get()
                .extracting(result -> result.closePrice())
                .isEqualTo(105.0);
        assertThat(gateway.calls()).containsExactly(
                "1m:2026-04-30T04:00:00Z:2026-04-30T05:00:00Z",
                "1h:2026-04-30T04:00:00Z:2026-04-30T05:00:00Z"
        );
    }

    @Test
    void shortTtlPreventsRepeatedProviderBursts() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        RealtimeMarketCandleProjector projector = new RealtimeMarketCandleProjector(store);
        RecordingMarketDataGateway gateway = new RecordingMarketDataGateway();
        CurrentMarketCandleBootstrapper bootstrapper = bootstrapper(gateway, store, projector);

        bootstrapper.bootstrapIfNeeded("BTCUSDT", MarketCandleInterval.ONE_MINUTE);
        bootstrapper.bootstrapIfNeeded("BTCUSDT", MarketCandleInterval.ONE_MINUTE);

        assertThat(gateway.calls()).hasSize(1);
    }

    @Test
    void webSocketUpdateSupersedesBootstrapStateForSameCandle() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        RealtimeMarketCandleProjector projector = new RealtimeMarketCandleProjector(store);
        RecordingMarketDataGateway gateway = new RecordingMarketDataGateway();
        gateway.candles.add(candle("2026-04-30T04:05:00Z", "2026-04-30T04:06:00Z", 100, 102));
        CurrentMarketCandleBootstrapper bootstrapper = bootstrapper(gateway, store, projector);
        bootstrapper.bootstrapIfNeeded("BTCUSDT", MarketCandleInterval.ONE_MINUTE);

        store.acceptCandle(new RealtimeMarketCandleUpdate(
                "BTCUSDT",
                MarketCandleInterval.ONE_MINUTE,
                Instant.parse("2026-04-30T04:05:00Z"),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(106),
                BigDecimal.valueOf(99),
                BigDecimal.valueOf(106),
                BigDecimal.ONE,
                BigDecimal.valueOf(106),
                BigDecimal.valueOf(106),
                Instant.parse("2026-04-30T04:05:10Z"),
                NOW
        ));

        assertThat(projector.latest("BTCUSDT", MarketCandleInterval.ONE_MINUTE)).isPresent()
                .get()
                .extracting(result -> result.closePrice())
                .isEqualTo(106.0);
    }

    private static CurrentMarketCandleBootstrapper bootstrapper(
            RecordingMarketDataGateway gateway,
            RealtimeMarketDataStore store,
            RealtimeMarketCandleProjector projector
    ) {
        return new CurrentMarketCandleBootstrapper(
                gateway,
                store,
                projector,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private static MarketHistoricalCandleSnapshot candle(
            String openTime,
            String closeTime,
            double open,
            double close
    ) {
        return new MarketHistoricalCandleSnapshot(
                Instant.parse(openTime),
                Instant.parse(closeTime),
                open,
                Math.max(open, close),
                Math.min(open, close),
                close,
                10,
                10 * close
        );
    }

    private static class RecordingMarketDataGateway extends coin.coinzzickmock.testsupport.TestMarketDataGateway {
        private final List<MarketHistoricalCandleSnapshot> candles = new ArrayList<>();
        private final List<MarketHistoricalCandleSnapshot> hourlyCandles = new ArrayList<>();
        private final List<String> calls = new ArrayList<>();

        @Override
        public List<MarketSnapshot> loadSupportedMarkets() {
            return List.of();
        }

        @Override
        public MarketSnapshot loadMarket(String symbol) {
            return null;
        }

        @Override
        public List<MarketMinuteCandleSnapshot> loadMinuteCandles(
                String symbol,
                Instant fromInclusive,
                Instant toExclusive
        ) {
            return List.of();
        }

        @Override
        public List<MarketHistoricalCandleSnapshot> loadHistoricalCandles(
                String symbol,
                MarketCandleInterval interval,
                Instant fromInclusive,
                Instant toExclusive,
                int limit
        ) {
            calls.add(interval.value() + ":" + fromInclusive + ":" + toExclusive);
            if (interval == MarketCandleInterval.ONE_HOUR) {
                return List.copyOf(hourlyCandles);
            }
            return List.copyOf(candles);
        }

        private List<String> calls() {
            return List.copyOf(calls);
        }
    }
}
