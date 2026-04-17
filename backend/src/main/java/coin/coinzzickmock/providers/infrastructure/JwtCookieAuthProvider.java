package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.infrastructure.security.JwtAccessTokenManager;
import coin.coinzzickmock.feature.member.infrastructure.security.JwtSessionClaims;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class JwtCookieAuthProvider implements AuthProvider {
    private final JwtAccessTokenManager jwtAccessTokenManager;
    private final MemberCredentialRepository memberCredentialRepository;

    @Override
    public Actor currentActor() {
        JwtSessionClaims claims = parseRequiredClaims();
        if (memberCredentialRepository.findByMemberId(claims.memberId()).isEmpty()) {
            throw new CoreException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return new Actor(claims.memberId(), claims.email(), claims.memberName());
    }

    @Override
    public boolean isAuthenticated() {
        try {
            JwtSessionClaims claims = parseOptionalClaims();
            return claims != null && memberCredentialRepository.findByMemberId(claims.memberId()).isPresent();
        } catch (CoreException exception) {
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
