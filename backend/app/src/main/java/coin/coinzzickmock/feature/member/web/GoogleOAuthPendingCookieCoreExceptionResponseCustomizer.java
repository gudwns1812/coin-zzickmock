package coin.coinzzickmock.feature.member.web;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.CoreExceptionResponseCustomizer;
import coin.coinzzickmock.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
class GoogleOAuthPendingCookieCoreExceptionResponseCustomizer implements CoreExceptionResponseCustomizer {
    private static final String GOOGLE_AUTH_PATH_PREFIX = "/api/futures/auth/google/";

    private final GoogleOAuthPendingCookieFactory googleOAuthPendingCookieFactory;

    GoogleOAuthPendingCookieCoreExceptionResponseCustomizer(
            GoogleOAuthPendingCookieFactory googleOAuthPendingCookieFactory
    ) {
        this.googleOAuthPendingCookieFactory = googleOAuthPendingCookieFactory;
    }

    @Override
    public void customize(CoreException exception, HttpServletRequest request, ResponseEntity.BodyBuilder response) {
        ErrorCode errorCode = exception.errorCode();
        if (isGoogleAuthPath(request) && expiresPendingCookie(errorCode)) {
            response.header(HttpHeaders.SET_COOKIE, googleOAuthPendingCookieFactory.expire().toString());
        }
    }

    private boolean isGoogleAuthPath(HttpServletRequest request) {
        return request != null
                && request.getRequestURI() != null
                && request.getRequestURI().startsWith(GOOGLE_AUTH_PATH_PREFIX);
    }

    private boolean expiresPendingCookie(ErrorCode errorCode) {
        return errorCode == ErrorCode.OAUTH_STATE_INVALID
                || errorCode == ErrorCode.OAUTH_ONBOARDING_EXPIRED
                || errorCode == ErrorCode.OAUTH_ONBOARDING_CONSUMED
                || errorCode == ErrorCode.OAUTH_IDENTITY_ALREADY_LINKED
                || errorCode == ErrorCode.OAUTH_LINK_TOO_MANY_ATTEMPTS;
    }
}
