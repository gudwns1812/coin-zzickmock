package coin.coinzzickmock.feature.market.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coin.coinzzickmock.common.web.SseSubscriptionLimitExceededException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class MarketRealtimeSseBrokerTest {
    @Test
    void deliversMatchingSymbolUpdatesToRegisteredEmitters() {
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(directExecutor(), 10, 20);
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        broker.register(broker.reserve("BTCUSDT"), emitter);

        broker.onMarketUpdated(summary("BTCUSDT", 74000));

        assertThat(emitter.events()).hasSize(1);
        assertThat(((MarketSummaryResponse) emitter.events().get(0)).lastPrice()).isEqualTo(74000);
    }

    @Test
    void doesNotDeliverEventToDifferentSymbolSubscribers() {
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(directExecutor(), 10, 20);
        CapturingSseEmitter btcEmitter = new CapturingSseEmitter();
        CapturingSseEmitter ethEmitter = new CapturingSseEmitter();
        broker.register(broker.reserve("BTCUSDT"), btcEmitter);
        broker.register(broker.reserve("ETHUSDT"), ethEmitter);

        broker.onMarketUpdated(summary("BTCUSDT", 74000));

        assertThat(btcEmitter.events()).hasSize(1);
        assertThat(ethEmitter.events()).isEmpty();
    }

    @Test
    void oneClientKeyCanReceiveMultipleSymbolUpdatesThroughOneEmitter() {
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(directExecutor(), 10, 20);
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        broker.register(broker.reserve(java.util.Set.of("BTCUSDT", "ETHUSDT"), "tab-1"), emitter);

        broker.onMarketUpdated(summary("BTCUSDT", 74000));
        broker.onMarketUpdated(summary("ETHUSDT", 3200));

        assertThat(emitter.events()).hasSize(2);
    }

    @Test
    void rejectsWhenTotalLimitExceeded() {
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(directExecutor(), 10, 1);
        broker.register(broker.reserve("BTCUSDT"), new CapturingSseEmitter());

        assertThatThrownBy(() -> broker.reserve("ETHUSDT"))
                .isInstanceOf(SseSubscriptionLimitExceededException.class)
                .extracting("reason")
                .isEqualTo("total_limit");
    }

    private static MarketSummaryResponse summary(String symbol, double price) {
        return MarketSummaryResponse.of(symbol, symbol, price, price, price, 0.01, 0.2, 100, Instant.parse("2026-05-12T00:00:00Z"), null, 8);
    }

    private Executor directExecutor() { return Runnable::run; }

    private static class CapturingSseEmitter extends SseEmitter {
        private final List<Object> events = new CopyOnWriteArrayList<>();
        @Override public void send(Object object) throws IOException { events.add(object); }
        List<Object> events() { return new ArrayList<>(events); }
    }
}
