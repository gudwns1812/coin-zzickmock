package coin.coinzzickmock.common.web.security;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

final class GoogleOAuthCredentials {
    private GoogleOAuthCredentials() {
    }

    static boolean areConfigured(Environment environment) {
        return StringUtils.hasText(environment.getProperty("app.auth.google.client-id"))
                && StringUtils.hasText(environment.getProperty("app.auth.google.client-secret"));
    }
}
