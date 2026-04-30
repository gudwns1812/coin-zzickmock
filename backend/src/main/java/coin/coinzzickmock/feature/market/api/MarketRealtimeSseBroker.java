package coin.coinzzickmock.feature.market.api;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.realtime.MarketSummaryUpdatedEvent;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class MarketRealtimeSseBroker {
    private final ConcurrentMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Semaphore> symbolSubscriberLimits = new ConcurrentHashMap<>();
    private final Executor sseEventExecutor;
    private final int maxSubscribersPerSymbol;
    private final Semaphore totalSubscriberLimit;

    public MarketRealtimeSseBroker(
            @Qualifier("marketRealtimeSseEventExecutor") Executor sseEventExecutor,
            @Value("${coin.market.sse.max-subscribers-per-symbol:50}") int maxSubscribersPerSymbol,
            @Value("${coin.market.sse.max-total-subscribers:100}") int maxSubscribersTotal
    ) {
        this.sseEventExecutor = sseEventExecutor;
        this.maxSubscribersPerSymbol = maxSubscribersPerSymbol;
        this.totalSubscriberLimit = new Semaphore(maxSubscribersTotal, true);
    }

    public SseSubscriptionPermit reserve(String symbol) {
        if (!totalSubscriberLimit.tryAcquire()) {
            throw new CoreException(ErrorCode.TOO_MANY_REQUESTS);
        }

        Semaphore symbolLimit = symbolSubscriberLimits.computeIfAbsent(
                symbol,
                key -> new Semaphore(maxSubscribersPerSymbol, true)
        );
        if (!symbolLimit.tryAcquire()) {
            totalSubscriberLimit.release();
            throw new CoreException(ErrorCode.TOO_MANY_REQUESTS, "실시간 스트림 연결이 너무 많습니다: " + symbol);
        }

        return new SseSubscriptionPermit(symbol, symbolLimit);
    }

    public void register(SseSubscriptionPermit permit, SseEmitter emitter) {
        bindLifecycle(permit, emitter);
        emitters.computeIfAbsent(permit.symbol(), key -> new CopyOnWriteArrayList<>()).add(emitter);
        permit.markRegistered();
    }

    public void unregister(String symbol, SseEmitter emitter) {
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
        sseEventExecutor.execute(() -> sendToSubscribers(symbol, symbolEmitters, response));
    }

    private void bindLifecycle(SseSubscriptionPermit permit, SseEmitter emitter) {
        emitter.onCompletion(() -> unregister(permit.symbol(), emitter));
        emitter.onTimeout(() -> {
            unregister(permit.symbol(), emitter);
            emitter.complete();
        });
        emitter.onError(error -> {
            log.debug("Market SSE emitter reported an error; closing subscription. symbol={}", permit.symbol(), error);
            unregister(permit.symbol(), emitter);
        });
    }

    private void send(String symbol, SseEmitter emitter, MarketSummaryResponse response) {
        try {
            emitter.send(response);
        } catch (IOException exception) {
            log.debug("Market SSE send failed; closing subscription. symbol={}", symbol, exception);
            unregister(symbol, emitter);
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
