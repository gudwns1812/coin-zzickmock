package coin.coinzzickmock.providers.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class BitgetTelemetryTest {
    @Test
    void recordsRequestCountAndDuration() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BitgetTelemetry telemetry = new BitgetTelemetry(registry);

        telemetry.recordRequest("ticker", "success", Duration.ofMillis(12));

        assertThat(registry.counter(
                "market.bitget.request.total",
                "operation",
                "ticker",
                "result",
                "success"
        ).count()).isEqualTo(1);
        assertThat(registry.timer(
                "market.bitget.request.duration",
                "operation",
                "ticker"
        ).count()).isEqualTo(1);
    }

    @Test
    void recordsFallbackWithBoundedSymbol() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BitgetTelemetry telemetry = new BitgetTelemetry(registry);

        telemetry.recordFallback("history_candles", "DOGEUSDT_2026_PRIVATE", "empty");

        assertThat(registry.counter(
                "market.bitget.fallback.total",
                "operation",
                "history_candles",
                "symbol",
                "UNKNOWN",
                "reason",
                "empty"
        ).count()).isEqualTo(1);
    }
}
