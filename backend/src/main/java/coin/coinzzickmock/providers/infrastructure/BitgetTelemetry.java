package coin.coinzzickmock.providers.infrastructure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class BitgetTelemetry {
    private static final String REQUEST_TOTAL = "market.bitget.request.total";
    private static final String REQUEST_DURATION = "market.bitget.request.duration";
    private static final String FALLBACK_TOTAL = "market.bitget.fallback.total";
    private static final Set<String> OPERATIONS = Set.of("ticker", "minute_candles", "history_candles");
    private static final Set<String> RESULTS = Set.of("success", "empty", "failure", "invalid_response");
    private static final Set<String> REASONS = Set.of("empty", "exception", "invalid_response");
    private static final Set<String> SUPPORTED_SYMBOLS = Set.of("BTCUSDT", "ETHUSDT");

    private final MeterRegistry meterRegistry;

    public BitgetTelemetry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRequest(String operation, String result, Duration duration) {
        if (meterRegistry == null) {
            return;
        }
        String safeOperation = operation(operation);
        meterRegistry.counter(REQUEST_TOTAL, requestTags(safeOperation, result(result))).increment();
        meterRegistry.timer(REQUEST_DURATION, operationTags(safeOperation)).record(duration);
    }

    public void recordFallback(String operation, String symbol, String reason) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter(FALLBACK_TOTAL, fallbackTags(
                operation(operation),
                symbol(symbol),
                reason(reason)
        )).increment();
    }

    static BitgetTelemetry noop() {
        return new BitgetTelemetry(null);
    }

    private Tags requestTags(String operation, String result) {
        return MetricTags.of(Map.of(
                "operation", operation,
                "result", result
        ));
    }

    private Tags operationTags(String operation) {
        return MetricTags.of(Map.of("operation", operation));
    }

    private Tags fallbackTags(String operation, String symbol, String reason) {
        return MetricTags.of(Map.of(
                "operation", operation,
                "symbol", symbol,
                "reason", reason
        ));
    }

    private String operation(String operation) {
        if (OPERATIONS.contains(operation)) {
            return operation;
        }
        return "unknown";
    }

    private String result(String result) {
        if (RESULTS.contains(result)) {
            return result;
        }
        return "failure";
    }

    private String reason(String reason) {
        if (REASONS.contains(reason)) {
            return reason;
        }
        return "exception";
    }

    private String symbol(String symbol) {
        if (SUPPORTED_SYMBOLS.contains(symbol)) {
            return symbol;
        }
        return "UNKNOWN";
    }

}
