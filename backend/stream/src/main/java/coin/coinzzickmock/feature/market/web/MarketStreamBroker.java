package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.common.web.SseDeliveryExecutor;
import coin.coinzzickmock.common.web.SseEmitterLifecycle;
import coin.coinzzickmock.providers.telemetry.NoopSseTelemetry;
import coin.coinzzickmock.providers.telemetry.SseTelemetry;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class MarketStreamBroker {
    private static final String STREAM = "market";

    private final MarketStreamRegistry registry;
    private final SseDeliveryExecutor sseEventExecutor;
    private final MarketSummarySnapshotReader marketSummarySnapshotReader;
    private final MarketCandleSnapshotReader marketCandleSnapshotReader;
    private final MarketCurrentCandleBootstrapper currentMarketCandleBootstrapper;
    private final SseTelemetry sseTelemetry;

    @Autowired
    public MarketStreamBroker(
            MarketStreamRegistry registry,
            SseDeliveryExecutor sseEventExecutor,
            MarketSummarySnapshotReader marketSummarySnapshotReader,
            MarketCandleSnapshotReader marketCandleSnapshotReader,
            MarketCurrentCandleBootstrapper currentMarketCandleBootstrapper,
            SseTelemetry sseTelemetry
    ) {
        this.registry = registry;
        this.sseEventExecutor = sseEventExecutor;
        this.marketSummarySnapshotReader = marketSummarySnapshotReader;
        this.marketCandleSnapshotReader = marketCandleSnapshotReader;
        this.currentMarketCandleBootstrapper = currentMarketCandleBootstrapper;
        this.sseTelemetry = sseTelemetry;
    }

    MarketStreamBroker(
            MarketStreamRegistry registry,
            Executor sseEventExecutor,
            MarketSummarySnapshotReader marketSummarySnapshotReader,
            MarketCandleSnapshotReader marketCandleSnapshotReader,
            MarketCurrentCandleBootstrapper currentMarketCandleBootstrapper
    ) {
        this(registry, new SseDeliveryExecutor(sseEventExecutor), marketSummarySnapshotReader,
                marketCandleSnapshotReader, currentMarketCandleBootstrapper, NoopSseTelemetry.INSTANCE);
    }

    public void openSession(
            Long memberId,
            String clientKey,
            String activeSymbol,
            Set<String> openPositionSymbols,
            String interval,
            SseEmitter emitter
    ) {
        MarketStreamSessionKey sessionKey = new MarketStreamSessionKey(memberId, clientKey);
        CandleSubscription candleSubscription = new CandleSubscription(activeSymbol, interval);
        MarketStreamRegistry.Registration registration = registry.registerSession(
                sessionKey,
                emitter,
                activeSymbol,
                new LinkedHashSet<>(openPositionSymbols),
                candleSubscription
        );
        bindLifecycle(sessionKey, emitter);
        recordConnectionOpened();
        completeReplacedEmitter(registration.replacedEmitter());
        try {
            sendInitialSnapshots(emitter, activeSymbol, openPositionSymbols, interval);
            logLifecycle(activeSymbol, interval, "register", null);
        } catch (RuntimeException exception) {
            registry.releaseSession(sessionKey, emitter, "initial_send_failure");
            SseEmitterLifecycle.completeSilently(emitter);
            recordConnectionClosed("initial_send_failure");
            throw exception;
        }
    }

    public void addOpenPositionReason(Long memberId, String symbol) {
        registry.sessionKeysForMember(memberId).forEach(sessionKey ->
                registry.addSummaryReason(sessionKey, symbol, SummarySubscriptionReason.OPEN_POSITION));
    }

    public void removeOpenPositionReason(Long memberId, String symbol) {
        registry.sessionKeysForMember(memberId).forEach(sessionKey ->
                registry.removeSummaryReason(sessionKey, symbol, SummarySubscriptionReason.OPEN_POSITION));
    }

    public void onMarketUpdated(MarketSummaryResponse result) {
        List<MarketStreamSubscriber> subscribers = registry.sessionsForSummary(result.symbol());
        if (subscribers.isEmpty()) {
            return;
        }
        MarketStreamEventResponse response = MarketStreamEventResponse.summary(
                result,
                MarketStreamEventSource.LIVE,
                Instant.now()
        );
        executeFanOut(result.symbol(), null, subscribers, response);
    }

    public void onCandleUpdated(String symbol) {
        registry.candleSubscriptionsForSymbol(symbol)
                .forEach(subscription -> fanOutCandleUpdate(symbol, subscription.interval()));
    }

    public void onHistoryFinalized(String symbol, Instant openTime, Instant closeTime) {
        registry.candleSubscriptionsForSymbol(symbol).forEach(subscription -> {
            List<MarketStreamSubscriber> subscribers = registry.sessionsForCandle(subscription);
            if (subscribers.isEmpty()) {
                return;
            }
            MarketStreamEventResponse response = MarketStreamEventResponse.historyFinalized(
                    subscription,
                    MarketCandleHistoryFinalizedResponse.of(
                            symbol,
                            openTime,
                            closeTime,
                            List.of(subscription.interval())
                    ),
                    Instant.now()
            );
            executeFanOut(symbol, subscription.interval(), subscribers, response);
        });
    }

    public void fanOutCandleUpdate(String symbol, String interval) {
        CandleSubscription subscription = new CandleSubscription(symbol, interval);
        List<MarketStreamSubscriber> subscribers = registry.sessionsForCandle(subscription);
        if (subscribers.isEmpty()) {
            return;
        }
        marketCandleSnapshotReader.latest(symbol, interval)
                .map(candle -> MarketStreamEventResponse.candle(
                        symbol,
                        subscription,
                        candle,
                        MarketStreamEventSource.LIVE,
                        Instant.now()
                ))
                .ifPresent(response -> executeFanOut(symbol, interval, subscribers, response));
    }

    private void sendInitialSnapshots(
            SseEmitter emitter,
            String activeSymbol,
            Set<String> openPositionSymbols,
            String interval
    ) {
        Set<String> summarySymbols = new LinkedHashSet<>();
        summarySymbols.add(activeSymbol);
        summarySymbols.addAll(openPositionSymbols);
        for (String symbol : summarySymbols) {
            MarketSummaryResponse result = marketSummarySnapshotReader.getMarket(symbol);
            sendOrThrow(
                    activeSymbol,
                    interval,
                    emitter,
                    MarketStreamEventResponse.summary(result, MarketStreamEventSource.INITIAL_SNAPSHOT, Instant.now())
            );
        }
        if (currentMarketCandleBootstrapper != null) {
            currentMarketCandleBootstrapper.bootstrapIfNeeded(activeSymbol, interval);
        }
        marketCandleSnapshotReader.latest(activeSymbol, interval)
                .map(candle -> MarketStreamEventResponse.candle(
                        activeSymbol,
                        new CandleSubscription(activeSymbol, interval),
                        candle,
                        MarketStreamEventSource.INITIAL_SNAPSHOT,
                        Instant.now()
                ))
                .ifPresent(response -> sendOrThrow(activeSymbol, interval, emitter, response));
    }

    private void executeFanOut(
            String symbol,
            String interval,
            List<MarketStreamSubscriber> subscribers,
            MarketStreamEventResponse response
    ) {
        try {
            sseEventExecutor.execute(() -> subscribers.forEach(subscriber -> send(subscriber, response)));
        } catch (RejectedExecutionException exception) {
            log.debug("Unified market SSE executor rejected fan-out. symbol={} interval={}",
                    symbol, interval, exception);
            recordExecutorRejected();
        }
    }

    private void bindLifecycle(MarketStreamSessionKey sessionKey, SseEmitter emitter) {
        SseEmitterLifecycle.bind(
                emitter,
                () -> release(sessionKey, emitter, "client_complete"),
                () -> release(sessionKey, emitter, "timeout"),
                error -> {
                    log.debug("Unified market SSE emitter reported an error; closing session.", error);
                    release(sessionKey, emitter, "error");
                }
        );
    }

    private void send(MarketStreamSubscriber subscriber, MarketStreamEventResponse response) {
        long startedAt = System.nanoTime();
        try {
            subscriber.emitter().send(response);
            recordSend("success", startedAt);
        } catch (IOException | IllegalStateException exception) {
            log.debug("Unified market SSE send failed; closing session. symbol={} reason=send_failure", response.symbol(), exception);
            recordSend("failure", startedAt);
            release(subscriber.sessionKey(), subscriber.emitter(), "send_failure");
        }
    }

    private void sendOrThrow(
            String activeSymbol,
            String interval,
            SseEmitter emitter,
            MarketStreamEventResponse response
    ) {
        long startedAt = System.nanoTime();
        try {
            emitter.send(response);
            recordSend("success", startedAt);
        } catch (IOException | IllegalStateException exception) {
            recordSend("failure", startedAt);
            log.debug("Initial unified market SSE send failed. symbol={} interval={}", activeSymbol, interval, exception);
            throw new IllegalStateException("Initial unified market stream send failed", exception);
        }
    }

    private void release(MarketStreamSessionKey sessionKey, SseEmitter emitter, String reason) {
        boolean removed = registry.releaseSession(sessionKey, emitter, reason);
        if (removed) {
            recordConnectionClosed(reason);
            logLifecycle(sessionKey, lifecycleAction(reason), reason);
        }
    }

    private void completeReplacedEmitter(SseEmitter replacedEmitter) {
        if (replacedEmitter == null) {
            return;
        }
        SseEmitterLifecycle.completeSilently(replacedEmitter);
        recordConnectionClosed("client_replaced");
    }

    private void logLifecycle(String symbol, String interval, String action, String reason) {
        log.info(
                "SSE lifecycle stream={} keyType=session symbol={} interval={} action={} reason={} activeKeyEmitters={} activeTotalEmitters={}",
                STREAM,
                symbol,
                interval,
                action,
                reason,
                registry.summarySubscriberCount(symbol),
                registry.activeSessionCount()
        );
    }

    private void logLifecycle(MarketStreamSessionKey sessionKey, String action, String reason) {
        log.info(
                "SSE lifecycle stream={} keyType=session memberId={} clientKey={} action={} reason={} activeKeyEmitters={} activeTotalEmitters={}",
                STREAM,
                sessionKey.memberId(),
                sessionKey.clientKey(),
                action,
                reason,
                registry.activeSessionCount(),
                registry.activeSessionCount()
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
            log.warn("Failed to record unified market SSE send telemetry", exception);
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
