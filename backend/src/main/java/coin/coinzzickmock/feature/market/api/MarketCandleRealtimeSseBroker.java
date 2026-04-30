package coin.coinzzickmock.feature.market.api;

import coin.coinzzickmock.feature.market.application.realtime.MarketCandleUpdatedEvent;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleProjector;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class MarketCandleRealtimeSseBroker {
    private final ConcurrentMap<SubscriptionKey, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Executor sseEventExecutor;
    private final RealtimeMarketCandleProjector realtimeMarketCandleProjector;

    public MarketCandleRealtimeSseBroker(
            @Qualifier("marketRealtimeSseEventExecutor") Executor sseEventExecutor,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector
    ) {
        this.sseEventExecutor = sseEventExecutor;
        this.realtimeMarketCandleProjector = realtimeMarketCandleProjector;
    }

    public void register(String symbol, MarketCandleInterval interval, SseEmitter emitter) {
        SubscriptionKey key = new SubscriptionKey(symbol, interval);
        bindLifecycle(key, emitter);
        emitters.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    public void unregister(SubscriptionKey key, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> keyEmitters = emitters.get(key);
        if (keyEmitters == null) {
            return;
        }
        keyEmitters.remove(emitter);
        if (keyEmitters.isEmpty()) {
            emitters.remove(key, keyEmitters);
        }
    }

    @EventListener
    public void onCandleUpdated(MarketCandleUpdatedEvent event) {
        emitters.keySet().stream()
                .filter(key -> key.symbol().equals(event.symbol()))
                .forEach(this::fanOutLatest);
    }

    private void fanOutLatest(SubscriptionKey key) {
        CopyOnWriteArrayList<SseEmitter> keyEmitters = emitters.get(key);
        if (keyEmitters == null || keyEmitters.isEmpty()) {
            return;
        }
        realtimeMarketCandleProjector.latest(key.symbol(), key.interval())
                .map(MarketCandleResponse::from)
                .ifPresent(response -> sseEventExecutor.execute(() -> keyEmitters.forEach(
                        emitter -> send(key, emitter, response)
                )));
    }

    private void bindLifecycle(SubscriptionKey key, SseEmitter emitter) {
        emitter.onCompletion(() -> unregister(key, emitter));
        emitter.onTimeout(() -> {
            unregister(key, emitter);
            emitter.complete();
        });
        emitter.onError(error -> {
            log.debug("Market candle SSE emitter reported an error; closing subscription. key={}", key, error);
            unregister(key, emitter);
        });
    }

    private void send(SubscriptionKey key, SseEmitter emitter, MarketCandleResponse response) {
        try {
            emitter.send(response);
        } catch (IOException exception) {
            log.debug("Market candle SSE send failed; closing subscription. key={}", key, exception);
            unregister(key, emitter);
        }
    }

    public record SubscriptionKey(String symbol, MarketCandleInterval interval) {
    }
}
