package coin.coinzzickmock.common.web.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@Conditional(GoogleOAuthCredentialsMissing.class)
class GoogleOAuthUnavailableController {
    private final String frontendBaseUrl;

    GoogleOAuthUnavailableController(
            @Value("${app.frontend.base-url:http://localhost:3000}") String frontendBaseUrl
    ) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @GetMapping("/oauth2/authorization/google")
    void redirectToLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect(UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .replacePath("/login")
                .replaceQueryParam("oauth", "unavailable")
                .build()
                .toUriString());
    }
}
