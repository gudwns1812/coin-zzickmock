package coin.coinzzickmock.providers.infrastructure.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.providers.auth.ActorRole;
import coin.coinzzickmock.providers.auth.AuthSessionClaims;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class JwtAccessTokenManagerTest {
    @Test
    void rejectsBlankJwtSecretOutsideTestProfile() {
        MockEnvironment environment = new MockEnvironment();

        assertThrows(IllegalStateException.class, () -> new JwtAccessTokenManager("", 3600, true, environment));
    }

    @Test
    void allowsBlankJwtSecretInTestProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");

        JwtAccessTokenManager manager = new JwtAccessTokenManager("", 3600, true, environment);
        String token = manager.issue(new AuthSessionClaims(
                1L,
                "demo-member",
                "Demo User",
                "demo@example.com",
                ActorRole.ADMIN
        ));

        AuthSessionClaims claims = manager.parse(token);
        assertEquals(1L, claims.memberId());
        assertEquals("demo-member", claims.account());
        assertEquals("Demo User", claims.nickname());
        assertEquals("demo@example.com", claims.email());
        assertEquals(ActorRole.ADMIN, claims.role());
    }

    @Test
    void exposesCookiePolicyValues() {
        MockEnvironment environment = new MockEnvironment();
        JwtAccessTokenManager manager = new JwtAccessTokenManager(
                "12345678901234567890123456789012",
                3600,
                true,
                environment
        );

        assertEquals("accessToken", manager.accessTokenCookieName());
        assertEquals(3600, manager.accessTokenExpirationSeconds());
        assertTrue(manager.secureCookie());
    }
}
