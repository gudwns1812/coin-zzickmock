package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.web.SseSubscriptionRegistry;
import coin.coinzzickmock.common.web.SseSubscriptionRegistry.ReservationRejection;
import coin.coinzzickmock.feature.market.application.realtime.MarketCandleUpdatedEvent;
import coin.coinzzickmock.feature.market.application.realtime.MarketHistoryFinalizedEvent;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleProjector;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import coin.coinzzickmock.providers.telemetry.NoopSseTelemetry;
import coin.coinzzickmock.providers.telemetry.SseTelemetry;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class MarketCandleRealtimeSseBroker {
    private static final String STREAM = "market_candle";

    private final SseSubscriptionRegistry<SubscriptionKey> subscriptions;
    private final Set<SubscriptionKey> activeKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Executor sseEventExecutor;
    private final RealtimeMarketCandleProjector realtimeMarketCandleProjector;
    private final MarketHistoryRepository marketHistoryRepository;
    private final SseTelemetry sseTelemetry;

    @Autowired
    public MarketCandleRealtimeSseBroker(
            @Qualifier("marketRealtimeSseEventExecutor") Executor sseEventExecutor,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector,
            MarketHistoryRepository marketHistoryRepository,
            @Value("${coin.market.sse.max-subscribers-per-symbol:50}") int maxSubscribersPerKey,
            @Value("${coin.market.sse.max-total-subscribers:100}") int maxSubscribersTotal,
            SseTelemetry sseTelemetry
    ) {
        this.sseEventExecutor = sseEventExecutor;
        this.realtimeMarketCandleProjector = realtimeMarketCandleProjector;
        this.marketHistoryRepository = marketHistoryRepository;
        this.subscriptions = new SseSubscriptionRegistry<>(maxSubscribersPerKey, maxSubscribersTotal);
        this.sseTelemetry = sseTelemetry;
    }

    MarketCandleRealtimeSseBroker(
            Executor sseEventExecutor,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector
    ) {
        this(sseEventExecutor, realtimeMarketCandleProjector, null, 50, 100, NoopSseTelemetry.INSTANCE);
    }

    MarketCandleRealtimeSseBroker(
            Executor sseEventExecutor,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector,
            SseTelemetry sseTelemetry
    ) {
        this(sseEventExecutor, realtimeMarketCandleProjector, null, 50, 100, sseTelemetry);
    }

    MarketCandleRealtimeSseBroker(
            Executor sseEventExecutor,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector,
            int maxSubscribersPerKey,
            int maxSubscribersTotal
    ) {
        this(
                sseEventExecutor,
                realtimeMarketCandleProjector,
                null,
                maxSubscribersPerKey,
                maxSubscribersTotal,
                NoopSseTelemetry.INSTANCE
        );
    }

    MarketCandleRealtimeSseBroker(
            Executor sseEventExecutor,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector,
            MarketHistoryRepository marketHistoryRepository,
            SseTelemetry sseTelemetry
    ) {
        this(sseEventExecutor, realtimeMarketCandleProjector, marketHistoryRepository, 50, 100, sseTelemetry);
    }

    public SseSubscriptionPermit reserve(SubscriptionKey key) {
        var reservation = subscriptions.reserve(key);
        if (reservation.accepted()) {
            return new SseSubscriptionPermit(reservation.permit());
        }
        if (reservation.rejection() == ReservationRejection.TOTAL_LIMIT) {
            recordConnectionRejected("total_limit");
            throw new CoreException(ErrorCode.TOO_MANY_REQUESTS);
        }

        recordConnectionRejected("key_limit");
        throw new CoreException(ErrorCode.TOO_MANY_REQUESTS);
    }

    public void register(SseSubscriptionPermit permit, SseEmitter emitter) {
        SubscriptionKey key = permit.key();
        boolean registered = false;
        try {
            synchronized (activeKeys) {
                subscriptions.register(permit.delegate, emitter);
                activeKeys.add(key);
                registered = true;
            }
            bindLifecycle(key, emitter);
        } catch (RuntimeException exception) {
            if (registered) {
                discardRegisteredSubscription(key, emitter);
            } else {
                release(permit);
                cleanupActiveKey(key);
            }
            throw exception;
        }
        recordConnectionOpened();
    }

    public void register(String symbol, MarketCandleInterval interval, SseEmitter emitter) {
        SubscriptionKey key = new SubscriptionKey(symbol, interval);
        register(reserve(key), emitter);
    }

    public void unregister(SubscriptionKey key, SseEmitter emitter) {
        unregister(key, emitter, "client_complete");
    }

    private void unregister(SubscriptionKey key, SseEmitter emitter, String reason) {
        if (subscriptions.unregister(key, emitter)) {
            cleanupActiveKey(key);
            recordConnectionClosed(reason);
        }
    }

    public void release(SseSubscriptionPermit permit) {
        subscriptions.release(permit.delegate);
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
        CopyOnWriteArrayList<SseEmitter> keyEmitters = subscriptions.subscribers(key);
        if (keyEmitters == null || keyEmitters.isEmpty()) {
            return;
        }
        realtimeMarketCandleProjector.latest(key.symbol(), key.interval())
                .map(MarketCandleResponse::from)
                .ifPresent(response -> executeFanOut(key, keyEmitters, response));
    }

    private void fanOut(SubscriptionKey key, Object response) {
        CopyOnWriteArrayList<SseEmitter> keyEmitters = subscriptions.subscribers(key);
        if (keyEmitters == null || keyEmitters.isEmpty()) {
            return;
        }
        executeFanOut(key, keyEmitters, response);
    }

    private void executeFanOut(
            SubscriptionKey key,
            CopyOnWriteArrayList<SseEmitter> keyEmitters,
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

    private void bindLifecycle(SubscriptionKey key, SseEmitter emitter) {
        emitter.onCompletion(() -> unregister(key, emitter, "client_complete"));
        emitter.onTimeout(() -> {
            unregister(key, emitter, "timeout");
            emitter.complete();
        });
        emitter.onError(error -> {
            log.debug("Market candle SSE emitter reported an error; closing subscription. stream={} symbol={} interval={}",
                    STREAM, key.symbol(), key.interval().value(), error);
            unregister(key, emitter, "error");
        });
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
        CopyOnWriteArrayList<SseEmitter> keyEmitters = subscriptions.subscribers(key);
        return keyEmitters == null || keyEmitters.isEmpty();
    }

    private List<String> affectedIntervals(MarketHistoryFinalizedEvent event) {
        List<String> affectedIntervals = new ArrayList<>();
        affectedIntervals.add(MarketCandleInterval.ONE_MINUTE.value());
        affectedIntervals.add(MarketCandleInterval.THREE_MINUTES.value());
        affectedIntervals.add(MarketCandleInterval.FIVE_MINUTES.value());
        affectedIntervals.add(MarketCandleInterval.FIFTEEN_MINUTES.value());
        if (completedHourVisible(event)) {
            affectedIntervals.add(MarketCandleInterval.ONE_HOUR.value());
        }
        return affectedIntervals;
    }

    private boolean completedHourVisible(MarketHistoryFinalizedEvent event) {
        if (marketHistoryRepository == null) {
            return false;
        }
        Long symbolId = marketHistoryRepository.findSymbolIdsBySymbols(List.of(event.symbol())).get(event.symbol());
        if (symbolId == null) {
            return false;
        }

        Instant hourOpenTime = MarketTime.truncate(event.openTime(), ChronoUnit.HOURS);
        return !marketHistoryRepository.findCompletedHourlyCandles(
                symbolId,
                hourOpenTime,
                hourOpenTime.plus(1, ChronoUnit.HOURS)
        ).isEmpty();
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
    }
}
