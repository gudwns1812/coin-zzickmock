package coin.coinzzickmock.common.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CookieOAuth2AuthorizationRequestRepositoryTest {
    @Test
    void loadAuthorizationRequestRejectsCookieWhenSignatureIsTampered() {
        CookieOAuth2AuthorizationRequestRepository repository = repositoryAt("2026-05-30T00:00:00Z");
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("google-client-id")
                .redirectUri("http://localhost:8080/login/oauth2/code/google")
                .state("state-123")
                .build();

        MockHttpServletResponse response = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(authorizationRequest, new MockHttpServletRequest(), response);
        String cookieValue = extractCookieValue(response.getHeader("Set-Cookie"));

        MockHttpServletRequest validRequest = new MockHttpServletRequest();
        validRequest.setCookies(new Cookie("oauth2AuthorizationRequest", cookieValue));
        assertThat(repository.loadAuthorizationRequest(validRequest))
                .extracting(OAuth2AuthorizationRequest::getState)
                .isEqualTo("state-123");

        MockHttpServletRequest tamperedRequest = new MockHttpServletRequest();
        tamperedRequest.setCookies(new Cookie("oauth2AuthorizationRequest", tamperLastCharacter(cookieValue)));
        assertThat(repository.loadAuthorizationRequest(tamperedRequest)).isNull();
    }

    @Test
    void loadAuthorizationRequestRejectsCookieAfterSignedPayloadExpires() {
        CookieOAuth2AuthorizationRequestRepository issuingRepository = repositoryAt("2026-05-30T00:00:00Z");
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("google-client-id")
                .redirectUri("http://localhost:8080/login/oauth2/code/google")
                .state("state-123")
                .build();

        MockHttpServletResponse response = new MockHttpServletResponse();
        issuingRepository.saveAuthorizationRequest(authorizationRequest, new MockHttpServletRequest(), response);
        String cookieValue = extractCookieValue(response.getHeader("Set-Cookie"));

        MockHttpServletRequest expiredRequest = new MockHttpServletRequest();
        expiredRequest.setCookies(new Cookie("oauth2AuthorizationRequest", cookieValue));

        CookieOAuth2AuthorizationRequestRepository laterRepository = repositoryAt("2026-05-30T00:05:00Z");
        assertThat(laterRepository.loadAuthorizationRequest(expiredRequest)).isNull();
    }

    @Test
    void removeAuthorizationRequestConsumesStateAndRejectsReplayedCookie() {
        CookieOAuth2AuthorizationRequestRepository repository = repositoryAt("2026-05-30T00:00:00Z");
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("google-client-id")
                .redirectUri("http://localhost:8080/login/oauth2/code/google")
                .state("state-123")
                .build();

        MockHttpServletResponse response = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(authorizationRequest, new MockHttpServletRequest(), response);
        String cookieValue = extractCookieValue(response.getHeader("Set-Cookie"));

        MockHttpServletRequest callbackRequest = new MockHttpServletRequest();
        callbackRequest.setCookies(new Cookie("oauth2AuthorizationRequest", cookieValue));

        assertThat(repository.removeAuthorizationRequest(callbackRequest, new MockHttpServletResponse()))
                .extracting(OAuth2AuthorizationRequest::getState)
                .isEqualTo("state-123");

        MockHttpServletRequest replayedRequest = new MockHttpServletRequest();
        replayedRequest.setCookies(new Cookie("oauth2AuthorizationRequest", cookieValue));

        assertThat(repository.loadAuthorizationRequest(replayedRequest)).isNull();
        assertThat(repository.removeAuthorizationRequest(replayedRequest, new MockHttpServletResponse())).isNull();
    }

    @Test
    void removeAuthorizationRequestEvictsExpiredConsumedStatesBeforeAddingAnotherState() {
        MutableClock mutableClock = new MutableClock(Instant.parse("2026-05-30T00:00:00Z"));
        CookieOAuth2AuthorizationRequestRepository repository = repositoryAt(mutableClock);

        String firstCookieValue = savedCookieValue(repository, "state-1");
        MockHttpServletRequest firstCallback = new MockHttpServletRequest();
        firstCallback.setCookies(new Cookie("oauth2AuthorizationRequest", firstCookieValue));
        repository.removeAuthorizationRequest(firstCallback, new MockHttpServletResponse());
        assertThat(consumedStateCount(repository)).isEqualTo(1);

        mutableClock.instant = Instant.parse("2026-05-30T00:05:01Z");

        String secondCookieValue = savedCookieValue(repository, "state-2");
        MockHttpServletRequest secondCallback = new MockHttpServletRequest();
        secondCallback.setCookies(new Cookie("oauth2AuthorizationRequest", secondCookieValue));
        repository.removeAuthorizationRequest(secondCallback, new MockHttpServletResponse());

        assertThat(consumedStateCount(repository)).isEqualTo(1);
    }

    private static CookieOAuth2AuthorizationRequestRepository repositoryAt(String instant) {
        return repositoryAt(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    private static CookieOAuth2AuthorizationRequestRepository repositoryAt(Clock clock) {
        return new CookieOAuth2AuthorizationRequestRepository(
                false,
                "Lax",
                Duration.ofMinutes(5),
                "test-cookie-signing-secret",
                new ObjectMapper(),
                clock
        );
    }

    private static String savedCookieValue(
            CookieOAuth2AuthorizationRequestRepository repository,
            String state
    ) {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("google-client-id")
                .redirectUri("http://localhost:8080/login/oauth2/code/google")
                .state(state)
                .build();
        MockHttpServletResponse response = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(authorizationRequest, new MockHttpServletRequest(), response);
        return extractCookieValue(response.getHeader("Set-Cookie"));
    }

    @SuppressWarnings("unchecked")
    private static int consumedStateCount(CookieOAuth2AuthorizationRequestRepository repository) {
        try {
            java.lang.reflect.Field field = CookieOAuth2AuthorizationRequestRepository.class
                    .getDeclaredField("consumedStateExpirations");
            field.setAccessible(true);
            return ((Map<String, Long>) field.get(repository)).size();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static String extractCookieValue(String setCookieHeader) {
        int valueStart = "oauth2AuthorizationRequest=".length();
        int valueEnd = setCookieHeader.indexOf(';');
        return setCookieHeader.substring(valueStart, valueEnd);
    }

    private static String tamperLastCharacter(String value) {
        String lastCharacter = value.substring(value.length() - 1);
        String replacement = lastCharacter.equals("A") ? "B" : "A";
        return value.substring(0, value.length() - 1) + replacement;
    }
    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
