package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.feature.market.application.query.GetMarketQuery;
import coin.coinzzickmock.feature.market.application.realtime.CurrentMarketCandleBootstrapper;
import coin.coinzzickmock.feature.market.application.realtime.MarketCandleUpdatedEvent;
import coin.coinzzickmock.feature.market.application.realtime.MarketHistoryFinalizedEvent;
import coin.coinzzickmock.feature.market.application.realtime.MarketSummaryUpdatedEvent;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleProjector;
import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.application.service.GetMarketSummaryService;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.providers.telemetry.NoopSseTelemetry;
import coin.coinzzickmock.providers.telemetry.SseTelemetry;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class MarketStreamBroker {
    private static final String STREAM = "market_stream";

    private final MarketStreamRegistry registry;
    private final Executor sseEventExecutor;
    private final GetMarketSummaryService getMarketSummaryService;
    private final RealtimeMarketCandleProjector realtimeMarketCandleProjector;
    private final CurrentMarketCandleBootstrapper currentMarketCandleBootstrapper;
    private final SseTelemetry sseTelemetry;

    @Autowired
    public MarketStreamBroker(
            MarketStreamRegistry registry,
            @Qualifier("marketRealtimeSseEventExecutor") Executor sseEventExecutor,
            GetMarketSummaryService getMarketSummaryService,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector,
            CurrentMarketCandleBootstrapper currentMarketCandleBootstrapper,
            SseTelemetry sseTelemetry
    ) {
        this.registry = registry;
        this.sseEventExecutor = sseEventExecutor;
        this.getMarketSummaryService = getMarketSummaryService;
        this.realtimeMarketCandleProjector = realtimeMarketCandleProjector;
        this.currentMarketCandleBootstrapper = currentMarketCandleBootstrapper;
        this.sseTelemetry = sseTelemetry;
    }

    MarketStreamBroker(
            MarketStreamRegistry registry,
            Executor sseEventExecutor,
            GetMarketSummaryService getMarketSummaryService,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector
    ) {
        this(registry, sseEventExecutor, getMarketSummaryService, realtimeMarketCandleProjector, null, NoopSseTelemetry.INSTANCE);
    }

    public void openSession(
            Long memberId,
            String clientKey,
            String activeSymbol,
            Set<String> openPositionSymbols,
            MarketCandleInterval interval,
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
            registry.releaseSession(sessionKey, "initial_send_failure");
            completeEmitter(emitter);
            recordConnectionClosed("initial_send_failure");
            throw exception;
        }
    }

    public void addOpenPositionReason(Long memberId, String symbol) {
        registry.sessionsForMember(memberId).forEach(session ->
                registry.addSummaryReason(session.key(), symbol, SummarySubscriptionReason.OPEN_POSITION));
    }

    public void removeOpenPositionReason(Long memberId, String symbol) {
        registry.sessionsForMember(memberId).forEach(session ->
                registry.removeSummaryReason(session.key(), symbol, SummarySubscriptionReason.OPEN_POSITION));
    }

    @EventListener
    public void onMarketUpdated(MarketSummaryUpdatedEvent event) {
        List<MarketStreamSession> sessions = registry.sessionsForSummary(event.result().symbol());
        if (sessions.isEmpty()) {
            return;
        }
        MarketStreamEventResponse response = MarketStreamEventResponse.summary(
                event.result().symbol(),
                MarketStreamEventSource.LIVE,
                MarketSummaryResponse.from(event.result())
        );
        executeFanOut(event.result().symbol(), null, sessions, response);
    }

    @EventListener
    public void onCandleUpdated(MarketCandleUpdatedEvent event) {
        registry.sessionsForMemberSnapshot().forEach(session -> {});
    }

    @EventListener
    public void onHistoryFinalized(MarketHistoryFinalizedEvent event) {
        List<MarketStreamSession> sessions = registry.sessionsForSummary(event.symbol());
        if (sessions.isEmpty()) {
            return;
        }
        sessions.stream()
                .filter(session -> session.candleSubscription().symbol().equals(event.symbol()))
                .forEach(session -> {
                    CandleSubscription subscription = session.candleSubscription();
                    MarketStreamEventResponse response = MarketStreamEventResponse.historyFinalized(
                            event.symbol(),
                            subscription.interval().value(),
                            MarketStreamEventSource.LIVE,
                            MarketCandleHistoryFinalizedResponse.of(
                                    event.symbol(),
                                    event.openTime(),
                                    event.closeTime(),
                                    List.of(subscription.interval().value())
                            )
                    );
                    executeFanOut(event.symbol(), subscription.interval(), List.of(session), response);
                });
    }

    public void fanOutCandleUpdate(String symbol, MarketCandleInterval interval) {
        CandleSubscription subscription = new CandleSubscription(symbol, interval);
        List<MarketStreamSession> sessions = registry.sessionsForCandle(subscription);
        if (sessions.isEmpty()) {
            return;
        }
        realtimeMarketCandleProjector.latest(symbol, interval)
                .map(candle -> MarketStreamEventResponse.candle(
                        symbol,
                        interval.value(),
                        MarketStreamEventSource.LIVE,
                        MarketCandleResponse.from(candle)
                ))
                .ifPresent(response -> executeFanOut(symbol, interval, sessions, response));
    }

    private void sendInitialSnapshots(
            SseEmitter emitter,
            String activeSymbol,
            Set<String> openPositionSymbols,
            MarketCandleInterval interval
    ) {
        Set<String> summarySymbols = new LinkedHashSet<>();
        summarySymbols.add(activeSymbol);
        summarySymbols.addAll(openPositionSymbols);
        for (String symbol : summarySymbols) {
            MarketSummaryResult result = getMarketSummaryService.getMarket(new GetMarketQuery(symbol));
            sendOrThrow(
                    activeSymbol,
                    interval,
                    emitter,
                    MarketStreamEventResponse.summary(symbol, MarketStreamEventSource.INITIAL_SNAPSHOT, MarketSummaryResponse.from(result))
            );
        }
        if (currentMarketCandleBootstrapper != null) {
            currentMarketCandleBootstrapper.bootstrapIfNeeded(activeSymbol, interval);
        }
        realtimeMarketCandleProjector.latest(activeSymbol, interval)
                .map(MarketCandleResponse::from)
                .map(candle -> MarketStreamEventResponse.candle(
                        activeSymbol,
                        interval.value(),
                        MarketStreamEventSource.INITIAL_SNAPSHOT,
                        candle
                ))
                .ifPresent(response -> sendOrThrow(activeSymbol, interval, emitter, response));
    }

    private void executeFanOut(
            String symbol,
            MarketCandleInterval interval,
            List<MarketStreamSession> sessions,
            MarketStreamEventResponse response
    ) {
        try {
            sseEventExecutor.execute(() -> sessions.forEach(session -> send(session, response)));
        } catch (RejectedExecutionException exception) {
            log.debug("Unified market SSE executor rejected fan-out. symbol={} interval={}",
                    symbol, interval == null ? null : interval.value(), exception);
            recordExecutorRejected();
        }
    }

    private void bindLifecycle(MarketStreamSessionKey sessionKey, SseEmitter emitter) {
        emitter.onCompletion(() -> release(sessionKey, "client_complete"));
        emitter.onTimeout(() -> {
            release(sessionKey, "timeout");
            completeEmitter(emitter);
        });
        emitter.onError(error -> {
            log.debug("Unified market SSE emitter reported an error; closing session.", error);
            release(sessionKey, "error");
        });
    }

    private void send(MarketStreamSession session, MarketStreamEventResponse response) {
        long startedAt = System.nanoTime();
        try {
            session.emitter().send(response);
            recordSend("success", startedAt);
        } catch (IOException | IllegalStateException exception) {
            log.debug("Unified market SSE send failed; closing session. symbol={} reason=send_failure", response.symbol(), exception);
            recordSend("failure", startedAt);
            release(session.key(), "send_failure");
        }
    }

    private void sendOrThrow(
            String activeSymbol,
            MarketCandleInterval interval,
            SseEmitter emitter,
            MarketStreamEventResponse response
    ) {
        long startedAt = System.nanoTime();
        try {
            emitter.send(response);
            recordSend("success", startedAt);
        } catch (IOException | IllegalStateException exception) {
            recordSend("failure", startedAt);
            log.debug("Initial unified market SSE send failed. symbol={} interval={}", activeSymbol, interval.value(), exception);
            throw new IllegalStateException("Initial unified market stream send failed", exception);
        }
    }

    private void release(MarketStreamSessionKey sessionKey, String reason) {
        SseEmitter removed = registry.releaseSession(sessionKey, reason);
        if (removed != null) {
            recordConnectionClosed(reason);
            logLifecycle("*", null, lifecycleAction(reason), reason);
        }
    }

    private void completeReplacedEmitter(SseEmitter replacedEmitter) {
        if (replacedEmitter == null) {
            return;
        }
        completeEmitter(replacedEmitter);
        recordConnectionClosed("client_replaced");
    }

    private void completeEmitter(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (RuntimeException ignored) {
            // The client may already be gone.
        }
    }

    private void logLifecycle(String symbol, MarketCandleInterval interval, String action, String reason) {
        log.info(
                "SSE lifecycle stream={} keyType=member_client symbol={} interval={} action={} reason={} activeSessions={}",
                STREAM,
                symbol,
                interval == null ? null : interval.value(),
                action,
                reason,
                registry.sessionCount()
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
