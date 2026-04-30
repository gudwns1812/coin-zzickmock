package coin.coinzzickmock.feature.order.api;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.order.application.realtime.TradingExecutionEvent;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class TradingExecutionSseBrokerTest {
    @Test
    void removesFailedEmitterWithoutCompletingOrAffectingHealthyEmitters() {
        TradingExecutionSseBroker broker = new TradingExecutionSseBroker(Runnable::run, 10, 20);
        FailingSseEmitter failingEmitter = new FailingSseEmitter();
        CapturingSseEmitter healthyEmitter = new CapturingSseEmitter();
        broker.register(broker.reserve(1L), failingEmitter);
        broker.register(broker.reserve(1L), healthyEmitter);
        TradingExecutionEvent event = TradingExecutionEvent.orderFilled(
                1L,
                "order-1",
                "BTCUSDT",
                "LONG",
                "CROSS",
                1,
                74000
        );

        broker.onTradingExecution(event);
        broker.onTradingExecution(event);

        assertThat(failingEmitter.sendAttempts()).isEqualTo(1);
        assertThat(failingEmitter.completed()).isFalse();
        assertThat(healthyEmitter.events()).hasSize(2);
    }

    private static class CapturingSseEmitter extends SseEmitter {
        private final List<Object> events = new CopyOnWriteArrayList<>();

        private CapturingSseEmitter() {
            super(0L);
        }

        @Override
        public synchronized void send(Object object) throws IOException {
            events.add(object);
        }

        private List<Object> events() {
            return List.copyOf(events);
        }
    }

    private static class FailingSseEmitter extends SseEmitter {
        private int sendAttempts;
        private boolean completed;

        private FailingSseEmitter() {
            super(0L);
        }

        @Override
        public synchronized void send(Object object) throws IOException {
            sendAttempts++;
            throw new IOException("client disconnected");
        }

        @Override
        public synchronized void complete() {
            completed = true;
            super.complete();
        }

        private int sendAttempts() {
            return sendAttempts;
        }

        private boolean completed() {
            return completed;
        }
    }
}
