package coin.coinzzickmock.feature.order.api;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.order.application.realtime.TradingExecutionEvent;
import coin.coinzzickmock.providers.telemetry.SseTelemetry;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;
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

    @Test
    void recordsConnectionSendRejectionAndExecutorTelemetry() {
        RecordingSseTelemetry telemetry = new RecordingSseTelemetry();
        TradingExecutionSseBroker broker = new TradingExecutionSseBroker(Runnable::run, 1, 2, telemetry);
        FailingSseEmitter failingEmitter = new FailingSseEmitter();
        broker.register(broker.reserve(1L), failingEmitter);

        broker.onTradingExecution(event());

        assertThat(telemetry.events()).contains(
                "opened:trading_execution",
                "send:trading_execution:failure",
                "closed:trading_execution:send_failure"
        );

        broker.register(broker.reserve(1L), new CapturingSseEmitter());
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> broker.reserve(1L))).isNotNull();
        assertThat(telemetry.events()).contains("rejected:trading_execution:member_limit");

        RecordingSseTelemetry rejectedTelemetry = new RecordingSseTelemetry();
        TradingExecutionSseBroker rejectedBroker = new TradingExecutionSseBroker(
                command -> {
                    throw new RejectedExecutionException("queue full");
                },
                10,
                20,
                rejectedTelemetry
        );
        rejectedBroker.register(rejectedBroker.reserve(2L), new CapturingSseEmitter());

        rejectedBroker.onTradingExecution(TradingExecutionEvent.orderFilled(
                2L,
                "order-2",
                "BTCUSDT",
                "LONG",
                "CROSS",
                1,
                74000
        ));

        assertThat(rejectedTelemetry.events()).contains("executor:trading_execution");
    }

    private TradingExecutionEvent event() {
        return TradingExecutionEvent.orderFilled(
                1L,
                "order-1",
                "BTCUSDT",
                "LONG",
                "CROSS",
                1,
                74000
        );
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

    private static class RecordingSseTelemetry implements SseTelemetry {
        private final List<String> events = new ArrayList<>();

        @Override
        public void connectionOpened(String stream) {
            events.add("opened:" + stream);
        }

        @Override
        public void connectionClosed(String stream, String reason) {
            events.add("closed:" + stream + ":" + reason);
        }

        @Override
        public void connectionRejected(String stream, String reason) {
            events.add("rejected:" + stream + ":" + reason);
        }

        @Override
        public void sendRecorded(String stream, String result, Duration duration) {
            events.add("send:" + stream + ":" + result);
        }

        @Override
        public void executorRejected(String stream) {
            events.add("executor:" + stream);
        }

        private List<String> events() {
            return List.copyOf(events);
        }
    }
}
