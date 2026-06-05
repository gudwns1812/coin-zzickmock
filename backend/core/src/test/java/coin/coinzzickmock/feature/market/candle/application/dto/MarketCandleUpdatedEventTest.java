package coin.coinzzickmock.feature.market.candle.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MarketCandleUpdatedEventTest {
    @Test
    void creates_payload_from_realtime_candle_update() {
        Instant openTime = Instant.parse("2026-05-28T00:00:00Z");
        RealtimeMarketCandleUpdate update = new RealtimeMarketCandleUpdate(
                "BTCUSDT",
                MarketCandleInterval.ONE_MINUTE,
                openTime,
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(2),
                BigDecimal.valueOf(0.5),
                BigDecimal.valueOf(1.5),
                BigDecimal.TEN,
                BigDecimal.valueOf(15),
                BigDecimal.valueOf(20),
                openTime,
                openTime
        );

        MarketCandleUpdatedEvent event = MarketCandleUpdatedEvent.from(update);

        assertThat(event.symbol()).isEqualTo("BTCUSDT");
        assertThat(event.interval()).isEqualTo("1m");
        assertThat(event.candle().closeTime()).isEqualTo(openTime.plusSeconds(60));
        assertThat(event.candle().volume()).isEqualTo(10.0);
        assertThat(event.hasPayload()).isTrue();
    }
}
