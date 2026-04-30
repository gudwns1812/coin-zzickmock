package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RealtimeMarketCandleProjectorTest {
    @Test
    void aggregatesThreeMinuteCandleFromOneMinuteState() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        RealtimeMarketCandleProjector projector = new RealtimeMarketCandleProjector(store);
        Instant open = Instant.parse("2026-04-30T04:00:00Z");
        store.acceptCandle(candle(MarketCandleInterval.ONE_MINUTE, open, 100, 105, 99, 102, 1));
        store.acceptCandle(candle(MarketCandleInterval.ONE_MINUTE, open.plusSeconds(60), 102, 108, 101, 107, 2));

        MarketCandleResult result = projector.latest("BTCUSDT", MarketCandleInterval.THREE_MINUTES).orElseThrow();

        assertThat(result.openTime()).isEqualTo(open);
        assertThat(result.closeTime()).isEqualTo(open.plusSeconds(180));
        assertThat(result.openPrice()).isEqualTo(100);
        assertThat(result.highPrice()).isEqualTo(108);
        assertThat(result.lowPrice()).isEqualTo(99);
        assertThat(result.closePrice()).isEqualTo(107);
        assertThat(result.volume()).isEqualTo(3);
    }

    @Test
    void aggregatesFourHourCandleFromOneHourState() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        RealtimeMarketCandleProjector projector = new RealtimeMarketCandleProjector(store);
        Instant open = Instant.parse("2026-04-30T04:00:00Z");
        store.acceptCandle(candle(MarketCandleInterval.ONE_HOUR, open, 100, 110, 90, 105, 3));
        store.acceptCandle(candle(MarketCandleInterval.ONE_HOUR, open.plusSeconds(3600), 105, 115, 95, 112, 4));

        MarketCandleResult result = projector.latest("BTCUSDT", MarketCandleInterval.FOUR_HOURS).orElseThrow();

        assertThat(result.openTime()).isEqualTo(open);
        assertThat(result.closeTime()).isEqualTo(open.plusSeconds(14400));
        assertThat(result.highPrice()).isEqualTo(115);
        assertThat(result.lowPrice()).isEqualTo(90);
        assertThat(result.closePrice()).isEqualTo(112);
        assertThat(result.volume()).isEqualTo(7);
    }

    private static RealtimeMarketCandleUpdate candle(
            MarketCandleInterval interval,
            Instant openTime,
            double open,
            double high,
            double low,
            double close,
            double volume
    ) {
        return new RealtimeMarketCandleUpdate(
                "BTCUSDT",
                interval,
                openTime,
                BigDecimal.valueOf(open),
                BigDecimal.valueOf(high),
                BigDecimal.valueOf(low),
                BigDecimal.valueOf(close),
                BigDecimal.valueOf(volume),
                BigDecimal.valueOf(volume * close),
                BigDecimal.valueOf(volume * close),
                openTime.plusSeconds(1),
                openTime.plusSeconds(1)
        );
    }
}
