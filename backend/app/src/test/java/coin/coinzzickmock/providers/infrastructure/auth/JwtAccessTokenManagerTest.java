package coin.coinzzickmock.providers.infrastructure.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.providers.auth.ActorRole;
import coin.coinzzickmock.providers.auth.AuthSessionClaims;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

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

        AuthSessionClaims claims = manager.parse(token);
        assertEquals(1L, claims.memberId());
        assertEquals("demo-member", claims.account());
        assertEquals("Demo User", claims.nickname());
        assertEquals("demo@example.com", claims.email());
        assertEquals(ActorRole.ADMIN, claims.role());
    }

    @Test
    void issuesAccessTokenWithMemberSubjectAndTokenTypeClaim() {
        JwtBeans beans = jwtBeans(TEST_SECRET, new MockEnvironment());
        JwtAccessTokenManager manager = new JwtAccessTokenManager(
                beans.encoder(),
                beans.decoder(),
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

    @Test
    void rejectsMalformedToken() {
        JwtAccessTokenManager manager = manager(TEST_SECRET, 3600, true, new MockEnvironment());

        assertThrows(CoreException.class, () -> manager.parse("not-a-jwt"));
    }

    @Test
    void rejectsTokenWithUnexpectedSubject() {
        JwtAccessTokenManager manager = manager(TEST_SECRET, 3600, true, new MockEnvironment());
        String token = signedToken("OTHER_TOKEN", "ACCESS");

        assertThrows(CoreException.class, () -> manager.parse(token));
    }

    @Test
    void rejectsTokenWithoutAccessTokenType() {
        JwtAccessTokenManager manager = manager(TEST_SECRET, 3600, true, new MockEnvironment());
        String token = signedToken("1", null);

        assertThrows(CoreException.class, () -> manager.parse(token));
    }

    @Test
    void rejectsTokenWithUnexpectedTokenType() {
        JwtAccessTokenManager manager = manager(TEST_SECRET, 3600, true, new MockEnvironment());
        String token = signedToken("1", "REFRESH");

        assertThrows(CoreException.class, () -> manager.parse(token));
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
                beans.decoder(),
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
        JwtDecoder decoder = configuration.accessTokenJwtDecoder(signingMaterial);
        JwsHeader jwsHeader = configuration.accessTokenJwsHeader(signingMaterial);
        return new JwtBeans(encoder, decoder, jwsHeader);
    }

    private String signedToken(String subject, String tokenType) {
        Instant now = Instant.now();
        SecretKey secretKey = new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("memberId", 1L)
                .claim("account", "demo-member")
                .claim("role", ActorRole.USER.name());
        if (tokenType != null) {
            claimsBuilder.claim("tokenType", tokenType);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey))
                .encode(JwtEncoderParameters.from(header, claimsBuilder.build()))
                .getTokenValue();
    }

    private record JwtBeans(JwtEncoder encoder, JwtDecoder decoder, JwsHeader jwsHeader) {
    }
}
