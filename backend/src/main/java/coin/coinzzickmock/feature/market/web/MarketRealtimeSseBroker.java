package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.web.SseClientKey;
import coin.coinzzickmock.common.web.SseSubscriptionRegistry.ReservationRejection;
import coin.coinzzickmock.feature.market.application.realtime.MarketSummaryUpdatedEvent;
import coin.coinzzickmock.providers.telemetry.NoopSseTelemetry;
import coin.coinzzickmock.providers.telemetry.SseTelemetry;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private static final String CLIENT_REPLACED_REASON = "client_replaced";

    private final Map<String, ClientSubscription> subscriptionsByClientKey = new LinkedHashMap<>();
    private final Map<String, Set<String>> clientKeysBySymbol = new LinkedHashMap<>();
    private final Map<String, Semaphore> subscriberLimitsBySymbol = new LinkedHashMap<>();
    private final Map<String, Integer> pendingSubscriberCountsBySymbol = new LinkedHashMap<>();
    private final int maxSubscribersPerSymbol;
    private final Semaphore totalSubscriberLimit;
    private final Executor sseEventExecutor;
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
        return reserve(Set.of(symbol), SseClientKey.fallback().value());
    }

    public SseSubscriptionPermit reserve(String symbol, String clientKey) {
        return reserve(Set.of(symbol), clientKey);
    }

    public SseSubscriptionPermit reserve(Set<String> symbols, String clientKey) {
        return reserveCapacity(normalizeSymbols(symbols), clientKey);
    }

    private synchronized SseSubscriptionPermit reserveCapacity(Set<String> symbols, String clientKey) {
        String resolvedClientKey = SseClientKey.resolve(clientKey).value();
        ClientSubscription previous = subscriptionsByClientKey.get(resolvedClientKey);
        boolean totalCapacityAcquired = previous == null;
        Set<String> acquiredSymbols = new LinkedHashSet<>();

        if (totalCapacityAcquired && !totalSubscriberLimit.tryAcquire()) {
            reject(ReservationRejection.TOTAL_LIMIT);
            throw new CoreException(ErrorCode.TOO_MANY_REQUESTS);
        }

        for (String symbol : symbols) {
            if (previous != null && previous.symbols().contains(symbol)) {
                continue;
            }
            if (!acquireSymbolCapacity(symbol)) {
                acquiredSymbols.forEach(this::decrementPendingSubscriberCount);
                releaseSymbolCapacities(acquiredSymbols);
                if (totalCapacityAcquired) {
                    totalSubscriberLimit.release();
                }
                reject(ReservationRejection.KEY_LIMIT);
                throw new CoreException(ErrorCode.TOO_MANY_REQUESTS);
            }
            acquiredSymbols.add(symbol);
            incrementPendingSubscriberCount(symbol);
        }

        return new SseSubscriptionPermit(resolvedClientKey, symbols, totalCapacityAcquired, acquiredSymbols);
    }

    public void register(SseSubscriptionPermit permit, SseEmitter emitter) {
        bindLifecycle(permit, emitter);
        SseEmitter replacedEmitter = registerCurrentEmitter(permit, emitter);
        recordConnectionOpened();
        logLifecycle(permit.primarySymbol(), "register", null);
        completeReplacedEmitter(permit.primarySymbol(), replacedEmitter);
    }

    private synchronized SseEmitter registerCurrentEmitter(SseSubscriptionPermit permit, SseEmitter emitter) {
        if (!permit.markRegistered()) {
            throw new IllegalStateException("SSE subscription permit is not active");
        }
        permit.acquiredSymbols().forEach(this::decrementPendingSubscriberCount);

        ClientSubscription previous = subscriptionsByClientKey.put(
                permit.clientKey(),
                new ClientSubscription(permit.clientKey(), permit.symbols(), emitter)
        );
        if (previous != null) {
            removeClientIndexes(previous.clientKey(), previous.symbols());
            releaseRemovedSymbolCapacities(previous.symbols(), permit.symbols());
        }
        addClientIndexes(permit.clientKey(), permit.symbols());
        return previous == null ? null : previous.emitter();
    }

    public void unregister(String symbol, SseEmitter emitter) {
        unregister(symbol, emitter, "client_complete");
    }

    private void unregister(String symbol, SseEmitter emitter, String reason) {
        ClientSubscription removed = unregisterByEmitter(emitter);
        if (removed != null) {
            recordConnectionClosed(reason);
            logLifecycle(symbol, lifecycleAction(reason), reason);
        }
    }

    private void unregister(String symbol, String clientKey, SseEmitter emitter, String reason) {
        ClientSubscription removed = unregisterByClientKey(clientKey, emitter);
        if (removed != null) {
            recordConnectionClosed(reason);
            logLifecycle(symbol, lifecycleAction(reason), reason);
        }
    }

    private synchronized ClientSubscription unregisterByEmitter(SseEmitter emitter) {
        for (ClientSubscription subscription : List.copyOf(subscriptionsByClientKey.values())) {
            if (subscription.emitter() == emitter) {
                return removeClientSubscription(subscription.clientKey(), emitter);
            }
        }
        return null;
    }

    private synchronized ClientSubscription unregisterByClientKey(String clientKey, SseEmitter emitter) {
        return removeClientSubscription(clientKey, emitter);
    }

    private ClientSubscription removeClientSubscription(String clientKey, SseEmitter emitter) {
        ClientSubscription current = subscriptionsByClientKey.get(clientKey);
        if (current == null || current.emitter() != emitter) {
            return null;
        }
        subscriptionsByClientKey.remove(clientKey);
        removeClientIndexes(clientKey, current.symbols());
        releaseSymbolCapacities(current.symbols());
        totalSubscriberLimit.release();
        return current;
    }

    public synchronized void release(SseSubscriptionPermit permit) {
        if (!permit.markReleasedBeforeRegistration()) {
            return;
        }
        permit.acquiredSymbols().forEach(this::decrementPendingSubscriberCount);
        releaseSymbolCapacities(permit.acquiredSymbols());
        if (permit.totalCapacityAcquired()) {
            totalSubscriberLimit.release();
        }
        logLifecycle(permit.primarySymbol(), "release", "before_register");
    }

    @EventListener
    public void onMarketUpdated(MarketSummaryUpdatedEvent event) {
        String symbol = event.result().symbol();
        List<SseEmitter> symbolEmitters = subscribers(symbol);
        if (symbolEmitters.isEmpty()) {
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
        emitter.onCompletion(() -> unregister(permit.primarySymbol(), permit.clientKey(), emitter, "client_complete"));
        emitter.onTimeout(() -> {
            unregister(permit.primarySymbol(), permit.clientKey(), emitter, "timeout");
            emitter.complete();
        });
        emitter.onError(error -> {
            log.debug("Market SSE emitter reported an error; closing subscription. symbol={}", permit.primarySymbol(), error);
            unregister(permit.primarySymbol(), permit.clientKey(), emitter, "error");
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

    private void sendToSubscribers(String symbol, List<SseEmitter> symbolEmitters,
                                   MarketSummaryResponse response) {
        symbolEmitters.forEach(emitter -> send(symbol, emitter, response));
    }

    private synchronized List<SseEmitter> subscribers(String symbol) {
        Set<String> clientKeys = clientKeysBySymbol.get(symbol);
        if (clientKeys == null || clientKeys.isEmpty()) {
            return List.of();
        }
        return clientKeys.stream()
                .map(subscriptionsByClientKey::get)
                .filter(Objects::nonNull)
                .map(ClientSubscription::emitter)
                .toList();
    }

    private static Set<String> normalizeSymbols(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String symbol : symbols) {
            if (symbol == null || symbol.isBlank()) {
                throw new CoreException(ErrorCode.INVALID_REQUEST);
            }
            normalized.add(symbol.trim());
        }
        return Collections.unmodifiableSet(normalized);
    }

    private boolean acquireSymbolCapacity(String symbol) {
        Semaphore symbolLimit = subscriberLimitsBySymbol.computeIfAbsent(
                symbol,
                ignored -> new Semaphore(maxSubscribersPerSymbol, true)
        );
        return symbolLimit.tryAcquire();
    }

    private void releaseRemovedSymbolCapacities(Set<String> previousSymbols, Set<String> nextSymbols) {
        previousSymbols.stream()
                .filter(symbol -> !nextSymbols.contains(symbol))
                .forEach(this::releaseSymbolCapacity);
    }

    private void releaseSymbolCapacities(Set<String> symbols) {
        symbols.forEach(this::releaseSymbolCapacity);
    }

    private void releaseSymbolCapacity(String symbol) {
        Semaphore symbolLimit = subscriberLimitsBySymbol.get(symbol);
        if (symbolLimit != null) {
            symbolLimit.release();
            cleanupSymbolLimit(symbol, symbolLimit);
        }
    }

    private void addClientIndexes(String clientKey, Set<String> symbols) {
        symbols.forEach(symbol -> clientKeysBySymbol
                .computeIfAbsent(symbol, ignored -> new LinkedHashSet<>())
                .add(clientKey));
    }

    private void removeClientIndexes(String clientKey, Set<String> symbols) {
        for (String symbol : symbols) {
            Set<String> clientKeys = clientKeysBySymbol.get(symbol);
            if (clientKeys == null) {
                continue;
            }
            clientKeys.remove(clientKey);
            if (clientKeys.isEmpty()) {
                clientKeysBySymbol.remove(symbol);
            }
        }
    }

    private void incrementPendingSubscriberCount(String symbol) {
        pendingSubscriberCountsBySymbol.merge(symbol, 1, Integer::sum);
    }

    private void decrementPendingSubscriberCount(String symbol) {
        Integer current = pendingSubscriberCountsBySymbol.get(symbol);
        if (current == null || current <= 1) {
            pendingSubscriberCountsBySymbol.remove(symbol);
            return;
        }
        pendingSubscriberCountsBySymbol.put(symbol, current - 1);
    }

    private void cleanupSymbolLimit(String symbol, Semaphore symbolLimit) {
        if (symbolLimit.availablePermits() == maxSubscribersPerSymbol
                && !clientKeysBySymbol.containsKey(symbol)
                && !pendingSubscriberCountsBySymbol.containsKey(symbol)) {
            subscriberLimitsBySymbol.remove(symbol, symbolLimit);
        }
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
        try {
            replacedEmitter.complete();
        } catch (RuntimeException ignored) {
            // The replaced client may already be closed.
        }
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

    synchronized boolean hasSubscriberLimit(String symbol) {
        return subscriberLimitsBySymbol.containsKey(symbol);
    }

    synchronized int subscriberCount(String symbol) {
        Set<String> clientKeys = clientKeysBySymbol.get(symbol);
        return clientKeys == null ? 0 : clientKeys.size();
    }

    synchronized int totalSubscriberCount() {
        return subscriptionsByClientKey.size();
    }

    private record ClientSubscription(String clientKey, Set<String> symbols, SseEmitter emitter) {
    }

    public static final class SseSubscriptionPermit {
        private final String clientKey;
        private final Set<String> symbols;
        private final boolean totalCapacityAcquired;
        private final Set<String> acquiredSymbols;
        private final AtomicBoolean released = new AtomicBoolean(false);
        private final AtomicBoolean registered = new AtomicBoolean(false);

        private SseSubscriptionPermit(
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

        private boolean totalCapacityAcquired() {
            return totalCapacityAcquired;
        }

        private Set<String> acquiredSymbols() {
            return acquiredSymbols;
        }

        private boolean markRegistered() {
            return !released.get() && registered.compareAndSet(false, true);
        }

        private boolean markReleasedBeforeRegistration() {
            return !registered.get() && released.compareAndSet(false, true);
        }
    }
}
