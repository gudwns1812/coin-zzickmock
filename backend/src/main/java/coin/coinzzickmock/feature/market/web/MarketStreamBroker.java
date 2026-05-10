package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.feature.market.application.realtime.MarketCandleUpdatedEvent;
import coin.coinzzickmock.feature.market.application.realtime.MarketHistoryFinalizedEvent;
import coin.coinzzickmock.feature.market.application.realtime.MarketSummaryUpdatedEvent;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleProjector;
import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.position.application.event.PositionFullyClosedEvent;
import coin.coinzzickmock.feature.position.application.event.PositionOpenedEvent;
import coin.coinzzickmock.providers.telemetry.NoopSseTelemetry;
import coin.coinzzickmock.providers.telemetry.SseTelemetry;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class MarketStreamBroker {
    private static final String STREAM = "market_unified";
    private static final String CLIENT_REPLACED_REASON = "client_replaced";

    private final MarketStreamRegistry registry;
    private final Executor sseEventExecutor;
    private final RealtimeMarketCandleProjector realtimeMarketCandleProjector;
    private final SseTelemetry sseTelemetry;

    @Autowired
    public MarketStreamBroker(
            MarketStreamRegistry registry,
            @Qualifier("marketRealtimeSseEventExecutor") Executor sseEventExecutor,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector,
            SseTelemetry sseTelemetry
    ) {
        this.registry = registry;
        this.sseEventExecutor = sseEventExecutor;
        this.realtimeMarketCandleProjector = realtimeMarketCandleProjector;
        this.sseTelemetry = sseTelemetry;
    }

    MarketStreamBroker(
            MarketStreamRegistry registry,
            Executor sseEventExecutor,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector
    ) {
        this(registry, sseEventExecutor, realtimeMarketCandleProjector, NoopSseTelemetry.INSTANCE);
    }

    public boolean openStream(
            MarketStreamSessionKey sessionKey,
            SseEmitter emitter,
            String activeSymbol,
            Collection<String> openPositionSymbols,
            CandleSubscription candleSubscription,
            Supplier<List<MarketSummaryResult>> initialSummarySupplier
    ) {
        Set<String> openSymbols = new LinkedHashSet<>(openPositionSymbols);
        MarketStreamRegistry.Registration registration = registry.registerSession(
                sessionKey,
                emitter,
                activeSymbol,
                openSymbols,
                candleSubscription
        );
        bindLifecycle(sessionKey, emitter);
        recordConnectionOpened();
        logLifecycle("register", null, sessionKey, activeSymbol, candleSubscription);
        completeReplacedEmitter(registration.replacedEmitter());

        try {
            if (!sendInitialSnapshots(sessionKey, emitter, candleSubscription, initialSummarySupplier.get())) {
                closeSession(sessionKey, emitter, "initial_send_failure");
                return false;
            }
            return true;
        } catch (RuntimeException exception) {
            closeSession(sessionKey, emitter, "initial_snapshot_failure");
            throw exception;
        }
    }

    @EventListener
    public void onMarketUpdated(MarketSummaryUpdatedEvent event) {
        String symbol = event.result().symbol();
        List<MarketStreamSubscriber> subscribers = registry.sessionsForSummary(symbol);
        if (subscribers.isEmpty()) {
            return;
        }
        MarketStreamEventResponse response = MarketStreamEventResponse.summary(
                event.result(),
                MarketStreamEventSource.LIVE,
                Instant.now()
        );
        executeFanOut(symbol, subscribers, response);
    }

    @EventListener
    public void onCandleUpdated(MarketCandleUpdatedEvent event) {
        for (CandleSubscription subscription : registry.candleSubscriptionsForSymbol(event.symbol())) {
            realtimeMarketCandleProjector.latest(subscription.symbol(), subscription.interval())
                    .map(result -> MarketStreamEventResponse.candle(
                            subscription.symbol(),
                            subscription,
                            result,
                            MarketStreamEventSource.LIVE,
                            Instant.now()
                    ))
                    .ifPresent(response -> executeFanOut(
                            subscription.symbol(),
                            registry.sessionsForCandle(subscription),
                            response
                    ));
        }
    }

    @EventListener
    public void onHistoryFinalized(MarketHistoryFinalizedEvent event) {
        for (CandleSubscription subscription : registry.candleSubscriptionsForSymbol(event.symbol())) {
            if (!affectedIntervals(event).contains(subscription.interval())) {
                continue;
            }
            MarketCandleHistoryFinalizedResponse response = MarketCandleHistoryFinalizedResponse.of(
                    event.symbol(),
                    event.openTime(),
                    event.closeTime(),
                    List.of(subscription.interval().value())
            );
            executeFanOut(
                    subscription.symbol(),
                    registry.sessionsForCandle(subscription),
                    MarketStreamEventResponse.historyFinalized(subscription, response, Instant.now())
            );
        }
    }

    @EventListener
    public void onPositionOpened(PositionOpenedEvent event) {
        for (MarketStreamSessionKey sessionKey : registry.sessionKeysForMember(event.memberId())) {
            registry.addSummaryReason(sessionKey, event.symbol(), SummarySubscriptionReason.OPEN_POSITION);
        }
    }

    @EventListener
    public void onPositionFullyClosed(PositionFullyClosedEvent event) {
        for (MarketStreamSessionKey sessionKey : registry.sessionKeysForMember(event.memberId())) {
            registry.removeSummaryReason(sessionKey, event.symbol(), SummarySubscriptionReason.OPEN_POSITION);
        }
    }

    private boolean sendInitialSnapshots(
            MarketStreamSessionKey sessionKey,
            SseEmitter emitter,
            CandleSubscription candleSubscription,
            Supplier<List<MarketSummaryResult>> initialSummarySupplier
    ) {
        Instant serverTime = Instant.now();
        for (MarketSummaryResult summary : initialSummaries) {
            if (!send(sessionKey, emitter, MarketStreamEventResponse.summary(
                    summary,
                    MarketStreamEventSource.INITIAL_SNAPSHOT,
                    serverTime
            ))) {
                return false;
            }
        }
        return realtimeMarketCandleProjector.latest(candleSubscription.symbol(), candleSubscription.interval())
                .map(candle -> send(sessionKey, emitter, MarketStreamEventResponse.candle(
                        candleSubscription.symbol(),
                        candleSubscription,
                        candle,
                        MarketStreamEventSource.INITIAL_SNAPSHOT,
                        serverTime
                )))
                .orElse(true);
    }

    private void executeFanOut(String symbol, List<MarketStreamSubscriber> subscribers, Object response) {
        if (subscribers.isEmpty()) {
            return;
        }
        try {
            sseEventExecutor.execute(() -> subscribers.forEach(subscriber -> send(subscriber.sessionKey(), subscriber.emitter(), response)));
        } catch (RejectedExecutionException exception) {
            log.debug("Unified market SSE executor rejected fan-out. stream={} symbol={}", STREAM, symbol, exception);
            recordExecutorRejected();
        }
    }

    private boolean send(MarketStreamSessionKey sessionKey, SseEmitter emitter, Object response) {
        long startedAt = System.nanoTime();
        try {
            emitter.send(response);
            recordSend("success", startedAt);
            return true;
        } catch (IOException | IllegalStateException exception) {
            log.debug("Unified market SSE send failed; closing session. stream={} memberFingerprint={} reason=send_failure",
                    STREAM, memberFingerprint(sessionKey.memberId()), exception);
            recordSend("failure", startedAt);
            closeSession(sessionKey, emitter, "send_failure");
            return false;
        }
    }

    private void bindLifecycle(MarketStreamSessionKey sessionKey, SseEmitter emitter) {
        emitter.onCompletion(() -> releaseSession(sessionKey, "client_complete"));
        emitter.onTimeout(() -> {
            releaseSession(sessionKey, "timeout");
            emitter.complete();
        });
        emitter.onError(error -> {
            log.debug("Unified market SSE emitter reported an error; closing session. stream={} memberFingerprint={}",
                    STREAM, memberFingerprint(sessionKey.memberId()), error);
            releaseSession(sessionKey, "error");
        });
    }

    private void closeSession(MarketStreamSessionKey sessionKey, SseEmitter emitter, String reason) {
        releaseSession(sessionKey, reason);
        try {
            emitter.complete();
        } catch (RuntimeException ignored) {
            // The client may already be disconnected.
        }
    }

    private void releaseSession(MarketStreamSessionKey sessionKey, String reason) {
        if (registry.releaseSession(sessionKey, reason)) {
            recordConnectionClosed(reason);
            logLifecycle("release", reason, sessionKey, null, null);
        }
    }

    private void completeReplacedEmitter(SseEmitter replacedEmitter) {
        if (replacedEmitter == null) {
            return;
        }
        try {
            replacedEmitter.complete();
        } catch (RuntimeException ignored) {
            // The replaced client may already be closed.
        }
        recordConnectionClosed(CLIENT_REPLACED_REASON);
    }

    private Set<MarketCandleInterval> affectedIntervals(MarketHistoryFinalizedEvent event) {
        // Minute finalization affects minute-derived intervals. Existing raw candle SSE keeps its more detailed
        // historical availability checks; unified first pass uses this conservative low-cardinality match set.
        Set<MarketCandleInterval> intervals = new LinkedHashSet<>();
        intervals.add(MarketCandleInterval.ONE_MINUTE);
        intervals.add(MarketCandleInterval.THREE_MINUTES);
        intervals.add(MarketCandleInterval.FIVE_MINUTES);
        intervals.add(MarketCandleInterval.FIFTEEN_MINUTES);
        if (event.openTime().getEpochSecond() % Duration.ofHours(1).toSeconds() == 0) {
            intervals.add(MarketCandleInterval.ONE_HOUR);
        }
        return intervals;
    }

    private void logLifecycle(
            String action,
            String reason,
            MarketStreamSessionKey sessionKey,
            String activeSymbol,
            CandleSubscription candleSubscription
    ) {
        log.info(
                "SSE lifecycle stream={} keyType=member_client memberFingerprint={} action={} reason={} activeSymbol={} candleInterval={} activeTotalEmitters={}",
                STREAM,
                memberFingerprint(sessionKey.memberId()),
                action,
                reason,
                activeSymbol,
                candleSubscription == null ? null : candleSubscription.interval().value(),
                registry.activeSessionCount()
        );
    }

    private String memberFingerprint(Long memberId) {
        return Integer.toHexString(Long.hashCode(memberId));
    }

    private void recordConnectionOpened() {
        try {
            sseTelemetry.connectionOpened(STREAM);
        } catch (RuntimeException exception) {
            log.warn("Failed to record unified market SSE connection open telemetry", exception);
        }
    }

    private void recordConnectionClosed(String reason) {
        try {
            sseTelemetry.connectionClosed(STREAM, reason);
        } catch (RuntimeException exception) {
            log.warn("Failed to record unified market SSE connection close telemetry. reason={}", reason, exception);
        }
    }

    private void recordSend(String result, long startedAt) {
        try {
            sseTelemetry.sendRecorded(STREAM, result, Duration.ofNanos(System.nanoTime() - startedAt));
        } catch (RuntimeException exception) {
            log.warn("Failed to record unified market SSE send telemetry. result={}", result, exception);
        }
    }

    private void recordExecutorRejected() {
        try {
            sseTelemetry.executorRejected(STREAM);
        } catch (RuntimeException exception) {
            log.warn("Failed to record unified market SSE executor rejection telemetry", exception);
        }
    }
}
