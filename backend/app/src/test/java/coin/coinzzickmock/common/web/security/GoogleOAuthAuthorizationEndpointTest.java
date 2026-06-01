package coin.coinzzickmock.common.web.security;

import coin.coinzzickmock.CoinZzickmockApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CoinZzickmockApplication.class, properties = {
        "app.auth.google.client-id=google-client-id",
        "app.auth.google.client-secret=google-client-secret"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoogleOAuthAuthorizationEndpointTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void googleAuthorizationEndpointRedirectsToGoogleWhenCredentialsAreConfigured() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("https://accounts.google.com/o/oauth2/v2/auth")))
                .andExpect(header().string("Location", containsString("client_id=google-client-id")))
                .andExpect(header().string("Set-Cookie", containsString("oauth2AuthorizationRequest=")));
    }
}
