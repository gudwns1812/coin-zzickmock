package coin.coinzzickmock.providers.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MicrometerTelemetryProviderTest {
    @Test
    void recordsUseCaseAndFailureCounters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerTelemetryProvider provider = new MicrometerTelemetryProvider(registry);

        provider.recordUseCase("market_history_lookup");
        provider.recordFailure("market_history_lookup", "timeout");

        assertThat(registry.counter("app.usecase.total", "use_case", "market_history_lookup").count())
                .isEqualTo(1);
        assertThat(registry.counter(
                "app.usecase.failure.total",
                "use_case",
                "market_history_lookup",
                "reason",
                "timeout"
        ).count()).isEqualTo(1);
    }

    @Test
    void recordsEventCounterWithGuardedTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerTelemetryProvider provider = new MicrometerTelemetryProvider(registry);

        provider.recordEvent("market.history.db.hit", Map.of(
                "symbol", "BTCUSDT",
                "interval", "1m",
                "source", "db",
                "result", "hit"
        ));

        assertThat(registry.counter(
                "market.history.db.hit",
                "symbol",
                "BTCUSDT",
                "interval",
                "1m",
                "source",
                "db",
                "result",
                "hit"
        ).count()).isEqualTo(1);
    }

    @Test
    void acceptsExistingRedisSourceTelemetry() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerTelemetryProvider provider = new MicrometerTelemetryProvider(registry);

        provider.recordEvent("market.history.redis.hit", Map.of(
                "symbol", "BTCUSDT",
                "interval", "1m",
                "range_bucket", "2026-04",
                "source", "redis",
                "result", "hit"
        ));

        assertThat(registry.counter(
                "market.history.redis.hit",
                "symbol",
                "BTCUSDT",
                "interval",
                "1m",
                "range_bucket",
                "2026-04",
                "source",
                "redis",
                "result",
                "hit"
        ).count()).isEqualTo(1);
    }
}
