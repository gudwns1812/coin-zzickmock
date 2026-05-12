package coin.coinzzickmock.providers.infrastructure.auth;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.providers.auth.AccessTokenManager;
import coin.coinzzickmock.providers.auth.ActorRole;
import coin.coinzzickmock.providers.auth.AuthSessionClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtAccessTokenManager implements AccessTokenManager {
    private static final String ACCESS_TOKEN_SUBJECT = "ACCESS_TOKEN";
    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final String TEST_DEFAULT_JWT_SECRET = "coin-zzickmock-test-only-secret-please-change";

    private final SecretKey secretKey;
    private final long accessTokenExpirationSeconds;
    private final boolean secureCookie;

    public JwtAccessTokenManager(
            @Value("${app.auth.jwt-secret:}") String jwtSecret,
            @Value("${APP_AUTH_ACCESS_TOKEN_EXPIRATION_SECONDS:3600}") long accessTokenExpirationSeconds,
            @Value("${APP_AUTH_COOKIE_SECURE:true}") boolean secureCookie,
            Environment environment
    ) {
        this.secretKey = Keys.hmacShaKeyFor(resolveJwtSecret(jwtSecret, environment).getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.secureCookie = secureCookie;
    }

    @Override
    public String issue(AuthSessionClaims sessionClaims) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenExpirationSeconds);
        ActorRole role = sessionClaims.role() == null ? ActorRole.USER : sessionClaims.role();

        return Jwts.builder()
                .subject(ACCESS_TOKEN_SUBJECT)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("memberId", sessionClaims.memberId())
                .claim("account", sessionClaims.account())
                .claim("nickname", sessionClaims.nickname())
                .claim("email", sessionClaims.email())
                .claim("role", role.name())
                .signWith(secretKey)
                .compact();
    }

    @Override
    public AuthSessionClaims parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!ACCESS_TOKEN_SUBJECT.equals(claims.getSubject())) {
                throw new CoreException(ErrorCode.UNAUTHORIZED);
            }

            return new AuthSessionClaims(
                    parseMemberId(claims.get("memberId")),
                    parseAccount(claims),
                    claims.get("nickname", String.class),
                    claims.get("email", String.class),
                    parseRole(claims.get("role", String.class))
            );
        } catch (JwtException | IllegalArgumentException exception) {
            throw new CoreException(ErrorCode.UNAUTHORIZED);
        }
    }

    private Long parseMemberId(Object memberId) {
        if (memberId instanceof Number number) {
            return number.longValue();
        }
        if (memberId instanceof String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String parseAccount(Claims claims) {
        String account = claims.get("account", String.class);
        if (account != null && !account.isBlank()) {
            return account;
        }
        Object legacyMemberId = claims.get("memberId");
        return legacyMemberId instanceof String value && !value.isBlank() ? value : null;
    }

    @Override
    public String accessTokenCookieName() {
        return ACCESS_TOKEN_COOKIE_NAME;
    }

    @Override
    public long accessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    @Override
    public boolean secureCookie() {
        return secureCookie;
    }

    private ActorRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            return ActorRole.USER;
        }
        try {
            return ActorRole.valueOf(role);
        } catch (IllegalArgumentException exception) {
            return ActorRole.USER;
        }
    }

    private String resolveJwtSecret(String jwtSecret, Environment environment) {
        if (jwtSecret != null && !jwtSecret.isBlank()) {
            return jwtSecret;
        }
        for (String profile : environment.getActiveProfiles()) {
            if ("test".equals(profile)) {
                return TEST_DEFAULT_JWT_SECRET;
            }
        }
        throw new IllegalStateException("JWT_SECRET must be configured outside test profile.");
    }
}
