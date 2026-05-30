package coin.coinzzickmock.feature.member.web;

import coin.coinzzickmock.feature.activity.application.service.RecordMemberActivityService;
import coin.coinzzickmock.feature.activity.domain.ActivitySource;
import coin.coinzzickmock.feature.member.application.dto.GoogleOAuthLoginResolution;
import coin.coinzzickmock.feature.member.application.dto.GoogleOAuthProfile;
import coin.coinzzickmock.feature.member.application.service.ResolveGoogleOAuthLoginService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
class GoogleOAuthLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final ResolveGoogleOAuthLoginService resolveGoogleOAuthLoginService;
    private final GoogleOAuthPendingTokenCodec pendingTokenCodec;
    private final GoogleOAuthPendingCookieFactory pendingCookieFactory;
    private final AccessTokenCookieFactory accessTokenCookieFactory;
    private final RecordMemberActivityService recordMemberActivityService;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        GoogleOAuthPendingTokenCodec.PendingToken pendingToken = pendingTokenCodec.issue();
        GoogleOAuthLoginResolution resolution = resolveGoogleOAuthLoginService.resolve(
                new GoogleOAuthProfile(
                        subject(oauth2User),
                        oauth2User.getAttribute("email"),
                        oauth2User.getAttribute("name")
                ),
                pendingToken.tokenHash(),
                Instant.now().plus(pendingCookieFactory.ttl())
        );

        if (resolution.linked()) {
            recordMemberActivityService.record(resolution.memberProfile().memberId(), ActivitySource.LOGIN);
            response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookieFactory.issue(resolution.memberProfile()).toString());
            response.addHeader(HttpHeaders.SET_COOKIE, pendingCookieFactory.expire().toString());
            response.sendRedirect(frontendUrl("/markets"));
            return;
        }

        response.addHeader(HttpHeaders.SET_COOKIE, pendingCookieFactory.issue(pendingToken.rawToken()).toString());
        response.sendRedirect(frontendUrl("/auth/google/onboarding"));
    }

    private String subject(OAuth2User oauth2User) {
        Object sub = oauth2User.getAttribute("sub");
        if (sub instanceof String value && !value.isBlank()) {
            return value;
        }
        return oauth2User.getName();
    }

    private String frontendUrl(String path) {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .replacePath(path)
                .replaceQuery(null)
                .build()
                .toUriString();
    }
}
