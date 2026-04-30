package coin.coinzzickmock.providers.infrastructure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HttpRequestTelemetry {
    private static final String REQUEST_TOTAL = "http.request.total";
    private static final String REQUEST_DURATION = "http.request.duration";
    private static final String SLOW_REQUEST_TOTAL = "http.request.slow.total";
    private static final String PAYLOAD_SIZE_BUCKET_TOTAL = "http.payload.size.bucket.total";
    private static final Duration SLOW_REQUEST_THRESHOLD = Duration.ofSeconds(1);

    private final MeterRegistry meterRegistry;

    public HttpRequestTelemetry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(
            String method,
            String routePattern,
            int status,
            Duration duration,
            long requestBytes,
            long responseBytes
    ) {
        String endpointGroup = endpointGroup(routePattern);
        String statusValue = String.valueOf(status);
        String statusFamily = status / 100 + "xx";
        Tags requestTags = tags(method, routePattern, endpointGroup, statusValue, statusFamily);

        meterRegistry.counter(REQUEST_TOTAL, requestTags).increment();
        meterRegistry.timer(REQUEST_DURATION, tags(method, routePattern, endpointGroup, statusFamily))
                .record(duration);
        if (!duration.minus(SLOW_REQUEST_THRESHOLD).isNegative()) {
            meterRegistry.counter(SLOW_REQUEST_TOTAL, tags(method, routePattern, endpointGroup, statusFamily))
                    .increment();
        }

        meterRegistry.counter(PAYLOAD_SIZE_BUCKET_TOTAL, tags(
                method,
                routePattern,
                endpointGroup,
                statusFamily,
                "request",
                sizeBucket(requestBytes)
        )).increment();
        meterRegistry.counter(PAYLOAD_SIZE_BUCKET_TOTAL, tags(
                method,
                routePattern,
                endpointGroup,
                statusFamily,
                "response",
                sizeBucket(responseBytes)
        )).increment();
    }

    private Tags tags(
            String method,
            String routePattern,
            String endpointGroup,
            String status,
            String statusFamily
    ) {
        return MetricTags.of(Map.of(
                "method", method,
                "route_pattern", routePattern,
                "endpoint_group", endpointGroup,
                "status", status,
                "status_family", statusFamily
        ));
    }

    private Tags tags(
            String method,
            String routePattern,
            String endpointGroup,
            String statusFamily
    ) {
        return MetricTags.of(Map.of(
                "method", method,
                "route_pattern", routePattern,
                "endpoint_group", endpointGroup,
                "status_family", statusFamily
        ));
    }

    private Tags tags(
            String method,
            String routePattern,
            String endpointGroup,
            String statusFamily,
            String direction,
            String sizeBucket
    ) {
        return MetricTags.of(Map.of(
                "method", method,
                "route_pattern", routePattern,
                "endpoint_group", endpointGroup,
                "status_family", statusFamily,
                "direction", direction,
                "size_bucket", sizeBucket
        ));
    }

    private String endpointGroup(String routePattern) {
        if (routePattern.startsWith("/api/futures/account")) {
            return "account";
        }
        if (routePattern.startsWith("/api/futures/auth")) {
            return "auth";
        }
        if (routePattern.startsWith("/api/futures/leaderboard")) {
            return "leaderboard";
        }
        if (routePattern.startsWith("/api/futures/markets")) {
            return "market";
        }
        if (routePattern.startsWith("/api/futures/orders")) {
            return "order";
        }
        if (routePattern.startsWith("/api/futures/positions")) {
            return "position";
        }
        if (routePattern.startsWith("/api/futures/rewards")
                || routePattern.startsWith("/api/futures/shop")
                || routePattern.startsWith("/api/futures/admin")) {
            return "reward";
        }
        return "unknown";
    }

    private String sizeBucket(long bytes) {
        if (bytes < 0) {
            return "unknown";
        }
        if (bytes == 0) {
            return "zero";
        }
        if (bytes <= 1024) {
            return "le_1kb";
        }
        if (bytes <= 10 * 1024) {
            return "le_10kb";
        }
        if (bytes <= 100 * 1024) {
            return "le_100kb";
        }
        return "gt_100kb";
    }
}
