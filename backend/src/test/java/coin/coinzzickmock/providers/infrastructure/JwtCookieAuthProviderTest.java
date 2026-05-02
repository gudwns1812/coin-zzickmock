package coin.coinzzickmock.providers.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.providers.auth.AccessTokenManager;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.ActorLookup;
import coin.coinzzickmock.providers.auth.ActorRole;
import coin.coinzzickmock.providers.auth.AuthSessionClaims;
import jakarta.servlet.http.Cookie;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class JwtCookieAuthProviderTest {
    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void currentActorReturnsActorForValidCookie() {
        requestWithCookie("valid");
        JwtCookieAuthProvider provider = new JwtCookieAuthProvider(
                new FakeAccessTokenManager(),
                new FakeActorLookup(Optional.of(new Actor(1L, "demo", "demo@example.com", "Demo", ActorRole.ADMIN)))
        );

        Actor actor = provider.currentActor();

        assertThat(actor.memberId()).isEqualTo(1L);
        assertThat(actor.admin()).isTrue();
    }

    @Test
    void currentActorRejectsMissingCookie() {
        requestWithoutCookie();
        JwtCookieAuthProvider provider = new JwtCookieAuthProvider(
                new FakeAccessTokenManager(),
                new FakeActorLookup(Optional.empty())
        );

        assertThrows(CoreException.class, provider::currentActor);
    }

    @Test
    void isAuthenticatedReturnsFalseForStaleCookie() {
        requestWithCookie("valid");
        JwtCookieAuthProvider provider = new JwtCookieAuthProvider(
                new FakeAccessTokenManager(),
                new FakeActorLookup(Optional.empty())
        );

        assertThat(provider.isAuthenticated()).isFalse();
    }

    @Test
    void isAuthenticatedReturnsFalseForMalformedCookie() {
        requestWithCookie("malformed");
        JwtCookieAuthProvider provider = new JwtCookieAuthProvider(
                new FakeAccessTokenManager(),
                new FakeActorLookup(Optional.of(new Actor(1L, "demo", "demo@example.com", "Demo")))
        );

        assertThat(provider.isAuthenticated()).isFalse();
    }

    private void requestWithCookie(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("accessToken", value));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private void requestWithoutCookie() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    private static class FakeAccessTokenManager implements AccessTokenManager {
        @Override
        public String issue(AuthSessionClaims claims) {
            return "valid";
        }

        @Override
        public AuthSessionClaims parse(String token) {
            if ("malformed".equals(token)) {
                throw new CoreException(ErrorCode.UNAUTHORIZED, "bad token");
            }
            return new AuthSessionClaims(1L, "demo", "Demo", "demo@example.com", ActorRole.USER);
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

    private record FakeActorLookup(Optional<Actor> actor) implements ActorLookup {
        @Override
        public Optional<Actor> findByMemberId(Long memberId) {
            return actor;
        }

        @Override
        public Optional<Actor> findByAccount(String account) {
            return actor;
        }
    }
}
