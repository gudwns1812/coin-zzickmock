package coin.coinzzickmock.feature.member.web;

import coin.coinzzickmock.feature.member.application.result.MemberProfileResult;
import coin.coinzzickmock.feature.member.domain.MemberRole;
import coin.coinzzickmock.providers.auth.AccessTokenManager;
import coin.coinzzickmock.providers.auth.ActorRole;
import coin.coinzzickmock.providers.auth.AuthSessionClaims;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
class AccessTokenCookieFactory {
    private final AccessTokenManager accessTokenManager;
    private final String sameSite;

    AccessTokenCookieFactory(
            AccessTokenManager accessTokenManager,
            @Value("${APP_AUTH_COOKIE_SAME_SITE:None}") String sameSite
    ) {
        this.accessTokenManager = accessTokenManager;
        this.sameSite = sameSite;
    }

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
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ofSeconds(accessTokenManager.accessTokenExpirationSeconds()))
                .build();
    }

    ResponseCookie expire() {
        return ResponseCookie.from(accessTokenManager.accessTokenCookieName(), "")
                .httpOnly(true)
                .secure(accessTokenManager.secureCookie())
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ActorRole toActorRole(MemberRole role) {
        return role == MemberRole.ADMIN ? ActorRole.ADMIN : ActorRole.USER;
    }
}
