package coin.coinzzickmock.feature.member.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coin.coinzzickmock.feature.member.domain.MemberRole;
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
        String token = manager.issue(1L, "demo-member", "Demo User", "demo@example.com", MemberRole.ADMIN);

        JwtSessionClaims claims = manager.parse(token);
        assertEquals(1L, claims.memberId());
        assertEquals("demo-member", claims.account());
        assertEquals("Demo User", claims.nickname());
        assertEquals("demo@example.com", claims.email());
        assertEquals(MemberRole.ADMIN, claims.role());
    }

    @Test
    void buildsSecureCookieByDefault() {
        MockEnvironment environment = new MockEnvironment();
        JwtAccessTokenManager manager = new JwtAccessTokenManager(
                "12345678901234567890123456789012",
                3600,
                true,
                environment
        );

        String cookie = manager.buildAccessTokenCookie("token").toString();

        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("HttpOnly"));
    }
}
