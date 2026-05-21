package coin.coinzzickmock.providers.infrastructure.auth;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.providers.auth.AccessTokenManager;
import coin.coinzzickmock.providers.auth.ActorRole;
import coin.coinzzickmock.providers.auth.AuthSessionClaims;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenManager implements AccessTokenManager {
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwsHeader jwsHeader;
    private final long accessTokenExpirationSeconds;
    private final boolean secureCookie;

    public JwtAccessTokenManager(
            JwtEncoder jwtEncoder,
            JwtDecoder jwtDecoder,
            JwsHeader jwsHeader,
            @Value("${APP_AUTH_ACCESS_TOKEN_EXPIRATION_SECONDS:3600}") long accessTokenExpirationSeconds,
            @Value("${APP_AUTH_COOKIE_SECURE:true}") boolean secureCookie
    ) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.jwsHeader = jwsHeader;
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.secureCookie = secureCookie;
    }

    @Override
    public String issue(AuthSessionClaims sessionClaims) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenExpirationSeconds);
        ActorRole role = sessionClaims.role() == null ? ActorRole.USER : sessionClaims.role();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(accessTokenSubject(sessionClaims))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(AccessTokenTypeValidator.TOKEN_TYPE_CLAIM, AccessTokenTypeValidator.ACCESS_TOKEN_TYPE)
                .claim("memberId", sessionClaims.memberId())
                .claim("account", sessionClaims.account())
                .claim("nickname", sessionClaims.nickname())
                .claim("email", sessionClaims.email())
                .claim("role", role.name())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    @Override
    public AuthSessionClaims parse(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);

            return new AuthSessionClaims(
                    parseSubjectMemberId(jwt),
                    parseAccount(jwt),
                    jwt.getClaimAsString("nickname"),
                    jwt.getClaimAsString("email"),
                    parseRole(jwt.getClaimAsString("role"))
            );
        } catch (JwtException | IllegalArgumentException exception) {
            throw new CoreException(ErrorCode.UNAUTHORIZED);
        }
    }

    private String accessTokenSubject(AuthSessionClaims sessionClaims) {
        Long memberId = sessionClaims.memberId();
        if (memberId == null) {
            throw new IllegalArgumentException("memberId is required for access token subject.");
        }
        return memberId.toString();
    }

    private Long parseSubjectMemberId(Jwt jwt) {
        Long memberId = parseMemberId(jwt.getSubject());
        if (memberId == null) {
            throw new CoreException(ErrorCode.UNAUTHORIZED);
        }
        return memberId;
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

    private String parseAccount(Jwt jwt) {
        String account = jwt.getClaimAsString("account");
        if (account != null && !account.isBlank()) {
            return account;
        }
        Object legacyMemberId = jwt.getClaim("memberId");
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

}
