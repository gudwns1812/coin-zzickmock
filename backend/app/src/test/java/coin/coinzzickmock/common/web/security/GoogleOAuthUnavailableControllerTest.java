package coin.coinzzickmock.common.web.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GoogleOAuthUnavailableControllerTest {
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new GoogleOAuthUnavailableController("http://localhost:3000"))
            .build();

    @Test
    void redirectsBackToLoginInsteadOfReturning404WhenCredentialsAreMissing() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:3000/login?oauth=unavailable"));
    }
}
