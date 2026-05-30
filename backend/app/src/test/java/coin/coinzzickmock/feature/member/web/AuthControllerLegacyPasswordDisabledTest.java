package coin.coinzzickmock.feature.member.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.auth.legacy-password-endpoints-enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerLegacyPasswordDisabledTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void legacyPasswordLoginRegisterAndDuplicateEndpointsAreDisabledWhenFlagIsOff() throws Exception {
        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "test",
                                  "password": "test@1234"
                                }
                                """))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("PASSWORD_LOGIN_DISABLED"));

        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "disabled-register",
                                  "password": "hello@1234",
                                  "name": "disabled-register",
                                  "nickname": "disabled-register",
                                  "phoneNumber": "010-5555-6666",
                                  "email": "disabled-register@coinzzickmock.dev",
                                  "address": {
                                    "zipcode": "04524",
                                    "address": "서울 중구 세종대로 110",
                                    "addressDetail": "12층"
                                  }
                                }
                                """))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("PASSWORD_LOGIN_DISABLED"));

        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "test"
                                }
                                """))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("PASSWORD_LOGIN_DISABLED"));
    }

    private static MockHttpServletRequestBuilder postWithTrustedOrigin(String urlTemplate) {
        return post(urlTemplate)
                .header(HttpHeaders.ORIGIN, "http://localhost:3000");
    }
}
