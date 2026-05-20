package coin.coinzzickmock.providers.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class HttpRequestTelemetryFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(HttpRequestTelemetryFilter.class);
    private static final String UNMATCHED_ROUTE = "unmatched";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final int MAX_OBSERVABILITY_ID_LENGTH = 64;

    private final HttpRequestTelemetry telemetry;

    public HttpRequestTelemetryFilter(HttpRequestTelemetry telemetry) {
        this.telemetry = telemetry;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || path.contains("/stream");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        String requestId = observabilityId(request.getHeader(REQUEST_ID_HEADER));
        String correlationId = observabilityId(request.getHeader(CORRELATION_ID_HEADER), requestId);

        wrappedResponse.setHeader(REQUEST_ID_HEADER, requestId);
        wrappedResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
        MDC.put("requestId", requestId);
        MDC.put("correlationId", correlationId);

        try {
            filterChain.doFilter(request, wrappedResponse);
        } finally {
            Duration duration = Duration.ofNanos(System.nanoTime() - startedAt);
            String routePattern = routePattern(request);
            int status = wrappedResponse.getStatus();
            try {
                telemetry.record(
                        request.getMethod(),
                        routePattern,
                        status,
                        duration,
                        request.getContentLengthLong(),
                        wrappedResponse.getContentSize()
                );
                logRequest(
                        request,
                        routePattern,
                        status,
                        duration,
                        requestId,
                        correlationId,
                        request.getContentLengthLong(),
                        wrappedResponse.getContentSize()
                );
            } catch (RuntimeException e) {
                log.warn("Failed to record HTTP telemetry", e);
            } finally {
                MDC.remove("requestId");
                MDC.remove("correlationId");
                wrappedResponse.copyBodyToResponse();
            }
        }
    }

    private String routePattern(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern == null) {
            return UNMATCHED_ROUTE;
        }
        return pattern.toString();
    }

    private void logRequest(
            HttpServletRequest request,
            String routePattern,
            int status,
            Duration duration,
            String requestId,
            String correlationId,
            long requestBytes,
            long responseBytes
    ) {
        String endpointGroup = telemetry.endpointGroup(routePattern);
        long durationMs = duration.toMillis();
        String statusFamily = status / 100 + "xx";
        String result = result(status);
        String requestSizeBucket = telemetry.sizeBucket(requestBytes);
        String responseSizeBucket = telemetry.sizeBucket(responseBytes);

        log.info(
                "event=http.request.completed service=backend method={} pathPattern={} endpointGroup={} status={} statusFamily={} result={} durationMs={} requestSizeBucket={} responseSizeBucket={} requestId={} correlationId={}",
                request.getMethod(),
                routePattern,
                endpointGroup,
                status,
                statusFamily,
                result,
                durationMs,
                requestSizeBucket,
                responseSizeBucket,
                requestId,
                correlationId
        );
        if (!duration.minus(HttpRequestTelemetry.SLOW_REQUEST_THRESHOLD).isNegative()) {
            log.warn(
                    "event=http.request.slow service=backend method={} pathPattern={} endpointGroup={} status={} statusFamily={} result={} durationMs={} requestId={} correlationId={}",
                    request.getMethod(),
                    routePattern,
                    endpointGroup,
                    status,
                    statusFamily,
                    result,
                    durationMs,
                    requestId,
                    correlationId
            );
        }
        if (status >= 500) {
            log.error(
                    "event=http.request.server_error service=backend method={} pathPattern={} endpointGroup={} status={} statusFamily={} durationMs={} requestId={} correlationId={}",
                    request.getMethod(),
                    routePattern,
                    endpointGroup,
                    status,
                    statusFamily,
                    durationMs,
                    requestId,
                    correlationId
            );
            return;
        }
        if (status >= 400) {
            log.warn(
                    "event=http.request.client_error service=backend method={} pathPattern={} endpointGroup={} status={} statusFamily={} durationMs={} requestId={} correlationId={}",
                    request.getMethod(),
                    routePattern,
                    endpointGroup,
                    status,
                    statusFamily,
                    durationMs,
                    requestId,
                    correlationId
            );
        }
    }

    private String result(int status) {
        if (status >= 500) {
            return "server_error";
        }
        if (status >= 400) {
            return "client_error";
        }
        return "success";
    }

    private String observabilityId(String candidate) {
        return observabilityId(candidate, UUID.randomUUID().toString());
    }

    private String observabilityId(String candidate, String fallback) {
        if (candidate == null || candidate.isBlank()) {
            return fallback;
        }
        String trimmed = candidate.trim();
        if (trimmed.length() > MAX_OBSERVABILITY_ID_LENGTH) {
            return fallback;
        }
        for (int index = 0; index < trimmed.length(); index++) {
            char value = trimmed.charAt(index);
            if (!isObservabilityIdCharacter(value)) {
                return fallback;
            }
        }
        return trimmed;
    }

    private boolean isObservabilityIdCharacter(char value) {
        return (value >= 'a' && value <= 'z')
                || (value >= 'A' && value <= 'Z')
                || (value >= '0' && value <= '9')
                || value == '-'
                || value == '_'
                || value == '.';
    }
}
