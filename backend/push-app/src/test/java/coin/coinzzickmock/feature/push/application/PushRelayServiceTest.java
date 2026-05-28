package coin.coinzzickmock.feature.push.application;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.push.application.dto.PushEventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class PushRelayServiceTest {
    private final java.util.concurrent.ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final PushSseConnectionRegistry registry = new PushSseConnectionRegistry(new PushServerProperties(
            true,
            "test-consumer",
            "coin:push:market:v1",
            "coin:push:trading:v1",
            "push-server-market-v1",
            "push-server-trading-v1",
            100,
            10,
            Duration.ofSeconds(15),
            Duration.ofSeconds(5),
            300_000,
            10,
            100
    ));
    private final PushRelayService relayService = new PushRelayService(
            registry,
            new ObjectMapper(),
            executor,
            new SimpleMeterRegistry()
    );

    @AfterEach
    void tearDown() {
        executor.close();
    }

    @Test
    void drops_stale_event_without_subscriber_lookup_replay() {
        Instant old = Instant.now().minusSeconds(30);
        PushEventEnvelope envelope = PushEventEnvelope.tradingExecution(
                1L,
                "BTCUSDT",
                "dedupe-1",
                old,
                old,
                Duration.ofSeconds(5),
                "{\"symbol\":\"BTCUSDT\"}"
        );

        PushRelayResult result = relayService.relay(envelope, "1-0");

        assertThat(result).isEqualTo(PushRelayResult.STALE_DROP);
    }

    @Test
    void drops_fresh_event_with_no_active_subscriber() {
        PushEventEnvelope envelope = freshSummary("dedupe-2");

        PushRelayResult result = relayService.relay(envelope, "1-0");

        assertThat(result).isEqualTo(PushRelayResult.NO_SUBSCRIBER);
    }

    @Test
    void sends_fresh_event_to_active_subscriber() throws Exception {
        RecordingEmitter emitter = new RecordingEmitter();
        addConnection("summary:BTCUSDT", emitter);

        PushRelayResult result = relayService.relay(freshSummary("dedupe-3"), "1-0");

        assertThat(result).isEqualTo(PushRelayResult.SENT);
        assertThat(emitter.sendCount()).isEqualTo(1);
    }

    @Test
    void suppresses_duplicate_event_for_same_connection() throws Exception {
        RecordingEmitter emitter = new RecordingEmitter();
        addConnection("summary:BTCUSDT", emitter);
        PushEventEnvelope envelope = freshSummary("same-dedupe");

        PushRelayResult first = relayService.relay(envelope, "1-0");
        PushRelayResult duplicate = relayService.relay(envelope, "2-0");

        assertThat(first).isEqualTo(PushRelayResult.SENT);
        assertThat(duplicate).isEqualTo(PushRelayResult.NO_SUBSCRIBER);
        assertThat(emitter.sendCount()).isEqualTo(1);
    }


    @Test
    void raw_market_summary_does_not_deliver_to_unified_subscriber() throws Exception {
        RecordingEmitter emitter = new RecordingEmitter();
        addConnection("unified:BTCUSDT", emitter);

        PushRelayResult result = relayService.relay(freshSummary("raw-summary-only"), "1-0");

        assertThat(result).isEqualTo(PushRelayResult.NO_SUBSCRIBER);
        assertThat(emitter.sendCount()).isZero();
    }

    @Test
    void unified_market_summary_delivers_to_unified_subscriber() throws Exception {
        RecordingEmitter emitter = new RecordingEmitter();
        addConnection("unified:BTCUSDT", emitter);
        Instant now = Instant.now();
        PushEventEnvelope envelope = PushEventEnvelope.marketUnifiedSummary(
                "BTCUSDT",
                "unified-summary",
                now,
                now,
                Duration.ofSeconds(15),
                "{\"kind\":\"MARKET_SUMMARY\",\"symbol\":\"BTCUSDT\",\"data\":{}}"
        );

        PushRelayResult result = relayService.relay(envelope, "1-0");

        assertThat(result).isEqualTo(PushRelayResult.SENT);
        assertThat(emitter.sendCount()).isEqualTo(1);
    }

    @Test
    void unregisters_connection_when_send_fails() throws Exception {
        addConnection("summary:BTCUSDT", new FailingEmitter());

        PushRelayResult result = relayService.relay(freshSummary("dedupe-4"), "1-0");

        assertThat(result).isEqualTo(PushRelayResult.CONNECTION_CLOSED);
        assertThat(registry.connectionsFor(java.util.Set.of("summary:BTCUSDT"))).isEmpty();
    }

    private PushEventEnvelope freshSummary(String dedupeKey) {
        Instant now = Instant.now();
        return PushEventEnvelope.marketSummary(
                "BTCUSDT",
                dedupeKey,
                now,
                now,
                Duration.ofSeconds(15),
                "{\"symbol\":\"BTCUSDT\"}"
        );
    }

    @SuppressWarnings("unchecked")
    private void addConnection(String key, SseEmitter emitter) throws Exception {
        Field byKeyField = PushSseConnectionRegistry.class.getDeclaredField("byKey");
        byKeyField.setAccessible(true);
        var byKey = (ConcurrentHashMap<String, CopyOnWriteArrayList<PushSseConnection>>) byKeyField.get(registry);
        byKey.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>())
                .add(new PushSseConnection(key, "client", emitter));

        Field totalField = PushSseConnectionRegistry.class.getDeclaredField("total");
        totalField.setAccessible(true);
        ((AtomicInteger) totalField.get(registry)).incrementAndGet();
    }

    private static final class RecordingEmitter extends SseEmitter {
        private int sendCount;

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            sendCount++;
        }

        int sendCount() {
            return sendCount;
        }
    }

    private static final class FailingEmitter extends SseEmitter {
        @Override
        public void send(SseEventBuilder builder) throws IOException {
            throw new IOException("closed");
        }
    }
}
