package coin.coinzzickmock.feature.market.api;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.realtime.MarketSummaryUpdatedEvent;
import coin.coinzzickmock.providers.telemetry.NoopSseTelemetry;
import coin.coinzzickmock.providers.telemetry.SseTelemetry;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class MarketRealtimeSseBroker {
    private static final String STREAM = "market";

    private final ConcurrentMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Semaphore> symbolSubscriberLimits = new ConcurrentHashMap<>();
    private final Executor sseEventExecutor;
    private final int maxSubscribersPerSymbol;
    private final Semaphore totalSubscriberLimit;
    private final SseTelemetry sseTelemetry;

    @Autowired
    public MarketRealtimeSseBroker(
            @Qualifier("marketRealtimeSseEventExecutor") Executor sseEventExecutor,
            @Value("${coin.market.sse.max-subscribers-per-symbol:50}") int maxSubscribersPerSymbol,
            @Value("${coin.market.sse.max-total-subscribers:100}") int maxSubscribersTotal,
            SseTelemetry sseTelemetry
    ) {
        this.sseEventExecutor = sseEventExecutor;
        this.maxSubscribersPerSymbol = maxSubscribersPerSymbol;
        this.totalSubscriberLimit = new Semaphore(maxSubscribersTotal, true);
        this.sseTelemetry = sseTelemetry;
    }

    MarketRealtimeSseBroker(
            Executor sseEventExecutor,
            int maxSubscribersPerSymbol,
            int maxSubscribersTotal
    ) {
        this(sseEventExecutor, maxSubscribersPerSymbol, maxSubscribersTotal, NoopSseTelemetry.INSTANCE);
    }

    public SseSubscriptionPermit reserve(String symbol) {
        if (!totalSubscriberLimit.tryAcquire()) {
            recordConnectionRejected("total_limit");
            throw new CoreException(ErrorCode.TOO_MANY_REQUESTS);
        }

        Semaphore symbolLimit = symbolSubscriberLimits.computeIfAbsent(
                symbol,
                key -> new Semaphore(maxSubscribersPerSymbol, true)
        );
        if (!symbolLimit.tryAcquire()) {
            totalSubscriberLimit.release();
            recordConnectionRejected("symbol_limit");
            throw new CoreException(ErrorCode.TOO_MANY_REQUESTS, "실시간 스트림 연결이 너무 많습니다: " + symbol);
        }

        return new SseSubscriptionPermit(symbol, symbolLimit);
    }

    public void register(SseSubscriptionPermit permit, SseEmitter emitter) {
        bindLifecycle(permit, emitter);
        emitters.computeIfAbsent(permit.symbol(), key -> new CopyOnWriteArrayList<>()).add(emitter);
        permit.markRegistered();
        recordConnectionOpened();
    }

    public void unregister(String symbol, SseEmitter emitter) {
        unregister(symbol, emitter, "client_complete");
    }

    private void unregister(String symbol, SseEmitter emitter, String reason) {
        CopyOnWriteArrayList<SseEmitter> symbolEmitters = emitters.get(symbol);
        if (symbolEmitters == null) {
            return;
        }

        boolean removed = symbolEmitters.remove(emitter);
        if (symbolEmitters.isEmpty()) {
            emitters.remove(symbol, symbolEmitters);
        }
        if (removed) {
            release(symbol);
            recordConnectionClosed(reason);
        }
    }

    public void release(SseSubscriptionPermit permit) {
        if (permit.markReleased()) {
            release(permit.symbol());
        }
    }

    @EventListener
    public void onMarketUpdated(MarketSummaryUpdatedEvent event) {
        String symbol = event.result().symbol();
        CopyOnWriteArrayList<SseEmitter> symbolEmitters = emitters.get(symbol);
        if (symbolEmitters == null || symbolEmitters.isEmpty()) {
            return;
        }

        MarketSummaryResponse response = MarketSummaryResponse.from(event.result());
        try {
            sseEventExecutor.execute(() -> sendToSubscribers(symbol, symbolEmitters, response));
        } catch (RejectedExecutionException exception) {
            log.debug("Market SSE executor rejected fan-out. symbol={}", symbol, exception);
            recordExecutorRejected();
        }
    }

    private void bindLifecycle(SseSubscriptionPermit permit, SseEmitter emitter) {
        emitter.onCompletion(() -> unregister(permit.symbol(), emitter, "client_complete"));
        emitter.onTimeout(() -> {
            unregister(permit.symbol(), emitter, "timeout");
            emitter.complete();
        });
        emitter.onError(error -> {
            log.debug("Market SSE emitter reported an error; closing subscription. symbol={}", permit.symbol(), error);
            unregister(permit.symbol(), emitter, "error");
        });
    }

    private void send(String symbol, SseEmitter emitter, MarketSummaryResponse response) {
        long startedAt = System.nanoTime();
        try {
            emitter.send(response);
            recordSend("success", startedAt);
        } catch (IOException exception) {
            log.debug("Market SSE send failed; closing subscription. symbol={}", symbol, exception);
            recordSend("failure", startedAt);
            unregister(symbol, emitter, "send_failure");
        }
    }

    private void sendToSubscribers(String symbol, CopyOnWriteArrayList<SseEmitter> symbolEmitters,
                                   MarketSummaryResponse response) {
        symbolEmitters.forEach(emitter -> send(symbol, emitter, response));
    }

    private void release(String symbol) {
        totalSubscriberLimit.release();
        Semaphore symbolLimit = symbolSubscriberLimits.get(symbol);
        if (symbolLimit != null) {
            symbolLimit.release();
            cleanupSymbolLimit(symbol, symbolLimit);
        }
    }

    private void cleanupSymbolLimit(String symbol, Semaphore symbolLimit) {
        if (symbolLimit.availablePermits() == maxSubscribersPerSymbol && !emitters.containsKey(symbol)) {
            symbolSubscriberLimits.remove(symbol, symbolLimit);
        }
    }

    private void recordConnectionOpened() {
        try {
            sseTelemetry.connectionOpened(STREAM);
        } catch (RuntimeException exception) {
            log.warn("Failed to record market SSE connection open telemetry", exception);
        }
    }

    private void recordConnectionClosed(String reason) {
        try {
            sseTelemetry.connectionClosed(STREAM, reason);
        } catch (RuntimeException exception) {
            log.warn("Failed to record market SSE connection close telemetry. reason={}", reason, exception);
        }
    }

    private void recordConnectionRejected(String reason) {
        try {
            sseTelemetry.connectionRejected(STREAM, reason);
        } catch (RuntimeException exception) {
            log.warn("Failed to record market SSE connection rejection telemetry. reason={}", reason, exception);
        }
    }

    private void recordSend(String result, long startedAt) {
        try {
            sseTelemetry.sendRecorded(STREAM, result, Duration.ofNanos(System.nanoTime() - startedAt));
        } catch (RuntimeException exception) {
            log.warn("Failed to record market SSE send telemetry. result={}", result, exception);
        }
    }

    private void recordExecutorRejected() {
        try {
            sseTelemetry.executorRejected(STREAM);
        } catch (RuntimeException exception) {
            log.warn("Failed to record market SSE executor rejection telemetry", exception);
        }
    }

    public static final class SseSubscriptionPermit {
        private final String symbol;
        private final Semaphore symbolLimit;
        private final AtomicBoolean released = new AtomicBoolean(false);
        private final AtomicBoolean registered = new AtomicBoolean(false);

        private SseSubscriptionPermit(String symbol, Semaphore symbolLimit) {
            this.symbol = symbol;
            this.symbolLimit = symbolLimit;
        }

        public String symbol() {
            return symbol;
        }

        private void markRegistered() {
            registered.set(true);
        }

        private boolean markReleased() {
            return !registered.get() && released.compareAndSet(false, true);
        }
    }
}
