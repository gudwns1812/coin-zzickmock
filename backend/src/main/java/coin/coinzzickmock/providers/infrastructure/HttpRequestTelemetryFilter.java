package coin.coinzzickmock.providers.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class HttpRequestTelemetryFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(HttpRequestTelemetryFilter.class);
    private static final String UNMATCHED_ROUTE = "unmatched";

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

        try {
            filterChain.doFilter(request, wrappedResponse);
        } finally {
            try {
                telemetry.record(
                        request.getMethod(),
                        routePattern(request),
                        wrappedResponse.getStatus(),
                        Duration.ofNanos(System.nanoTime() - startedAt),
                        request.getContentLengthLong(),
                        wrappedResponse.getContentSize()
                );
            } catch (RuntimeException e) {
                log.warn("Failed to record HTTP telemetry", e);
            } finally {
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
}
