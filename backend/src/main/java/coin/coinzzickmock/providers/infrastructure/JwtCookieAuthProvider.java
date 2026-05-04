package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.providers.auth.AccessTokenManager;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.ActorLookup;
import coin.coinzzickmock.providers.auth.AuthSessionClaims;
import coin.coinzzickmock.providers.auth.AuthProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtCookieAuthProvider implements AuthProvider {
    private final AccessTokenManager accessTokenManager;
    private final ActorLookup actorLookup;

    @Override
    public Actor currentActor() {
        AuthSessionClaims claims = parseRequiredClaims();
        return lookupActor(claims)
                .orElseThrow(() -> new CoreException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    @Override
    public boolean isAuthenticated() {
        return currentActorOptional().isPresent();
    }

    @Override
    public Optional<Actor> currentActorOptional() {
        try {
            AuthSessionClaims claims = parseOptionalClaims();
            return claims == null ? Optional.empty() : lookupActor(claims);
        } catch (CoreException exception) {
            log.debug("Optional authentication failed; treating request as anonymous.", exception);
            return Optional.empty();
        }
    }

    private AuthSessionClaims parseRequiredClaims() {
        AuthSessionClaims claims = parseOptionalClaims();
        if (claims == null) {
            throw new CoreException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return claims;
    }

    private AuthSessionClaims parseOptionalClaims() {
        HttpServletRequest request = currentRequest();
        if (request == null || request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> accessTokenManager.accessTokenCookieName().equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .map(accessTokenManager::parse)
                .orElse(null);
    }

    private Optional<Actor> lookupActor(AuthSessionClaims claims) {
        if (claims.memberId() != null) {
            return actorLookup.findByMemberId(claims.memberId());
        }
        if (claims.account() != null && !claims.account().isBlank()) {
            return actorLookup.findByAccount(claims.account());
        }
        return Optional.empty();
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
