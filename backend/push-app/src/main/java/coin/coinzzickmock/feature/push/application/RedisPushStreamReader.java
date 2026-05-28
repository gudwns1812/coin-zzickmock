package coin.coinzzickmock.feature.push.application;

import coin.coinzzickmock.feature.push.application.PushRelayResult;
import coin.coinzzickmock.feature.push.application.PushRelayService;
import coin.coinzzickmock.feature.push.application.PushServerProperties;
import coin.coinzzickmock.feature.push.application.dto.PushEventEnvelope;
import coin.coinzzickmock.feature.push.application.dto.PushEventType;
import coin.coinzzickmock.feature.push.application.dto.PushStream;
import coin.coinzzickmock.feature.push.application.dto.PushTargetType;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPushStreamReader {
    private final StringRedisTemplate redisTemplate;
    private final PushServerProperties properties;
    private final PushRelayService relayService;
    private final MeterRegistry meterRegistry;
    private final AtomicBoolean groupsInitialized = new AtomicBoolean(false);

    public void initializeGroups() {
        if (!properties.enabled()) {
            return;
        }
        initializeGroup(properties.marketStreamKey(), properties.marketGroup());
        initializeGroup(properties.tradingStreamKey(), properties.tradingGroup());
        groupsInitialized.set(true);
    }

    public void poll() {
        if (!properties.enabled() || !groupsInitialized.get()) {
            return;
        }
        read(properties.marketStreamKey(), properties.marketGroup(), PushStream.MARKET);
        read(properties.tradingStreamKey(), properties.tradingGroup(), PushStream.TRADING);
    }

    private void initializeGroup(String key, String group) {
        try {
            redisTemplate.opsForStream().add(key, Map.of("eventType", "__INIT__", "publishedAt", Instant.now().toString()));
            redisTemplate.opsForStream().createGroup(key, ReadOffset.latest(), group);
        } catch (RedisSystemException exception) {
            if (!String.valueOf(exception.getMessage()).contains("BUSYGROUP")) {
                throw exception;
            }
        }
    }

    private void read(String key, String group, PushStream expectedStream) {
        var records = redisTemplate.opsForStream().read(
                Consumer.from(group, properties.consumerName()),
                StreamReadOptions.empty().count(properties.batchSize()),
                StreamOffset.create(key, ReadOffset.lastConsumed())
        );
        if (records == null || records.isEmpty()) {
            return;
        }
        for (MapRecord<String, Object, Object> record : records) {
            handle(key, group, expectedStream, record);
        }
    }

    private void handle(String key, String group, PushStream expectedStream, MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();
        if ("__INIT__".equals(value.get("eventType"))) {
            ack(key, group, record);
            return;
        }
        String ackResult = "success";
        try {
            PushEventEnvelope envelope = envelope(value, expectedStream);
            PushRelayResult result = relayService.relay(envelope, record.getId().getValue());
            ackResult = result.name().toLowerCase();
        } catch (RuntimeException exception) {
            ackResult = "invalid_payload";
            log.warn("Dropping invalid push stream record. key={} id={}", key, record.getId(), exception);
        } finally {
            ack(key, group, record);
            meterRegistry.counter("push.event.ack.total", "stream", expectedStream.name().toLowerCase(), "result", ackResult).increment();
        }
    }

    private PushEventEnvelope envelope(Map<Object, Object> fields, PushStream expectedStream) {
        PushEventType eventType = PushEventType.valueOf(required(fields, "eventType"));
        PushTargetType targetType = PushTargetType.valueOf(required(fields, "targetType"));
        Instant publishedAt = Instant.parse(required(fields, "publishedAt"));
        return new PushEventEnvelope(
                Integer.parseInt(required(fields, "schemaVersion")),
                expectedStream,
                eventType,
                targetType,
                optional(fields, "symbol"),
                optional(fields, "interval"),
                optionalLong(fields, "memberId"),
                optional(fields, "dedupeKey"),
                Instant.parse(required(fields, "eventTime")),
                publishedAt,
                Duration.ofMillis(Long.parseLong(required(fields, "maxAgeMs"))),
                required(fields, "payloadJson")
        );
    }

    private void ack(String key, String group, MapRecord<String, Object, Object> record) {
        redisTemplate.opsForStream().acknowledge(key, group, record.getId());
    }

    private String required(Map<Object, Object> fields, String key) {
        return Objects.toString(fields.get(key), null);
    }

    private String optional(Map<Object, Object> fields, String key) {
        Object value = fields.get(key);
        return value == null ? null : value.toString();
    }

    private Long optionalLong(Map<Object, Object> fields, String key) {
        String value = optional(fields, key);
        return value == null || value.isBlank() ? null : Long.parseLong(value);
    }
}
