package coin.coinzzickmock.feature.push.application;

import coin.coinzzickmock.feature.push.application.dto.PushEventEnvelope;
import coin.coinzzickmock.feature.push.application.dto.PushEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushRelayService {
    private final PushSseConnectionRegistry registry;
    private final ObjectMapper objectMapper;
    private final ExecutorService pushVirtualThreadExecutor;
    private final MeterRegistry meterRegistry;

    public PushRelayResult relay(PushEventEnvelope envelope, String streamId) {
        Instant now = Instant.now();
        if (envelope.staleAt(now)) {
            recordDrop(envelope, "stale");
            return PushRelayResult.STALE_DROP;
        }

        List<PushSseConnection> subscribers = registry.connectionsFor(keys(envelope));
        if (subscribers.isEmpty()) {
            recordDrop(envelope, "no_subscriber");
            return PushRelayResult.NO_SUBSCRIBER;
        }

        Object payload = payload(envelope);
        boolean anySent = false;
        boolean anyFailure = false;
        for (PushSseConnection subscriber : subscribers) {
            if (!subscriber.firstDelivery(envelope.dedupeKey() == null ? streamId : envelope.dedupeKey())) {
                continue;
            }
            PushRelayResult result = send(envelope, streamId, subscriber, payload);
            anySent = anySent || result == PushRelayResult.SENT;
            anyFailure = anyFailure || result == PushRelayResult.CONNECTION_CLOSED || result == PushRelayResult.FAILURE;
        }

        if (anySent) {
            return PushRelayResult.SENT;
        }
        return anyFailure ? PushRelayResult.CONNECTION_CLOSED : PushRelayResult.NO_SUBSCRIBER;
    }

    private PushRelayResult send(PushEventEnvelope envelope, String streamId, PushSseConnection subscriber, Object payload) {
        try {
            pushVirtualThreadExecutor.submit(() -> {
                try {
                    doSend(streamId, subscriber, payload);
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            }).get();
            recordSend(envelope, "success");
            return PushRelayResult.SENT;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            recordSend(envelope, "failure");
            return PushRelayResult.FAILURE;
        } catch (Exception exception) {
            registry.unregister(subscriber);
            recordSend(envelope, "connection_closed");
            log.debug("Push SSE delivery failed. key={} stream={}", subscriber.key(), envelope.stream(), exception);
            return PushRelayResult.CONNECTION_CLOSED;
        }
    }

    private void doSend(String streamId, PushSseConnection subscriber, Object payload) throws IOException {
        SseEmitter.SseEventBuilder event = SseEmitter.event().id(streamId).data(payload);
        subscriber.emitter().send(event);
    }

    private Object payload(PushEventEnvelope envelope) {
        try {
            return objectMapper.readValue(envelope.payloadJson(), Object.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid push payload JSON.", exception);
        }
    }

    private Set<String> keys(PushEventEnvelope envelope) {
        Set<String> keys = new LinkedHashSet<>();
        switch (envelope.eventType()) {
            case TRADING_EXECUTION -> keys.add("member:" + envelope.memberId());
            case MARKET_SUMMARY -> keys.add("summary:" + envelope.symbol());
            case MARKET_UNIFIED_SUMMARY -> keys.add("unified:" + envelope.symbol());
            case MARKET_CANDLE, MARKET_HISTORY_FINALIZED ->
                    keys.add("candle:" + envelope.symbol() + ":" + envelope.interval());
            case MARKET_UNIFIED_CANDLE, MARKET_UNIFIED_HISTORY_FINALIZED ->
                    keys.add("unified:" + envelope.symbol() + ":" + envelope.interval());
        }
        return keys;
    }

    private void recordDrop(PushEventEnvelope envelope, String reason) {
        meterRegistry.counter("push.event.drop.total", "stream", envelope.stream().name().toLowerCase(), "reason", reason).increment();
        recordAge(envelope, "drop_" + reason);
    }

    private void recordSend(PushEventEnvelope envelope, String result) {
        meterRegistry.counter("push.delivery.send.total", "stream", envelope.stream().name().toLowerCase(), "result", result).increment();
        recordAge(envelope, result);
    }

    private void recordAge(PushEventEnvelope envelope, String result) {
        double ageSeconds = Duration.between(envelope.publishedAt(), Instant.now()).toMillis() / 1000.0;
        meterRegistry.summary("push.event.age.seconds", "stream", envelope.stream().name().toLowerCase(), "result", result)
                .record(Math.max(ageSeconds, 0));
    }
}
