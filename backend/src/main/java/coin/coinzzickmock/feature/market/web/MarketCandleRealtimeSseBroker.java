package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.web.SseDeliveryExecutor;
import coin.coinzzickmock.common.web.SseSubscriptionRegistry;
import coin.coinzzickmock.common.web.SseSubscriptionRegistry.ReservationRejection;
import coin.coinzzickmock.feature.market.application.realtime.MarketCandleUpdatedEvent;
import coin.coinzzickmock.feature.market.application.realtime.MarketHistoryFinalizedEvent;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleProjector;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import coin.coinzzickmock.providers.telemetry.NoopSseTelemetry;
import coin.coinzzickmock.providers.telemetry.SseTelemetry;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private static final List<MarketCandleInterval> COMPLETED_HOURLY_INTERVALS = List.of(
            MarketCandleInterval.ONE_HOUR,
            MarketCandleInterval.FOUR_HOURS,
            MarketCandleInterval.TWELVE_HOURS,
            MarketCandleInterval.ONE_DAY,
            MarketCandleInterval.ONE_WEEK,
            MarketCandleInterval.ONE_MONTH
    );

    private final SseSubscriptionRegistry<SubscriptionKey> subscriptions;
    private final Set<SubscriptionKey> activeKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final SseDeliveryExecutor sseEventExecutor;
    private final RealtimeMarketCandleProjector realtimeMarketCandleProjector;
    private final MarketHistoryRepository marketHistoryRepository;
    private final SseTelemetry sseTelemetry;

    @Autowired
    public MarketCandleRealtimeSseBroker(
            SseDeliveryExecutor sseEventExecutor,
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
        this(new SseDeliveryExecutor(sseEventExecutor), realtimeMarketCandleProjector, null, 50, 100, NoopSseTelemetry.INSTANCE);
    }

    MarketCandleRealtimeSseBroker(
            Executor sseEventExecutor,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector,
            SseTelemetry sseTelemetry
    ) {
        this(new SseDeliveryExecutor(sseEventExecutor), realtimeMarketCandleProjector, null, 50, 100, sseTelemetry);
    }

    MarketCandleRealtimeSseBroker(
            Executor sseEventExecutor,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector,
            int maxSubscribersPerKey,
            int maxSubscribersTotal
    ) {
        this(
                new SseDeliveryExecutor(sseEventExecutor),
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
            int maxSubscribersPerKey,
            int maxSubscribersTotal,
            SseTelemetry sseTelemetry
    ) {
        this(
                new SseDeliveryExecutor(sseEventExecutor),
                realtimeMarketCandleProjector,
                marketHistoryRepository,
                maxSubscribersPerKey,
                maxSubscribersTotal,
                sseTelemetry
        );
    }

    MarketCandleRealtimeSseBroker(
            Executor sseEventExecutor,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector,
            MarketHistoryRepository marketHistoryRepository,
            SseTelemetry sseTelemetry
    ) {
        this(new SseDeliveryExecutor(sseEventExecutor), realtimeMarketCandleProjector, marketHistoryRepository, 50, 100, sseTelemetry);
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
        emitter.onCompletion(() -> unregister(key, permit.clientKey(), emitter, "client_complete"));
        emitter.onTimeout(() -> {
            unregister(key, permit.clientKey(), emitter, "timeout");
            emitter.complete();
        });
        emitter.onError(error -> {
            log.debug("Market candle SSE emitter reported an error; closing subscription. stream={} symbol={} interval={}",
                    STREAM, key.symbol(), key.interval().value(), error);
            unregister(key, permit.clientKey(), emitter, "error");
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
        List<SseEmitter> keyEmitters = subscriptions.subscribers(key);
        return keyEmitters.isEmpty();
    }

    private List<String> affectedIntervals(MarketHistoryFinalizedEvent event) {
        List<String> affectedIntervals = new ArrayList<>();
        affectedIntervals.add(MarketCandleInterval.ONE_MINUTE.value());
        affectedIntervals.add(MarketCandleInterval.THREE_MINUTES.value());
        affectedIntervals.add(MarketCandleInterval.FIVE_MINUTES.value());
        affectedIntervals.add(MarketCandleInterval.FIFTEEN_MINUTES.value());
        Long symbolId = symbolId(event);
        if (symbolId == null) {
            return affectedIntervals;
        }
        Map<MarketCandleInterval, BucketRange> bucketRanges = bucketRanges(event.openTime());
        List<HourlyMarketCandle> completedHourlyCandles = completedHourlyCandles(symbolId, bucketRanges);
        for (MarketCandleInterval interval : COMPLETED_HOURLY_INTERVALS) {
            if (isCompletedBucketVisible(completedHourlyCandles, bucketRanges.get(interval))) {
                affectedIntervals.add(interval.value());
            }
        }
        return affectedIntervals;
    }

    private Long symbolId(MarketHistoryFinalizedEvent event) {
        if (marketHistoryRepository == null) {
            return null;
        }
        Map<String, Long> symbolIds = marketHistoryRepository.findSymbolIdsBySymbols(List.of(event.symbol()));
        Long symbolId = symbolIds == null ? null : symbolIds.get(event.symbol());
        if (symbolId == null) {
            log.warn("Symbol not found during market history finalization. symbol={} openTime={} closeTime={}",
                    event.symbol(), event.openTime(), event.closeTime());
        }
        return symbolId;
    }

    private Map<MarketCandleInterval, BucketRange> bucketRanges(Instant eventOpenTime) {
        Map<MarketCandleInterval, BucketRange> bucketRanges = new LinkedHashMap<>();
        for (MarketCandleInterval interval : COMPLETED_HOURLY_INTERVALS) {
            Instant bucketOpenTime = bucketOpenTime(eventOpenTime, interval);
            Instant bucketCloseTime = bucketCloseTime(bucketOpenTime, interval);
            bucketRanges.put(interval, new BucketRange(
                    bucketOpenTime,
                    bucketCloseTime,
                    expectedHourlyCandles(bucketOpenTime, bucketCloseTime)
            ));
        }
        return bucketRanges;
    }

    private List<HourlyMarketCandle> completedHourlyCandles(
            long symbolId,
            Map<MarketCandleInterval, BucketRange> bucketRanges
    ) {
        Instant fromInclusive = bucketRanges.values().stream()
                .map(BucketRange::openTime)
                .min(Instant::compareTo)
                .orElseThrow();
        Instant toExclusive = bucketRanges.values().stream()
                .map(BucketRange::closeTime)
                .max(Instant::compareTo)
                .orElseThrow();
        List<HourlyMarketCandle> completedHourlyCandles = marketHistoryRepository.findCompletedHourlyCandles(
                symbolId,
                fromInclusive,
                toExclusive
        );
        return completedHourlyCandles == null ? List.of() : completedHourlyCandles;
    }

    private boolean isCompletedBucketVisible(List<HourlyMarketCandle> completedHourlyCandles, BucketRange bucketRange) {
        long completedCount = completedHourlyCandles.stream()
                .filter(Objects::nonNull)
                .filter(candle -> candle.openTime() != null)
                .filter(candle -> !candle.openTime().isBefore(bucketRange.openTime()))
                .filter(candle -> candle.openTime().isBefore(bucketRange.closeTime()))
                .count();
        return completedCount >= bucketRange.expectedHourlyCandles();
    }

    private Instant bucketOpenTime(Instant eventOpenTime, MarketCandleInterval interval) {
        if (interval == MarketCandleInterval.ONE_HOUR) {
            return MarketTime.truncate(eventOpenTime, ChronoUnit.HOURS);
        }
        return MarketTime.bucketStart(eventOpenTime, interval);
    }

    private Instant bucketCloseTime(Instant bucketOpenTime, MarketCandleInterval interval) {
        if (interval == MarketCandleInterval.ONE_HOUR) {
            return bucketOpenTime.plus(1, ChronoUnit.HOURS);
        }
        return MarketTime.bucketClose(bucketOpenTime, interval);
    }

    private int expectedHourlyCandles(Instant bucketOpenTime, Instant bucketCloseTime) {
        return (int) ChronoUnit.HOURS.between(bucketOpenTime, bucketCloseTime);
    }

    private record BucketRange(Instant openTime, Instant closeTime, int expectedHourlyCandles) {
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
        try {
            replacedEmitter.complete();
        } catch (RuntimeException ignored) {
            // The replaced client may already be closed.
        }
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
