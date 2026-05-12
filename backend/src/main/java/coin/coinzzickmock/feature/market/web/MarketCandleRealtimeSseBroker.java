package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.web.SseDeliveryExecutor;
import coin.coinzzickmock.common.web.SseEmitterLifecycle;
import coin.coinzzickmock.common.web.SseSubscriptionRegistry;
import coin.coinzzickmock.common.web.SseSubscriptionRegistry.ReservationRejection;
import coin.coinzzickmock.feature.market.application.query.FinalizedCandleIntervalsReader;
import coin.coinzzickmock.feature.market.application.realtime.MarketCandleUpdatedEvent;
import coin.coinzzickmock.feature.market.application.realtime.MarketHistoryFinalizedEvent;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleProjector;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.providers.telemetry.SseTelemetry;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class MarketCandleRealtimeSseBroker {
    private static final String STREAM = "market_candle";
    private static final String CLIENT_REPLACED_REASON = "client_replaced";

    private final SseSubscriptionRegistry<SubscriptionKey> subscriptions;
    private final Set<SubscriptionKey> activeKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final SseDeliveryExecutor sseEventExecutor;
    private final RealtimeMarketCandleProjector realtimeMarketCandleProjector;
    private final FinalizedCandleIntervalsReader finalizedCandleIntervalsReader;
    private final SseTelemetry sseTelemetry;

    @Autowired
    public MarketCandleRealtimeSseBroker(
            SseDeliveryExecutor sseEventExecutor,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector,
            FinalizedCandleIntervalsReader finalizedCandleIntervalsReader,
            @Value("${coin.market.sse.max-subscribers-per-symbol:50}") int maxSubscribersPerKey,
            @Value("${coin.market.sse.max-total-subscribers:100}") int maxSubscribersTotal,
            SseTelemetry sseTelemetry
    ) {
        this.sseEventExecutor = sseEventExecutor;
        this.realtimeMarketCandleProjector = realtimeMarketCandleProjector;
        this.finalizedCandleIntervalsReader = finalizedCandleIntervalsReader;
        this.subscriptions = new SseSubscriptionRegistry<>(maxSubscribersPerKey, maxSubscribersTotal);
        this.sseTelemetry = sseTelemetry;
    }

    MarketCandleRealtimeSseBroker(
            Executor sseEventExecutor,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector,
            FinalizedCandleIntervalsReader finalizedCandleIntervalsReader,
            int maxSubscribersPerKey,
            int maxSubscribersTotal,
            SseTelemetry sseTelemetry
    ) {
        this(
                new SseDeliveryExecutor(sseEventExecutor),
                realtimeMarketCandleProjector,
                finalizedCandleIntervalsReader,
                maxSubscribersPerKey,
                maxSubscribersTotal,
                sseTelemetry
        );
    }

    MarketCandleRealtimeSseBroker(
            Executor sseEventExecutor,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector,
            FinalizedCandleIntervalsReader finalizedCandleIntervalsReader,
            SseTelemetry sseTelemetry
    ) {
        this(
                new SseDeliveryExecutor(sseEventExecutor),
                realtimeMarketCandleProjector,
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
        throw new CoreException(ErrorCode.TOO_MANY_REQUESTS);
    }

    public void register(SseSubscriptionPermit permit, SseEmitter emitter) {
        SubscriptionKey key = permit.key();
        try {
            bindLifecycle(permit, emitter);
            var registration = subscriptions.register(permit.delegate, emitter);
            if (!registration.registered()) {
                reject(registration.rejection());
                throw new CoreException(ErrorCode.TOO_MANY_REQUESTS);
            }
            synchronized (activeKeys) {
                activeKeys.add(key);
            }
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

    public void register(String symbol, MarketCandleInterval interval, SseEmitter emitter) {
        SubscriptionKey key = new SubscriptionKey(symbol, interval);
        register(reserve(key), emitter);
    }

    public void register(String symbol, MarketCandleInterval interval, String clientKey, SseEmitter emitter) {
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

    @EventListener
    public void onCandleUpdated(MarketCandleUpdatedEvent event) {
        activeKeySnapshot().stream()
                .filter(key -> key.symbol().equals(event.symbol()))
                .forEach(this::fanOutLatest);
    }

    @EventListener
    public void onHistoryFinalized(MarketHistoryFinalizedEvent event) {
        List<String> affectedIntervals = affectedIntervals(event);
        if (affectedIntervals.isEmpty()) {
            return;
        }

        MarketCandleHistoryFinalizedResponse response = MarketCandleHistoryFinalizedResponse.of(
                event.symbol(),
                event.openTime(),
                event.closeTime(),
                affectedIntervals
        );
        activeKeySnapshot().stream()
                .filter(key -> key.symbol().equals(event.symbol()))
                .filter(key -> affectedIntervals.contains(key.interval().value()))
                .forEach(key -> fanOut(key, response));
    }

    private void fanOutLatest(SubscriptionKey key) {
        List<SseEmitter> keyEmitters = subscriptions.subscribers(key);
        if (keyEmitters.isEmpty()) {
            return;
        }
        realtimeMarketCandleProjector.latest(key.symbol(), key.interval())
                .map(MarketCandleResponse::from)
                .ifPresent(response -> executeFanOut(key, keyEmitters, response));
    }

    private void fanOut(SubscriptionKey key, Object response) {
        List<SseEmitter> keyEmitters = subscriptions.subscribers(key);
        if (keyEmitters.isEmpty()) {
            return;
        }
        executeFanOut(key, keyEmitters, response);
    }

    private void executeFanOut(
            SubscriptionKey key,
            List<SseEmitter> keyEmitters,
            Object response
    ) {
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
                    log.debug(
                            "Market candle SSE emitter reported an error; closing subscription. stream={} symbol={} interval={}",
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
            log.debug(
                    "Market candle SSE send failed; closing subscription. stream={} symbol={} interval={} reason=send_failure",
                    STREAM, key.symbol(), key.interval().value(), exception);
            recordSend("failure", startedAt);
            unregister(key, emitter, "send_failure");
        }
    }

    private List<SubscriptionKey> activeKeySnapshot() {
        synchronized (activeKeys) {
            return List.copyOf(activeKeys);
        }
    }

    private void cleanupActiveKey(SubscriptionKey key) {
        synchronized (activeKeys) {
            if (isSubscriptionEmpty(key)) {
                activeKeys.remove(key);
            }
        }
    }

    private void discardRegisteredSubscription(SubscriptionKey key, SseEmitter emitter) {
        if (subscriptions.unregister(key, emitter)) {
            cleanupActiveKey(key);
        }
    }

    private boolean isSubscriptionEmpty(SubscriptionKey key) {
        List<SseEmitter> keyEmitters = subscriptions.subscribers(key);
        return keyEmitters.isEmpty();
    }

    private List<String> affectedIntervals(MarketHistoryFinalizedEvent event) {
        return finalizedCandleIntervalsReader.readAffectedIntervals(
                        event.symbol(),
                        event.openTime(),
                        event.closeTime()
                ).stream()
                .map(MarketCandleInterval::value)
                .toList();
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

    public record SubscriptionKey(String symbol, MarketCandleInterval interval) {
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
