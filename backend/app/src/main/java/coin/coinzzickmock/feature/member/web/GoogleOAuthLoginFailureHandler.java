package coin.coinzzickmock.feature.member.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
class GoogleOAuthLoginFailureHandler implements AuthenticationFailureHandler {
    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        response.sendRedirect(UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .replacePath("/login")
                .replaceQueryParam("oauth", "failed")
                .build()
                .toUriString());
    }
}
