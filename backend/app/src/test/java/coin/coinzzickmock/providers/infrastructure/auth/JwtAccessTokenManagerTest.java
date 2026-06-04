package coin.coinzzickmock.providers.infrastructure.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coin.coinzzickmock.providers.auth.ActorRole;
import coin.coinzzickmock.providers.auth.AuthSessionClaims;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class JwtAccessTokenManagerTest {
    private static final String TEST_SECRET = "12345678901234567890123456789012";

    @Test
    void rejectsBlankJwtSecretOutsideTestProfile() {
        MockEnvironment environment = new MockEnvironment();

        assertThrows(IllegalStateException.class, () -> jwtBeans("", environment));
    }

    @Test
    void allowsBlankJwtSecretInTestProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");

        JwtAccessTokenManager manager = manager("", 3600, true, environment);
        String token = manager.issue(new AuthSessionClaims(
                1L,
                "demo-member",
                "Demo User",
                "demo@example.com",
                ActorRole.ADMIN
        ));

        Jwt jwt = jwtBeans("", environment).decoder().decode(token);
        assertEquals("1", jwt.getSubject());
        assertEquals("ACCESS", jwt.getClaimAsString("tokenType"));
    }

    @Test
    void issuesAccessTokenWithMemberSubjectAndTokenTypeClaim() {
        JwtBeans beans = jwtBeans(TEST_SECRET, new MockEnvironment());
        JwtAccessTokenManager manager = new JwtAccessTokenManager(
                beans.encoder(),
                beans.jwsHeader(),
                3600,
                true
        );

        String token = manager.issue(new AuthSessionClaims(
                1L,
                "demo-member",
                "Demo User",
                "demo@example.com",
                ActorRole.ADMIN
        ));

        Jwt jwt = beans.decoder().decode(token);
        assertEquals("1", jwt.getSubject());
        assertEquals("ACCESS", jwt.getClaimAsString("tokenType"));
        assertEquals(1L, jwt.<Number>getClaim("memberId").longValue());
    }

    @Test
    void exposesCookiePolicyValues() {
        MockEnvironment environment = new MockEnvironment();
        JwtAccessTokenManager manager = manager(
                TEST_SECRET,
                3600,
                true,
                environment
        );

        assertEquals("accessToken", manager.accessTokenCookieName());
        assertEquals(3600, manager.accessTokenExpirationSeconds());
        assertTrue(manager.secureCookie());
    }

    private JwtAccessTokenManager manager(
            String jwtSecret,
            long accessTokenExpirationSeconds,
            boolean secureCookie,
            MockEnvironment environment
    ) {
        JwtBeans beans = jwtBeans(jwtSecret, environment);
        return new JwtAccessTokenManager(
                beans.encoder(),
                beans.jwsHeader(),
                accessTokenExpirationSeconds,
                secureCookie
        );
    }

    private JwtBeans jwtBeans(String jwtSecret, MockEnvironment environment) {
        JwtAccessTokenConfiguration configuration = new JwtAccessTokenConfiguration(jwtSecret, environment);
        JwtAccessTokenConfiguration.JwtAccessTokenSigningMaterial signingMaterial =
                configuration.accessTokenSigningMaterial();
        JwtEncoder encoder = configuration.accessTokenJwtEncoder(signingMaterial);
        JwtDecoder decoder = configuration.accessTokenJwtDecoder(signingMaterial, new AccessTokenTypeValidator());
        JwsHeader jwsHeader = configuration.accessTokenJwsHeader(signingMaterial);
        return new JwtBeans(encoder, decoder, jwsHeader);
    }

    private record JwtBeans(JwtEncoder encoder, JwtDecoder decoder, JwsHeader jwsHeader) {
    }
}
