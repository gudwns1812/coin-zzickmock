package coin.coinzzickmock.feature.member.web;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
class GoogleOAuthPendingCookieFactory {
    static final String COOKIE_NAME = "googleOAuthPending";

    private final String sameSite;
    private final boolean secure;
    private final Duration ttl;

    GoogleOAuthPendingCookieFactory(
            @Value("${APP_AUTH_COOKIE_SAME_SITE:None}") String sameSite,
            @Value("${APP_AUTH_COOKIE_SECURE:true}") boolean secure,
            @Value("${app.auth.google.pending-cookie-ttl:PT10M}") Duration ttl
    ) {
        this.sameSite = sameSite;
        this.secure = secure;
        this.ttl = ttl;
    }

    Duration ttl() {
        return ttl;
    }

    ResponseCookie issue(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(ttl)
                .build();
    }

    ResponseCookie expire() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }
}
