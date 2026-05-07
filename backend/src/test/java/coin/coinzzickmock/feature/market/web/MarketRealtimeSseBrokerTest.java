package coin.coinzzickmock.feature.market.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.realtime.MarketSummaryUpdatedEvent;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.providers.telemetry.SseTelemetry;
import java.io.IOException;
import java.time.Duration;
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

        broker.onMarketUpdated(new MarketSummaryUpdatedEvent(
                new MarketSummaryResult("BTCUSDT", "Bitcoin Perpetual", 74000, 74010, 74005, 0.0001, 0.2)
        ));

        assertThat(emitter.events()).hasSize(1);
        assertThat(emitter.events().get(0)).isInstanceOf(MarketSummaryResponse.class);
        assertThat(((MarketSummaryResponse) emitter.events().get(0)).lastPrice()).isEqualTo(74000);
    }

    @Test
    void includesTurnoverInRealtimePayloads() {
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(directExecutor(), 10, 20);
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        broker.register(broker.reserve("BTCUSDT"), emitter);

        broker.onMarketUpdated(new MarketSummaryUpdatedEvent(
                new MarketSummaryResult(
                        "BTCUSDT", "Bitcoin Perpetual", 74000, 74010, 74005, 0.0001, 0.2, 5_250_000_000d
                )
        ));

        MarketSummaryResponse response = (MarketSummaryResponse) emitter.events().get(0);
        assertThat(response.turnover24hUsdt()).isEqualTo(5_250_000_000d);
        assertThat(response.volume24h()).isEqualTo(5_250_000_000d);
    }

    @Test
    void removesFailedEmitterWithoutCompletingOrAffectingHealthyEmitters() {
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(directExecutor(), 10, 20);
        FailingSseEmitter failingEmitter = new FailingSseEmitter();
        CapturingSseEmitter healthyEmitter = new CapturingSseEmitter();
        MarketSummaryUpdatedEvent firstEvent = new MarketSummaryUpdatedEvent(
                new MarketSummaryResult("BTCUSDT", "Bitcoin Perpetual", 74000, 74010, 74005, 0.0001, 0.2)
        );
        MarketSummaryUpdatedEvent secondEvent = new MarketSummaryUpdatedEvent(
                new MarketSummaryResult("BTCUSDT", "Bitcoin Perpetual", 74100, 74105, 74102, 0.0001, 0.3)
        );
        broker.register(broker.reserve("BTCUSDT"), failingEmitter);
        broker.register(broker.reserve("BTCUSDT"), healthyEmitter);

        broker.onMarketUpdated(firstEvent);
        broker.onMarketUpdated(secondEvent);

        assertThat(failingEmitter.sendAttempts()).isEqualTo(1);
        assertThat(failingEmitter.completed()).isFalse();
        assertThat(healthyEmitter.events()).hasSize(2);
        assertThat(((MarketSummaryResponse) healthyEmitter.events().get(1)).lastPrice()).isEqualTo(74100);
    }

    @Test
    void doesNotDeliverEventToDifferentSymbolSubscribers() {
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(directExecutor(), 10, 20);
        CapturingSseEmitter btcEmitter = new CapturingSseEmitter();
        CapturingSseEmitter ethEmitter = new CapturingSseEmitter();
        broker.register(broker.reserve("BTCUSDT"), btcEmitter);
        broker.register(broker.reserve("ETHUSDT"), ethEmitter);

        broker.onMarketUpdated(new MarketSummaryUpdatedEvent(
                new MarketSummaryResult("BTCUSDT", "Bitcoin Perpetual", 74000, 74010, 74005, 0.0001, 0.2)
        ));

        assertThat(btcEmitter.events()).hasSize(1);
        assertThat(ethEmitter.events()).isEmpty();
    }

    @Test
    void doesNotDeliverEventAfterEmitterIsUnregistered() {
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(directExecutor(), 10, 20);
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        broker.register(broker.reserve("BTCUSDT"), emitter);
        broker.unregister("BTCUSDT", emitter);

        broker.onMarketUpdated(new MarketSummaryUpdatedEvent(
                new MarketSummaryResult("BTCUSDT", "Bitcoin Perpetual", 74000, 74010, 74005, 0.0001, 0.2)
        ));

        assertThat(emitter.events()).isEmpty();
    }

    @Test
    void unregistersEmitterWhenCompletionCallbackRuns() {
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(directExecutor(), 10, 20);
        CallbackCapturingSseEmitter emitter = new CallbackCapturingSseEmitter();
        broker.register(broker.reserve("BTCUSDT"), emitter);

        emitter.fireCompletion();
        broker.onMarketUpdated(new MarketSummaryUpdatedEvent(
                new MarketSummaryResult("BTCUSDT", "Bitcoin Perpetual", 74000, 74010, 74005, 0.0001, 0.2)
        ));

        assertThat(emitter.capturedEvents()).isEmpty();
    }

    @Test
    void unregistersAndCompletesEmitterWhenTimeoutCallbackRuns() {
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(directExecutor(), 10, 20);
        CallbackCapturingSseEmitter emitter = new CallbackCapturingSseEmitter();
        broker.register(broker.reserve("BTCUSDT"), emitter);

        emitter.fireTimeout();
        broker.onMarketUpdated(new MarketSummaryUpdatedEvent(
                new MarketSummaryResult("BTCUSDT", "Bitcoin Perpetual", 74000, 74010, 74005, 0.0001, 0.2)
        ));

        assertThat(emitter.completed()).isTrue();
        assertThat(emitter.capturedEvents()).isEmpty();
    }

    @Test
    void unregistersEmitterWhenErrorCallbackRuns() {
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(directExecutor(), 10, 20);
        CallbackCapturingSseEmitter emitter = new CallbackCapturingSseEmitter();
        broker.register(broker.reserve("BTCUSDT"), emitter);

        emitter.fireError(new IOException("boom"));
        broker.onMarketUpdated(new MarketSummaryUpdatedEvent(
                new MarketSummaryResult("BTCUSDT", "Bitcoin Perpetual", 74000, 74010, 74005, 0.0001, 0.2)
        ));

        assertThat(emitter.events()).isEmpty();
    }

    @Test
    void rejectsRegistrationWhenSubscriberLimitIsReached() {
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(directExecutor(), 1, 20);
        broker.register(broker.reserve("BTCUSDT"), new CapturingSseEmitter());

        assertThatThrownBy(() -> broker.reserve("BTCUSDT"))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).errorCode())
                .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    void rejectsRegistrationWhenGlobalSubscriberLimitIsReached() {
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(directExecutor(), 10, 2);
        broker.register(broker.reserve("BTCUSDT"), new CapturingSseEmitter());
        broker.register(broker.reserve("ETHUSDT"), new CapturingSseEmitter());

        assertThatThrownBy(() -> broker.reserve("SOLUSDT"))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).errorCode())
                .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    void rejectsRegisterWhenGlobalSubscriberLimitIsAlreadyReserved() {
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(directExecutor(), 10, 2);
        MarketRealtimeSseBroker.SseSubscriptionPermit first = broker.reserve("BTCUSDT");
        MarketRealtimeSseBroker.SseSubscriptionPermit second = broker.reserve("ETHUSDT");
        broker.register(first, new CapturingSseEmitter());
        broker.register(second, new CapturingSseEmitter());

        assertThatThrownBy(() -> broker.reserve("SOLUSDT"))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).errorCode())
                .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    void cleansUpUnusedSymbolPermitWhenReservedSubscriptionIsReleased() {
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(directExecutor(), 10, 20);
        MarketRealtimeSseBroker.SseSubscriptionPermit permit = broker.reserve("UNSUPPORTED");

        broker.release(permit);

        assertThat(broker.hasSubscriberLimit("UNSUPPORTED")).isFalse();
    }

    @Test
    void cleansUpUnusedSymbolPermitWhenEmitterIsUnregistered() {
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(directExecutor(), 10, 20);
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        broker.register(broker.reserve("BTCUSDT"), emitter);

        broker.unregister("BTCUSDT", emitter);

        assertThat(broker.hasSubscriberLimit("BTCUSDT")).isFalse();
    }

    @Test
    void deliversUpdatesOnlyAfterQueuedFanoutTaskRuns() {
        RecordingExecutor executor = new RecordingExecutor();
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(executor, 10, 20);
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        broker.register(broker.reserve("BTCUSDT"), emitter);

        broker.onMarketUpdated(new MarketSummaryUpdatedEvent(
                new MarketSummaryResult("BTCUSDT", "Bitcoin Perpetual", 74000, 74010, 74005, 0.0001, 0.2)
        ));

        assertThat(executor.taskCount()).isEqualTo(1);
        assertThat(emitter.events()).isEmpty();

        executor.runAll();

        assertThat(emitter.events()).hasSize(1);
    }

    @Test
    void recordsConnectionSendAndRejectionTelemetry() {
        RecordingSseTelemetry telemetry = new RecordingSseTelemetry();
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(directExecutor(), 1, 2, telemetry);
        CapturingSseEmitter healthyEmitter = new CapturingSseEmitter();
        FailingSseEmitter failingEmitter = new FailingSseEmitter();
        broker.register(broker.reserve("BTCUSDT"), healthyEmitter);
        broker.register(broker.reserve("ETHUSDT"), failingEmitter);

        assertThatThrownBy(() -> broker.reserve("BTCUSDT"))
                .isInstanceOf(CoreException.class);
        broker.onMarketUpdated(new MarketSummaryUpdatedEvent(
                new MarketSummaryResult("BTCUSDT", "Bitcoin Perpetual", 74000, 74010, 74005, 0.0001, 0.2)
        ));
        broker.onMarketUpdated(new MarketSummaryUpdatedEvent(
                new MarketSummaryResult("ETHUSDT", "Ethereum Perpetual", 3200, 3201, 3199, 0.0001, 0.2)
        ));

        assertThat(telemetry.events()).contains(
                "opened:market",
                "send:market:success",
                "send:market:failure",
                "closed:market:send_failure",
                "rejected:market:total_limit"
        );
    }


    @Test
    void replacesDuplicateClientKeyEvenWhenSymbolLimitIsFull() {
        RecordingSseTelemetry telemetry = new RecordingSseTelemetry();
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(directExecutor(), 1, 1, telemetry);
        CallbackCapturingSseEmitter first = new CallbackCapturingSseEmitter();
        CapturingSseEmitter second = new CapturingSseEmitter();

        broker.register(broker.reserve("BTCUSDT", "tab-1"), first);
        broker.register(broker.reserve("BTCUSDT", "tab-1"), second);
        first.fireCompletion();

        broker.onMarketUpdated(new MarketSummaryUpdatedEvent(
                new MarketSummaryResult("BTCUSDT", "Bitcoin Perpetual", 74000, 74010, 74005, 0.0001, 0.2)
        ));

        assertThat(first.completed()).isTrue();
        assertThat(first.events()).isEmpty();
        assertThat(second.events()).hasSize(1);
        assertThat(telemetry.events()).contains("closed:market:replaced");
    }

    @Test
    void sendsSameSymbolUpdatesToDifferentClientKeys() {
        MarketRealtimeSseBroker broker = new MarketRealtimeSseBroker(directExecutor(), 2, 2);
        CapturingSseEmitter first = new CapturingSseEmitter();
        CapturingSseEmitter second = new CapturingSseEmitter();

        broker.register(broker.reserve("BTCUSDT", "tab-1"), first);
        broker.register(broker.reserve("BTCUSDT", "tab-2"), second);

        broker.onMarketUpdated(new MarketSummaryUpdatedEvent(
                new MarketSummaryResult("BTCUSDT", "Bitcoin Perpetual", 74000, 74010, 74005, 0.0001, 0.2)
        ));

        assertThat(first.events()).hasSize(1);
        assertThat(second.events()).hasSize(1);
    }

    private static Executor directExecutor() {
        return Runnable::run;
    }

    private static class RecordingExecutor implements Executor {
        private final List<Runnable> tasks = new CopyOnWriteArrayList<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        private int taskCount() {
            return tasks.size();
        }

        private void runAll() {
            List<Runnable> snapshot = List.copyOf(tasks);
            tasks.clear();
            snapshot.forEach(Runnable::run);
        }
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

        protected List<Object> events() {
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

    private static class CallbackCapturingSseEmitter extends CapturingSseEmitter {
        private Runnable completionCallback;
        private Runnable timeoutCallback;
        private java.util.function.Consumer<Throwable> errorCallback;
        private boolean completed;

        @Override
        public synchronized void onCompletion(Runnable callback) {
            this.completionCallback = callback;
        }

        @Override
        public synchronized void onTimeout(Runnable callback) {
            this.timeoutCallback = callback;
        }

        @Override
        public synchronized void onError(java.util.function.Consumer<Throwable> callback) {
            this.errorCallback = callback;
        }

        @Override
        public synchronized void complete() {
            this.completed = true;
            super.complete();
        }

        private void fireCompletion() {
            completionCallback.run();
        }

        private void fireTimeout() {
            timeoutCallback.run();
        }

        private void fireError(Throwable throwable) {
            errorCallback.accept(throwable);
        }

        private boolean completed() {
            return completed;
        }

        private List<Object> capturedEvents() {
            return events();
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
