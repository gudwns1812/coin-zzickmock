package stock.stockzzickmock.support.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;
import stock.stockzzickmock.core.domain.member.Member;
import stock.stockzzickmock.support.error.AuthErrorType;
import stock.stockzzickmock.support.error.CoreException;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(toBase64(jwtProperties.secret())));
    }

    public String createAccessToken(Member member) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(jwtProperties.accessExpirationSeconds());

        return Jwts.builder()
                .subject(JwtTokenType.ACCESS_TOKEN.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim("memberId", member.getMemberId())
                .claim("memberName", member.getProfile().getName())
                .claim("email", member.getProfile().getEmail())
                .claim("phoneNumber", member.getProfile().getPhoneNumber())
                .claim("zipCode", member.getAddress().getZipCode())
                .claim("Address", member.getAddress().getAddress())
                .claim("AddressDetail", member.getAddress().getAddressDetail())
                .claim("invest", member.getInvest())
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Member member) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(jwtProperties.refreshExpirationSeconds());

        return Jwts.builder()
                .subject(JwtTokenType.REFRESH_TOKEN.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim("memberId", member.getMemberId())
                .claim("account", member.getAccount().getAccount())
                .claim("version", member.getRefreshTokenVersion())
                .signWith(secretKey)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        Claims claims = parse(token);
        if (!JwtTokenType.ACCESS_TOKEN.name().equals(claims.getSubject())) {
            throw new CoreException(AuthErrorType.INVALID_JWT);
        }
        return claims;
    }

    public Claims parseRefreshToken(String token) {
        Claims claims = parse(token);
        if (!JwtTokenType.REFRESH_TOKEN.name().equals(claims.getSubject())) {
            throw new CoreException(AuthErrorType.INVALID_JWT);
        }
        return claims;
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new CoreException(AuthErrorType.INVALID_JWT, e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CoreException(AuthErrorType.INVALID_JWT, e);
        }
    }

    private String toBase64(String rawSecret) {
        return Base64.getEncoder().encodeToString(rawSecret.getBytes());
    }
}
