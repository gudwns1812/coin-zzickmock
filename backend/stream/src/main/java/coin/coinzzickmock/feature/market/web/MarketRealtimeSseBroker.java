package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.common.web.SseClientKey;
import coin.coinzzickmock.common.web.SseDeliveryExecutor;
import coin.coinzzickmock.common.web.SseEmitterLifecycle;
import coin.coinzzickmock.common.web.SseClientKeyException;
import coin.coinzzickmock.common.web.SseSubscriptionLimitExceededException;
import coin.coinzzickmock.common.web.SseSubscriptionRegistry.ReservationRejection;
import coin.coinzzickmock.providers.telemetry.NoopSseTelemetry;
import coin.coinzzickmock.providers.telemetry.SseTelemetry;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class MarketRealtimeSseBroker {
    private static final String STREAM = "market";
    private static final String CLIENT_REPLACED_REASON = "client_replaced";

    private final MarketSummarySubscriptionRegistry subscriptions;
    private final SseDeliveryExecutor sseEventExecutor;
    private final SseTelemetry sseTelemetry;

    @Autowired
    public MarketRealtimeSseBroker(
            SseDeliveryExecutor sseEventExecutor,
            @Value("${coin.market.sse.max-subscribers-per-symbol:50}") int maxSubscribersPerSymbol,
            @Value("${coin.market.sse.max-total-subscribers:100}") int maxSubscribersTotal,
            SseTelemetry sseTelemetry
    ) {
        this.subscriptions = new MarketSummarySubscriptionRegistry(maxSubscribersPerSymbol, maxSubscribersTotal);
        this.sseEventExecutor = sseEventExecutor;
        this.sseTelemetry = sseTelemetry;
    }

    MarketRealtimeSseBroker(
            Executor sseEventExecutor,
            int maxSubscribersPerSymbol,
            int maxSubscribersTotal
    ) {
        this(new SseDeliveryExecutor(sseEventExecutor), maxSubscribersPerSymbol, maxSubscribersTotal, NoopSseTelemetry.INSTANCE);
    }

    MarketRealtimeSseBroker(
            Executor sseEventExecutor,
            int maxSubscribersPerSymbol,
            int maxSubscribersTotal,
            SseTelemetry sseTelemetry
    ) {
        this(new SseDeliveryExecutor(sseEventExecutor), maxSubscribersPerSymbol, maxSubscribersTotal, sseTelemetry);
    }

    public SseSubscriptionPermit reserve(String symbol) {
        return reserve(Set.of(symbol), SseClientKey.fallback().value());
    }

    public SseSubscriptionPermit reserve(String symbol, String clientKey) {
        return reserve(Set.of(symbol), clientKey);
    }

    public SseSubscriptionPermit reserve(Set<String> symbols, String clientKey) {
        return reserveCapacity(normalizeSymbols(symbols), clientKey);
    }

    private SseSubscriptionPermit reserveCapacity(Set<String> symbols, String clientKey) {
        MarketSummarySubscriptionRegistry.Reservation reservation = subscriptions.reserve(symbols, clientKey);
        if (!reservation.accepted()) {
            reject(reservation.rejection());
            throw new SseSubscriptionLimitExceededException(
                    reservation.rejection() == ReservationRejection.TOTAL_LIMIT ? "total_limit" : "symbol_limit"
            );
        }
        return reservation.permit();
    }


    public void register(SseSubscriptionPermit permit, SseEmitter emitter) {
        try {
            bindLifecycle(permit, emitter);
            MarketSummarySubscriptionRegistry.Registration registration = subscriptions.register(permit, emitter);
            recordConnectionOpened();
            logLifecycle(permit.primarySymbol(), "register", null);
            completeReplacedEmitter(permit.primarySymbol(), registration.replacedEmitter());
        } catch (RuntimeException exception) {
            subscriptions.release(permit);
            throw exception;
        }
    }


    public void unregister(String symbol, SseEmitter emitter) {
        unregister(symbol, emitter, "client_complete");
    }

    private void unregister(String symbol, SseEmitter emitter, String reason) {
        if (subscriptions.unregister(emitter)) {
            recordConnectionClosed(reason);
            logLifecycle(symbol, lifecycleAction(reason), reason);
        }
    }

    private void unregister(String symbol, String clientKey, SseEmitter emitter, String reason) {
        if (subscriptions.unregister(clientKey, emitter)) {
            recordConnectionClosed(reason);
            logLifecycle(symbol, lifecycleAction(reason), reason);
        }
    }


    public void release(SseSubscriptionPermit permit) {
        if (subscriptions.release(permit)) {
            logLifecycle(permit.primarySymbol(), "release", "before_register");
        }
    }

    public void onMarketUpdated(MarketSummaryResponse response) {
        String symbol = response.symbol();
        List<SseEmitter> symbolEmitters = subscribers(symbol);
        if (symbolEmitters.isEmpty()) {
            return;
        }
        try {
            sseEventExecutor.execute(() -> sendToSubscribers(symbol, symbolEmitters, response));
        } catch (RejectedExecutionException exception) {
            log.debug("Market SSE executor rejected fan-out. symbol={}", symbol, exception);
            recordExecutorRejected();
        }
    }

    private void bindLifecycle(SseSubscriptionPermit permit, SseEmitter emitter) {
        SseEmitterLifecycle.bind(
                emitter,
                () -> unregister(permit.primarySymbol(), permit.clientKey(), emitter, "client_complete"),
                () -> unregister(permit.primarySymbol(), permit.clientKey(), emitter, "timeout"),
                error -> {
                    log.debug("Market SSE emitter reported an error; closing subscription. symbol={}", permit.primarySymbol(), error);
                    unregister(permit.primarySymbol(), permit.clientKey(), emitter, "error");
                }
        );
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

    private void sendToSubscribers(String symbol, List<SseEmitter> symbolEmitters,
                                   MarketSummaryResponse response) {
        symbolEmitters.forEach(emitter -> send(symbol, emitter, response));
    }

    private List<SseEmitter> subscribers(String symbol) {
        return subscriptions.subscribers(symbol);
    }

    private static Set<String> normalizeSymbols(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            throw new SseClientKeyException("Invalid market stream symbols");
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String symbol : symbols) {
            if (symbol == null || symbol.isBlank()) {
                throw new SseClientKeyException("Invalid market stream symbol");
            }
            normalized.add(symbol.trim());
        }
        return Collections.unmodifiableSet(normalized);
    }

    private void reject(ReservationRejection rejection) {
        if (rejection == ReservationRejection.TOTAL_LIMIT) {
            recordConnectionRejected("total_limit");
            return;
        }
        recordConnectionRejected("symbol_limit");
    }

    private void completeReplacedEmitter(String symbol, SseEmitter replacedEmitter) {
        if (replacedEmitter == null) {
            return;
        }
        SseEmitterLifecycle.completeSilently(replacedEmitter);
        recordConnectionClosed(CLIENT_REPLACED_REASON);
        logLifecycle(symbol, "replace", CLIENT_REPLACED_REASON);
    }

    private void logLifecycle(String symbol, String action, String reason) {
        log.info(
                "SSE lifecycle stream={} keyType=symbol symbol={} action={} reason={} activeKeyEmitters={} activeTotalEmitters={}",
                STREAM,
                symbol,
                action,
                reason,
                subscriberCount(symbol),
                totalSubscriberCount()
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

    boolean hasSubscriberLimit(String symbol) {
        return subscriptions.hasSubscriberLimit(symbol);
    }

    int subscriberCount(String symbol) {
        return subscriptions.subscriberCount(symbol);
    }

    int totalSubscriberCount() {
        return subscriptions.totalSubscriberCount();
    }

    public static final class SseSubscriptionPermit {
        private final String clientKey;
        private final Set<String> symbols;
        private final boolean totalCapacityAcquired;
        private final Set<String> acquiredSymbols;
        private final AtomicBoolean released = new AtomicBoolean(false);
        private final AtomicBoolean registered = new AtomicBoolean(false);

        SseSubscriptionPermit(
                String clientKey,
                Set<String> symbols,
                boolean totalCapacityAcquired,
                Set<String> acquiredSymbols
        ) {
            this.clientKey = clientKey;
            this.symbols = Collections.unmodifiableSet(new LinkedHashSet<>(symbols));
            this.totalCapacityAcquired = totalCapacityAcquired;
            this.acquiredSymbols = Collections.unmodifiableSet(new LinkedHashSet<>(acquiredSymbols));
        }

        public String symbol() {
            return primarySymbol();
        }

        public String primarySymbol() {
            return symbols.iterator().next();
        }

        public Set<String> symbols() {
            return symbols;
        }

        public String clientKey() {
            return clientKey;
        }

        boolean totalCapacityAcquired() {
            return totalCapacityAcquired;
        }

        Set<String> acquiredSymbols() {
            return acquiredSymbols;
        }

        boolean markRegistered() {
            return !released.get() && registered.compareAndSet(false, true);
        }

        boolean markReleasedBeforeRegistration() {
            return !registered.get() && released.compareAndSet(false, true);
        }
    }
}
