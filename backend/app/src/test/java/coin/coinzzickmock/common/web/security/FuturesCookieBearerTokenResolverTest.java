package coin.coinzzickmock.common.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.providers.auth.AccessTokenManager;
import coin.coinzzickmock.providers.auth.AuthSessionClaims;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

class FuturesCookieBearerTokenResolverTest {
    private final FuturesCookieBearerTokenResolver resolver = new FuturesCookieBearerTokenResolver(
            new FakeAccessTokenManager()
    );

    @Test
    void returnsNullWhenAccessTokenCookieIsAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void returnsAccessTokenCookieValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("accessToken", "token-value"));

        assertThat(resolver.resolve(request)).isEqualTo("token-value");
    }

    @Test
    void rejectsBlankAccessTokenCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("accessToken", " "));

        assertThrows(OAuth2AuthenticationException.class, () -> resolver.resolve(request));
    }

    private static class FakeAccessTokenManager implements AccessTokenManager {
        @Override
        public String issue(AuthSessionClaims claims) {
            return "token-value";
        }

        @Override
        public AuthSessionClaims parse(String token) {
            throw new UnsupportedOperationException("parse should not be called by the cookie bearer token resolver.");
        }

        @Override
        public String accessTokenCookieName() {
            return "accessToken";
        }

        @Override
        public long accessTokenExpirationSeconds() {
            return 3600;
        }

        @Override
        public boolean secureCookie() {
            return true;
        }
    }
}
