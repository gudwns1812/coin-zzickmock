package coin.coinzzickmock.feature.push.infrastructure;

import coin.coinzzickmock.feature.push.application.dto.PushEventEnvelope;
import coin.coinzzickmock.feature.push.application.publisher.PushEventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPushEventPublisher implements PushEventPublisher {
    private final StringRedisTemplate redisTemplate;
    private final PushPublicationProperties properties;
    private final MeterRegistry meterRegistry;

    @Override
    public void publish(PushEventEnvelope event) {
        String streamKey = properties.streamKey(event.stream());
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "success";
        try {
            redisTemplate.opsForStream().add(MapRecord.create(streamKey, fields(event)));
            redisTemplate.opsForStream().trim(streamKey, properties.maxLen(), true);
        } catch (RuntimeException exception) {
            result = "failure";
            log.warn("Failed to publish push event. stream={} eventType={} targetType={} symbol={} interval={}",
                    event.stream(), event.eventType(), event.targetType(), event.symbol(), event.interval(), exception);
            throw exception;
        } finally {
            meterRegistry.counter("push.event.publish.total", "stream", event.stream().name().toLowerCase(), "result", result).increment();
            sample.stop(Timer.builder("push.event.publish.duration")
                    .tag("stream", event.stream().name().toLowerCase())
                    .tag("result", result)
                    .register(meterRegistry));
        }
    }

    private Map<String, String> fields(PushEventEnvelope event) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("schemaVersion", Integer.toString(event.schemaVersion()));
        fields.put("stream", event.stream().name());
        fields.put("eventType", event.eventType().name());
        fields.put("targetType", event.targetType().name());
        putIfPresent(fields, "symbol", event.symbol());
        putIfPresent(fields, "interval", event.interval());
        putIfPresent(fields, "memberId", event.memberId() == null ? null : event.memberId().toString());
        putIfPresent(fields, "dedupeKey", event.dedupeKey());
        fields.put("eventTime", event.eventTime().toString());
        fields.put("publishedAt", event.publishedAt().toString());
        fields.put("maxAgeMs", Long.toString(event.maxAge().toMillis()));
        fields.put("payloadJson", event.payloadJson());
        return fields;
    }

    private void putIfPresent(Map<String, String> fields, String key, String value) {
        if (value != null && !value.isBlank()) {
            fields.put(key, value);
        }
    }
}
