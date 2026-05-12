package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.common.web.SseDeliveryExecutor;
import coin.coinzzickmock.common.web.SseEmitterLifecycle;
import coin.coinzzickmock.common.web.SseSubscriptionLimitExceededException;
import coin.coinzzickmock.common.web.SseSubscriptionRegistry;
import coin.coinzzickmock.common.web.SseSubscriptionRegistry.ReservationRejection;
import coin.coinzzickmock.providers.telemetry.SseTelemetry;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class MarketCandleRealtimeSseBroker {
    private static final String STREAM = "market_candle";

    private final SseSubscriptionRegistry<SubscriptionKey> subscriptions;
    private final Set<SubscriptionKey> activeKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final SseDeliveryExecutor sseEventExecutor;
    private final MarketCandleSnapshotReader marketCandleSnapshotReader;
    private final MarketFinalizedCandleIntervalsReader finalizedCandleIntervalsReader;
    private final SseTelemetry sseTelemetry;

    @Autowired
    public MarketCandleRealtimeSseBroker(
            SseDeliveryExecutor sseEventExecutor,
            MarketCandleSnapshotReader marketCandleSnapshotReader,
            MarketFinalizedCandleIntervalsReader finalizedCandleIntervalsReader,
            @Value("${coin.market.sse.max-subscribers-per-symbol:50}") int maxSubscribersPerKey,
            @Value("${coin.market.sse.max-total-subscribers:100}") int maxSubscribersTotal,
            SseTelemetry sseTelemetry
    ) {
        this.sseEventExecutor = sseEventExecutor;
        this.marketCandleSnapshotReader = marketCandleSnapshotReader;
        this.finalizedCandleIntervalsReader = finalizedCandleIntervalsReader;
        this.subscriptions = new SseSubscriptionRegistry<>(maxSubscribersPerKey, maxSubscribersTotal);
        this.sseTelemetry = sseTelemetry;
    }

    MarketCandleRealtimeSseBroker(
            Executor sseEventExecutor,
            MarketCandleSnapshotReader marketCandleSnapshotReader,
            MarketFinalizedCandleIntervalsReader finalizedCandleIntervalsReader,
            int maxSubscribersPerKey,
            int maxSubscribersTotal,
            SseTelemetry sseTelemetry
    ) {
        this(
                new SseDeliveryExecutor(sseEventExecutor),
                marketCandleSnapshotReader,
                finalizedCandleIntervalsReader,
                maxSubscribersPerKey,
                maxSubscribersTotal,
                sseTelemetry
        );
    }

    MarketCandleRealtimeSseBroker(
            Executor sseEventExecutor,
            MarketCandleSnapshotReader marketCandleSnapshotReader,
            MarketFinalizedCandleIntervalsReader finalizedCandleIntervalsReader,
            SseTelemetry sseTelemetry
    ) {
        this(
                new SseDeliveryExecutor(sseEventExecutor),
                marketCandleSnapshotReader,
                finalizedCandleIntervalsReader,
                50,
                100,
                sseTelemetry
        );
    }

    public SseSubscriptionPermit reserve(SubscriptionKey key) {
        return reserve(subscriptions.reserve(key));
    }

    public SseSubscriptionPermit reserve(SubscriptionKey key, String clientKey) {
        return reserve(subscriptions.reserve(key, clientKey));
    }

    private SseSubscriptionPermit reserve(SseSubscriptionRegistry.Reservation<SubscriptionKey> reservation) {
        if (reservation.accepted()) {
            return new SseSubscriptionPermit(reservation.permit());
        }
        reject(reservation.rejection());
        throw new SseSubscriptionLimitExceededException(
                reservation.rejection() == ReservationRejection.TOTAL_LIMIT ? "total_limit" : "key_limit"
        );
    }

    public void register(SseSubscriptionPermit permit, SseEmitter emitter) {
        SubscriptionKey key = permit.key();
        try {
            bindLifecycle(permit, emitter);
            var registration = subscriptions.register(permit.delegate, emitter);
            if (!registration.registered()) {
                reject(registration.rejection());
                throw new SseSubscriptionLimitExceededException(
                        registration.rejection() == ReservationRejection.TOTAL_LIMIT ? "total_limit" : "key_limit"
                );
            }
            activeKeys.add(key);
            recordConnectionOpened();
            logLifecycle(key, "register", null);
            completeReplacedEmitter(key, registration.replacedEmitter());
        } catch (RuntimeException exception) {
            discardRegisteredSubscription(key, emitter);
            release(permit);
            cleanupActiveKey(key);
            throw exception;
        }
    }

    public void register(String symbol, String interval, SseEmitter emitter) {
        SubscriptionKey key = new SubscriptionKey(symbol, interval);
        register(reserve(key), emitter);
    }

    public void register(String symbol, String interval, String clientKey, SseEmitter emitter) {
        SubscriptionKey key = new SubscriptionKey(symbol, interval);
        register(reserve(key, clientKey), emitter);
    }

    public void unregister(SubscriptionKey key, SseEmitter emitter) {
        unregister(key, emitter, "client_complete");
    }

    private void unregister(SubscriptionKey key, SseEmitter emitter, String reason) {
        if (subscriptions.unregister(key, emitter)) {
            cleanupActiveKey(key);
            recordConnectionClosed(reason);
            logLifecycle(key, lifecycleAction(reason), reason);
        }
    }

    private void unregister(SubscriptionKey key, String clientKey, SseEmitter emitter, String reason) {
        if (subscriptions.unregister(key, clientKey, emitter)) {
            cleanupActiveKey(key);
            recordConnectionClosed(reason);
            logLifecycle(key, lifecycleAction(reason), reason);
        }
    }

    public void release(SseSubscriptionPermit permit) {
        if (subscriptions.release(permit.delegate)) {
            logLifecycle(permit.key(), "release", "before_register");
        }
    }

    public void onCandleUpdated(String symbol) {
        activeKeySnapshot().stream()
                .filter(key -> key.symbol().equals(symbol))
                .forEach(this::fanOutLatest);
    }

    public void onHistoryFinalized(String symbol, Instant openTime, Instant closeTime) {
        List<String> affectedIntervals = finalizedCandleIntervalsReader.readAffectedIntervals(symbol, openTime, closeTime);
        if (affectedIntervals.isEmpty()) {
            return;
        }
        MarketCandleHistoryFinalizedResponse response = MarketCandleHistoryFinalizedResponse.of(
                symbol,
                openTime,
                closeTime,
                affectedIntervals
        );
        activeKeySnapshot().stream()
                .filter(key -> key.symbol().equals(symbol))
                .filter(key -> affectedIntervals.contains(key.interval().value()))
                .forEach(key -> fanOut(key, response));
    }

    private void fanOutLatest(SubscriptionKey key) {
        List<SseEmitter> keyEmitters = subscriptions.subscribers(key);
        if (keyEmitters.isEmpty()) {
            return;
        }
        marketCandleSnapshotReader.latest(key.symbol(), key.interval().value())
                .ifPresent(response -> executeFanOut(key, keyEmitters, response));
    }

    private void fanOut(SubscriptionKey key, Object response) {
        List<SseEmitter> keyEmitters = subscriptions.subscribers(key);
        if (keyEmitters.isEmpty()) {
            return;
        }
        executeFanOut(key, keyEmitters, response);
    }

    private void executeFanOut(SubscriptionKey key, List<SseEmitter> keyEmitters, Object response) {
        try {
            sseEventExecutor.execute(() -> keyEmitters.forEach(emitter -> send(key, emitter, response)));
        } catch (RejectedExecutionException exception) {
            log.debug("Market candle SSE executor rejected fan-out. stream={} symbol={} interval={}",
                    STREAM, key.symbol(), key.interval().value(), exception);
            recordExecutorRejected();
        }
    }

    private void bindLifecycle(SseSubscriptionPermit permit, SseEmitter emitter) {
        SubscriptionKey key = permit.key();
        SseEmitterLifecycle.bind(
                emitter,
                () -> unregister(key, permit.clientKey(), emitter, "client_complete"),
                () -> unregister(key, permit.clientKey(), emitter, "timeout"),
                error -> {
                    log.debug("Market candle SSE emitter reported an error; closing subscription. stream={} symbol={} interval={}",
                            STREAM, key.symbol(), key.interval().value(), error);
                    unregister(key, permit.clientKey(), emitter, "error");
                }
        );
    }

    private void send(SubscriptionKey key, SseEmitter emitter, Object response) {
        long startedAt = System.nanoTime();
        try {
            emitter.send(response);
            recordSend("success", startedAt);
        } catch (IOException | IllegalStateException exception) {
            log.debug("Market candle SSE send failed; closing subscription. stream={} symbol={} interval={} reason=send_failure",
                    STREAM, key.symbol(), key.interval().value(), exception);
            recordSend("failure", startedAt);
            unregister(key, emitter, "send_failure");
        }
    }

    private List<SubscriptionKey> activeKeySnapshot() {
        return List.copyOf(activeKeys);
    }

    private void cleanupActiveKey(SubscriptionKey key) {
        if (isSubscriptionEmpty(key)) {
            activeKeys.remove(key);
        }
    }

    private void discardRegisteredSubscription(SubscriptionKey key, SseEmitter emitter) {
        if (subscriptions.unregister(key, emitter)) {
            cleanupActiveKey(key);
        }
    }

    private boolean isSubscriptionEmpty(SubscriptionKey key) {
        return subscriptions.subscribers(key).isEmpty();
    }

    private void reject(ReservationRejection rejection) {
        if (rejection == ReservationRejection.TOTAL_LIMIT) {
            recordConnectionRejected("total_limit");
            return;
        }
        recordConnectionRejected("key_limit");
    }

    private void completeReplacedEmitter(SubscriptionKey key, SseEmitter replacedEmitter) {
        if (replacedEmitter == null) {
            return;
        }
        SseEmitterLifecycle.completeSilently(replacedEmitter);
        recordConnectionClosed("client_replaced");
        logLifecycle(key, "replace", "client_replaced");
    }

    private void logLifecycle(SubscriptionKey key, String action, String reason) {
        log.info(
                "SSE lifecycle stream={} keyType=symbol_interval symbol={} interval={} action={} reason={} activeKeyEmitters={} activeTotalEmitters={}",
                STREAM,
                key.symbol(),
                key.interval().value(),
                action,
                reason,
                subscriptions.subscriberCount(key),
                subscriptions.totalSubscriberCount()
        );
    }

    private String lifecycleAction(String reason) {
        if ("client_complete".equals(reason)) {
            return "complete";
        }
        return reason;
    }

    private void recordConnectionOpened() {
        try {
            sseTelemetry.connectionOpened(STREAM);
        } catch (RuntimeException exception) {
            log.warn("Failed to record market candle SSE connection open telemetry", exception);
        }
    }

    private void recordConnectionClosed(String reason) {
        try {
            sseTelemetry.connectionClosed(STREAM, reason);
        } catch (RuntimeException exception) {
            log.warn("Failed to record market candle SSE connection close telemetry. reason={}", reason, exception);
        }
    }

    private void recordConnectionRejected(String reason) {
        try {
            sseTelemetry.connectionRejected(STREAM, reason);
        } catch (RuntimeException exception) {
            log.warn("Failed to record market candle SSE connection rejection telemetry. reason={}", reason, exception);
        }
    }

    private void recordSend(String result, long startedAt) {
        try {
            sseTelemetry.sendRecorded(STREAM, result, Duration.ofNanos(System.nanoTime() - startedAt));
        } catch (RuntimeException exception) {
            log.warn("Failed to record market candle SSE send telemetry. result={}", result, exception);
        }
    }

    private void recordExecutorRejected() {
        try {
            sseTelemetry.executorRejected(STREAM);
        } catch (RuntimeException exception) {
            log.warn("Failed to record market candle SSE executor rejection telemetry", exception);
        }
    }

    public record SubscriptionKey(String symbol, CandleInterval interval) {
        public SubscriptionKey(String symbol, String interval) {
            this(symbol, new CandleInterval(interval));
        }

        public SubscriptionKey {
            if (symbol == null || symbol.isBlank()) {
                throw new IllegalArgumentException("symbol is required");
            }
            if (interval == null) {
                throw new IllegalArgumentException("interval is required");
            }
            symbol = symbol.toUpperCase();
        }
    }

    public record CandleInterval(String value) {
        public CandleInterval {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("interval is required");
            }
            value = value.trim();
        }
    }

    boolean hasSubscriberLimit(SubscriptionKey key) {
        return subscriptions.hasSubscriberLimit(key);
    }

    public static final class SseSubscriptionPermit {
        private final SseSubscriptionRegistry.Permit<SubscriptionKey> delegate;

        private SseSubscriptionPermit(SseSubscriptionRegistry.Permit<SubscriptionKey> delegate) {
            this.delegate = delegate;
        }

        private SubscriptionKey key() {
            return delegate.key();
        }

        private String clientKey() {
            return delegate.clientKey();
        }
    }
}
