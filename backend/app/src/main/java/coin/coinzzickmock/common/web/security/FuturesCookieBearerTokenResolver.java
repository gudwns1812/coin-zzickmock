package coin.coinzzickmock.common.web.security;

import coin.coinzzickmock.providers.auth.AccessTokenManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FuturesCookieBearerTokenResolver implements BearerTokenResolver {
    private static final OAuth2Error BLANK_ACCESS_TOKEN = new OAuth2Error(
            "invalid_token",
            "accessToken cookie must not be blank.",
            null
    );

    private final AccessTokenManager accessTokenManager;

    @Override
    public String resolve(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (accessTokenManager.accessTokenCookieName().equals(cookie.getName())) {
                String value = cookie.getValue();
                if (value == null || value.isBlank()) {
                    throw new OAuth2AuthenticationException(BLANK_ACCESS_TOKEN);
                }
                return value;
            }
        }
        return null;
    }
}
