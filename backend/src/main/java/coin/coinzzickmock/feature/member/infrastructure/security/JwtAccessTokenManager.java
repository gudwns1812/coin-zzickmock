package coin.coinzzickmock.feature.member.infrastructure.security;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

    private final SecretKey secretKey;
    private final long accessTokenExpirationSeconds;

    public JwtAccessTokenManager(
            @Value("${JWT_SECRET:coin-zzickmock-local-dev-secret-please-change}") String jwtSecret,
            @Value("${APP_AUTH_ACCESS_TOKEN_EXPIRATION_SECONDS:3600}") long accessTokenExpirationSeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
    }

    public String issue(MemberCredential memberCredential) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenExpirationSeconds);

        return Jwts.builder()
                .subject(ACCESS_TOKEN_SUBJECT)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("memberId", memberCredential.memberId())
                .claim("memberName", memberCredential.memberName())
                .claim("email", memberCredential.memberEmail())
                .claim("phoneNumber", memberCredential.phoneNumber())
                .claim("zipCode", memberCredential.zipCode())
                .claim("Address", memberCredential.address())
                .claim("AddressDetail", memberCredential.addressDetail())
                .claim("invest", memberCredential.investScore())
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
                    claims.get("memberId", String.class),
                    claims.get("memberName", String.class),
                    claims.get("email", String.class),
                    claims.get("phoneNumber", String.class),
                    claims.get("zipCode", String.class),
                    claims.get("Address", String.class),
                    claims.get("AddressDetail", String.class),
                    claims.get("invest", Integer.class) == null ? 0 : claims.get("invest", Integer.class)
            );
        } catch (JwtException | IllegalArgumentException exception) {
            throw new CoreException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
    }

    public ResponseCookie buildAccessTokenCookie(String token) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(accessTokenExpirationSeconds))
                .build();
    }

    public ResponseCookie expireAccessTokenCookie() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    public String accessTokenCookieName() {
        return ACCESS_TOKEN_COOKIE_NAME;
    }
}
