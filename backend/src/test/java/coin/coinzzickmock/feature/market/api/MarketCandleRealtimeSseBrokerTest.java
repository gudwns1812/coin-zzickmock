package coin.coinzzickmock.feature.market.api;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.application.realtime.MarketCandleUpdatedEvent;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleProjector;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleUpdate;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketDataStore;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class MarketCandleRealtimeSseBrokerTest {
    @Test
    void deliversProjectedCandleToMatchingIntervalSubscribers() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        MarketCandleRealtimeSseBroker broker = new MarketCandleRealtimeSseBroker(
                Runnable::run,
                new RealtimeMarketCandleProjector(store)
        );
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        broker.register("BTCUSDT", MarketCandleInterval.ONE_MINUTE, emitter);
        Instant open = Instant.parse("2026-04-30T04:00:00Z");
        store.acceptCandle(new RealtimeMarketCandleUpdate(
                "BTCUSDT",
                MarketCandleInterval.ONE_MINUTE,
                open,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(105),
                BigDecimal.valueOf(99),
                BigDecimal.valueOf(102),
                BigDecimal.ONE,
                BigDecimal.valueOf(102),
                BigDecimal.valueOf(102),
                open.plusSeconds(1),
                open.plusSeconds(1)
        ));

        broker.onCandleUpdated(new MarketCandleUpdatedEvent("BTCUSDT"));

        assertThat(emitter.events()).hasSize(1);
        assertThat(((MarketCandleResponse) emitter.events().get(0)).closePrice()).isEqualTo(102);
    }

    private static class CapturingSseEmitter extends SseEmitter {
        private final List<Object> events = new ArrayList<>();

        @Override
        public void send(Object object) throws IOException {
            events.add(object);
        }

        private List<Object> events() {
            return events;
        }
    }
}
