package coin.coinzzickmock.feature.member.infrastructure.security;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtAccessTokenManager {
    private static final String ACCESS_TOKEN_SUBJECT = "ACCESS_TOKEN";
    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final String TEST_DEFAULT_JWT_SECRET = "coin-zzickmock-test-only-secret-please-change";

    private final SecretKey secretKey;
    private final long accessTokenExpirationSeconds;
    private final boolean secureCookie;

    public JwtAccessTokenManager(
            @Value("${JWT_SECRET:}") String jwtSecret,
            @Value("${APP_AUTH_ACCESS_TOKEN_EXPIRATION_SECONDS:3600}") long accessTokenExpirationSeconds,
            @Value("${APP_AUTH_COOKIE_SECURE:true}") boolean secureCookie,
            Environment environment
    ) {
        this.secretKey = Keys.hmacShaKeyFor(resolveJwtSecret(jwtSecret, environment).getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.secureCookie = secureCookie;
    }

    public String issue(String memberId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenExpirationSeconds);

        return Jwts.builder()
                .subject(ACCESS_TOKEN_SUBJECT)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("memberId", memberId)
                .signWith(secretKey)
                .compact();
    }

    public JwtSessionClaims parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!ACCESS_TOKEN_SUBJECT.equals(claims.getSubject())) {
                throw new CoreException(ErrorCode.UNAUTHORIZED, "유효하지 않은 액세스 토큰입니다.");
            }

            return new JwtSessionClaims(
                    claims.get("memberId", String.class)
            );
        } catch (JwtException | IllegalArgumentException exception) {
            throw new CoreException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
    }

    public ResponseCookie buildAccessTokenCookie(String token) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(accessTokenExpirationSeconds))
                .build();
    }

    public ResponseCookie expireAccessTokenCookie() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    public String accessTokenCookieName() {
        return ACCESS_TOKEN_COOKIE_NAME;
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
