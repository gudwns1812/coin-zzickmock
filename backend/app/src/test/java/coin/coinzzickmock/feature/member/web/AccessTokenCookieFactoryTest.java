package coin.coinzzickmock.feature.member.web;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.member.application.result.MemberProfileResult;
import coin.coinzzickmock.feature.member.domain.MemberRole;
import coin.coinzzickmock.providers.auth.AccessTokenManager;
import coin.coinzzickmock.providers.auth.ActorRole;
import coin.coinzzickmock.providers.auth.AuthSessionClaims;
import org.junit.jupiter.api.Test;

class AccessTokenCookieFactoryTest {
    @Test
    void issuesHttpOnlyAccessTokenCookieFromMemberProfile() {
        RecordingAccessTokenManager accessTokenManager = new RecordingAccessTokenManager();
        AccessTokenCookieFactory factory = new AccessTokenCookieFactory(accessTokenManager, "None");

        String cookie = factory.issue(profile(MemberRole.ADMIN)).toString();

        assertThat(accessTokenManager.lastClaims.role()).isEqualTo(ActorRole.ADMIN);
        assertThat(cookie).contains("accessToken=signed-token");
        assertThat(cookie).contains("Path=/");
        assertThat(cookie).contains("Max-Age=3600");
        assertThat(cookie).contains("Secure");
        assertThat(cookie).contains("HttpOnly");
        assertThat(cookie).contains("SameSite=None");
    }

    @Test
    void expiresAccessTokenCookie() {
        AccessTokenCookieFactory factory = new AccessTokenCookieFactory(new RecordingAccessTokenManager(), "None");

        String cookie = factory.expire().toString();

        assertThat(cookie).contains("accessToken=");
        assertThat(cookie).contains("Max-Age=0");
        assertThat(cookie).contains("Path=/");
        assertThat(cookie).contains("HttpOnly");
    }

    private MemberProfileResult profile(MemberRole role) {
        return new MemberProfileResult(
                1L,
                "demo",
                "Demo",
                "Demo",
                "demo@example.com",
                "010-0000-0000",
                "00000",
                "Seoul",
                "101",
                0,
                role
        );
    }

    private static class RecordingAccessTokenManager implements AccessTokenManager {
        private AuthSessionClaims lastClaims;

        @Override
        public String issue(AuthSessionClaims claims) {
            this.lastClaims = claims;
            return "signed-token";
        }

        @Override
        public AuthSessionClaims parse(String token) {
            return null;
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
