package stock.stockzzickmock.support.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class JwtCookieFactory {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final JwtProperties jwtProperties;

    public JwtCookieFactory(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public ResponseCookie createAccessTokenCookie(String token, HttpServletRequest request) {
        return buildCookie(
                ACCESS_TOKEN_COOKIE_NAME,
                token,
                jwtProperties.accessExpirationSeconds(),
                request
        );
    }

    public ResponseCookie createRefreshTokenCookie(String token, HttpServletRequest request) {
        return buildCookie(
                REFRESH_TOKEN_COOKIE_NAME,
                token,
                jwtProperties.refreshExpirationSeconds(),
                request
        );
    }

    public ResponseCookie expireAccessTokenCookie(HttpServletRequest request) {
        return buildCookie(ACCESS_TOKEN_COOKIE_NAME, "", 0, request);
    }

    public ResponseCookie expireRefreshTokenCookie(HttpServletRequest request) {
        return buildCookie(REFRESH_TOKEN_COOKIE_NAME, "", 0, request);
    }

    private ResponseCookie buildCookie(
            String name,
            String value,
            long maxAgeSeconds,
            HttpServletRequest request
    ) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(isSecureRequest(request))
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        return jwtProperties.cookieSecure() || request.isSecure();
    }
}
