package coin.coinzzickmock.feature.member.web;

import coin.coinzzickmock.feature.member.application.result.MemberProfileResult;
import coin.coinzzickmock.feature.member.domain.MemberRole;
import coin.coinzzickmock.providers.auth.AccessTokenManager;
import coin.coinzzickmock.providers.auth.ActorRole;
import coin.coinzzickmock.providers.auth.AuthSessionClaims;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AccessTokenCookieFactory {
    private final AccessTokenManager accessTokenManager;

    ResponseCookie issue(MemberProfileResult memberProfile) {
        String token = accessTokenManager.issue(new AuthSessionClaims(
                memberProfile.memberId(),
                memberProfile.account(),
                memberProfile.nickname(),
                memberProfile.memberEmail(),
                toActorRole(memberProfile.role())
        ));
        return ResponseCookie.from(accessTokenManager.accessTokenCookieName(), token)
                .httpOnly(true)
                .secure(accessTokenManager.secureCookie())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(accessTokenManager.accessTokenExpirationSeconds()))
                .build();
    }

    ResponseCookie expire() {
        return ResponseCookie.from(accessTokenManager.accessTokenCookieName(), "")
                .httpOnly(true)
                .secure(accessTokenManager.secureCookie())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ActorRole toActorRole(MemberRole role) {
        return role == MemberRole.ADMIN ? ActorRole.ADMIN : ActorRole.USER;
    }
}
