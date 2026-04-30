package coin.coinzzickmock.providers.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class HttpRequestTelemetryTest {
    @Test
    void recordsRequestDurationCountAndPayloadBuckets() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        HttpRequestTelemetry telemetry = new HttpRequestTelemetry(registry);

        telemetry.record(
                "GET",
                "/api/futures/markets/{symbol}/candles",
                200,
                Duration.ofMillis(250),
                0,
                512
        );

        assertThat(registry.counter(
                "http.request.total",
                "method",
                "GET",
                "route_pattern",
                "/api/futures/markets/{symbol}/candles",
                "endpoint_group",
                "market",
                "status",
                "200",
                "status_family",
                "2xx"
        ).count()).isEqualTo(1);
        assertThat(registry.timer(
                "http.request.duration",
                "method",
                "GET",
                "route_pattern",
                "/api/futures/markets/{symbol}/candles",
                "endpoint_group",
                "market",
                "status_family",
                "2xx"
        ).count()).isEqualTo(1);
        assertThat(registry.counter(
                "http.payload.size.bucket.total",
                "method",
                "GET",
                "route_pattern",
                "/api/futures/markets/{symbol}/candles",
                "endpoint_group",
                "market",
                "status_family",
                "2xx",
                "direction",
                "response",
                "size_bucket",
                "le_1kb"
        ).count()).isEqualTo(1);
    }

    @Test
    void incrementsSlowRequestCounterAfterThreshold() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        HttpRequestTelemetry telemetry = new HttpRequestTelemetry(registry);

        telemetry.record(
                "POST",
                "/api/futures/orders",
                201,
                Duration.ofSeconds(2),
                2048,
                256
        );

        assertThat(registry.counter(
                "http.request.slow.total",
                "method",
                "POST",
                "route_pattern",
                "/api/futures/orders",
                "endpoint_group",
                "order",
                "status_family",
                "2xx"
        ).count()).isEqualTo(1);
    }
}
