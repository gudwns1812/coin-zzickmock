package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.infrastructure.security.JwtAccessTokenManager;
import coin.coinzzickmock.feature.member.infrastructure.security.JwtSessionClaims;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.ActorLookup;
import coin.coinzzickmock.providers.auth.AuthProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtCookieAuthProvider implements AuthProvider {
    private final JwtAccessTokenManager jwtAccessTokenManager;
    private final ActorLookup actorLookup;

    @Override
    public Actor currentActor() {
        JwtSessionClaims claims = parseRequiredClaims();
        return actorLookup.findByMemberId(claims.memberId())
                .orElseThrow(() -> new CoreException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    @Override
    public boolean isAuthenticated() {
        try {
            JwtSessionClaims claims = parseOptionalClaims();
            return claims != null && actorLookup.findByMemberId(claims.memberId()).isPresent();
        } catch (CoreException exception) {
            log.debug("Optional authentication failed; treating request as anonymous.", exception);
            return false;
        }
    }

    private JwtSessionClaims parseRequiredClaims() {
        JwtSessionClaims claims = parseOptionalClaims();
        if (claims == null) {
            throw new CoreException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return claims;
    }

    private JwtSessionClaims parseOptionalClaims() {
        HttpServletRequest request = currentRequest();
        if (request == null || request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> jwtAccessTokenManager.accessTokenCookieName().equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .map(jwtAccessTokenManager::parse)
                .orElse(null);
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
