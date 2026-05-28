package coin.coinzzickmock.common.web.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
@RequiredArgsConstructor
public class PushAccessTokenVerifier {
    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";

    private final JwtDecoder jwtDecoder;

    public Long requireMemberId(HttpServletRequest request) {
        String token = accessToken(request);
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing access token.");
        }
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return Long.parseLong(jwt.getSubject());
        } catch (JwtException | NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token.", exception);
        }
    }

    private String accessToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
