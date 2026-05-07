package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.providers.telemetry.SseTelemetry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class MicrometerSseTelemetry implements SseTelemetry {
    private static final String CONNECTIONS_CURRENT = "sse.connections.current";
    private static final String CONNECTIONS_OPENED_TOTAL = "sse.connections.opened.total";
    private static final String CONNECTIONS_CLOSED_TOTAL = "sse.connections.closed.total";
    private static final String CONNECTIONS_REJECTED_TOTAL = "sse.connections.rejected.total";
    private static final String SEND_TOTAL = "sse.send.total";
    private static final String SEND_DURATION = "sse.send.duration";
    private static final String EXECUTOR_REJECTED_TOTAL = "sse.executor.rejected.total";
    private static final Set<String> STREAMS = Set.of("market", "market_candle", "trading_execution");
    private static final Set<String> REASONS = Set.of(
            "total_limit",
            "symbol_limit",
            "member_limit",
            "client_complete",
            "timeout",
            "error",
            "send_failure",
            "executor_rejected",
            "replaced",
            "unknown"
    );
    private static final Set<String> RESULTS = Set.of("success", "failure");

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, AtomicInteger> activeConnections = new ConcurrentHashMap<>();

    public MicrometerSseTelemetry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void connectionOpened(String stream) {
        String safeStream = stream(stream);
        activeConnections(safeStream).incrementAndGet();
        meterRegistry.counter(CONNECTIONS_OPENED_TOTAL, streamTags(safeStream)).increment();
    }

    @Override
    public void connectionClosed(String stream, String reason) {
        String safeStream = stream(stream);
        activeConnections(safeStream).updateAndGet(current -> Math.max(0, current - 1));
        meterRegistry.counter(CONNECTIONS_CLOSED_TOTAL, reasonTags(safeStream, reason(reason))).increment();
    }

    @Override
    public void connectionRejected(String stream, String reason) {
        meterRegistry.counter(CONNECTIONS_REJECTED_TOTAL, reasonTags(stream(stream), reason(reason))).increment();
    }

    @Override
    public void sendRecorded(String stream, String result, Duration duration) {
        String safeStream = stream(stream);
        String safeResult = result(result);
        meterRegistry.counter(SEND_TOTAL, resultTags(safeStream, safeResult)).increment();
        meterRegistry.timer(SEND_DURATION, resultTags(safeStream, safeResult)).record(duration);
    }

    @Override
    public void executorRejected(String stream) {
        meterRegistry.counter(EXECUTOR_REJECTED_TOTAL, streamTags(stream(stream))).increment();
    }

    private AtomicInteger activeConnections(String stream) {
        return activeConnections.computeIfAbsent(stream, key -> {
            AtomicInteger value = new AtomicInteger();
            meterRegistry.gauge(MetricTags.meterName(CONNECTIONS_CURRENT), streamTags(key), value);
            return value;
        });
    }

    private Tags streamTags(String stream) {
        return MetricTags.of(Map.of("stream", stream));
    }

    private Tags reasonTags(String stream, String reason) {
        return MetricTags.of(Map.of(
                "stream", stream,
                "reason", reason
        ));
    }

    private Tags resultTags(String stream, String result) {
        return MetricTags.of(Map.of(
                "stream", stream,
                "result", result
        ));
    }

    private String stream(String stream) {
        if (STREAMS.contains(stream)) {
            return stream;
        }
        return "unknown";
    }

    private String reason(String reason) {
        if (REASONS.contains(reason)) {
            return reason;
        }
        return "unknown";
    }

    private String result(String result) {
        if (RESULTS.contains(result)) {
            return result;
        }
        return "failure";
    }
}
