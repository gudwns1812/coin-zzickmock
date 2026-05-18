package coin.coinzzickmock.common.web;

import coin.coinzzickmock.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FuturesUnsafeMethodOriginFilter extends OncePerRequestFilter {
    private static final String FUTURES_API_PATH = "/api/futures";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "https://coin-zzickmock-frontend.vercel.app",
            "http://localhost:3000"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isFuturesApiPath(request.getRequestURI()) || SAFE_METHODS.contains(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        String requestOrigin = origin == null || origin.isBlank()
                ? refererOrigin(request.getHeader(HttpHeaders.REFERER))
                : origin;

        if (requestOrigin == null || !ALLOWED_ORIGINS.contains(requestOrigin)) {
            reject(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isFuturesApiPath(String path) {
        return FUTURES_API_PATH.equals(path) || path.startsWith(FUTURES_API_PATH + "/");
    }

    private String refererOrigin(String referer) {
        if (referer == null || referer.isBlank()) {
            return null;
        }

        try {
            URI uri = new URI(referer);
            if (uri.getScheme() == null || uri.getRawAuthority() == null) {
                return null;
            }
            return uri.getScheme() + "://" + uri.getRawAuthority();
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(ErrorCode.FORBIDDEN.httpStatusCode());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":\""
                + ErrorCode.FORBIDDEN.name()
                + "\",\"message\":\""
                + ErrorCode.FORBIDDEN.message()
                + "\"}");
    }
}
