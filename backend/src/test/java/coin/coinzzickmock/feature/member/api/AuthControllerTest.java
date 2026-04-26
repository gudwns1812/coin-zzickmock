package coin.coinzzickmock.feature.member.api;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void demoAccountCanLoginAndReadAccountSummary() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/futures/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "test",
                                  "password": "test@1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("accessToken=")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value("test"))
                .andExpect(jsonPath("$.data.memberName").value("demo-trader"))
                .andReturn();

        Cookie accessTokenCookie = accessTokenCookie(loginResult);

        mockMvc.perform(get("/api/futures/account/me").cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value("test"))
                .andExpect(jsonPath("$.data.memberName").value("demo-trader"));
    }

    @Test
    void registerMakesAccountUnavailableForDuplicateCheck() throws Exception {
        mockMvc.perform(post("/api/futures/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "new-user",
                                  "password": "hello@1234",
                                  "name": "new-user-name",
                                  "phoneNumber": "010-5555-6666",
                                  "email": "new-user@coinzzickmock.dev",
                                  "fgOffset": "unused",
                                  "address": {
                                    "zipcode": "04524",
                                    "address": "서울 중구 세종대로 110",
                                    "addressDetail": "12층"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value("new-user"))
                .andExpect(jsonPath("$.data.memberName").value("new-user-name"));

        mockMvc.perform(post("/api/futures/auth/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "new-user"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void registerCreatesDefaultTradingAccountState() throws Exception {
        mockMvc.perform(post("/api/futures/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "account-seed-user",
                                  "password": "hello@1234",
                                  "name": "account-seed-user",
                                  "phoneNumber": "010-5555-6666",
                                  "email": "account-seed-user@coinzzickmock.dev",
                                  "fgOffset": "unused",
                                  "address": {
                                    "zipcode": "04524",
                                    "address": "서울 중구 세종대로 110",
                                    "addressDetail": "12층"
                                  }
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/futures/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "account-seed-user",
                                  "password": "hello@1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(get("/api/futures/account/me").cookie(accessTokenCookie(loginResult)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value("account-seed-user"))
                .andExpect(jsonPath("$.data.usdtBalance").value(100000.0))
                .andExpect(jsonPath("$.data.walletBalance").value(100000.0))
                .andExpect(jsonPath("$.data.available").value(100000.0))
                .andExpect(jsonPath("$.data.totalUnrealizedPnl").value(0.0))
                .andExpect(jsonPath("$.data.roi").value(0.0));
    }

    @Test
    void registerAndLoginNormalizeTrimmedIdentityFields() throws Exception {
        mockMvc.perform(post("/api/futures/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": " trimmed-user ",
                                  "password": "hello@1234",
                                  "name": " trimmed-name ",
                                  "phoneNumber": "010-5555-6666",
                                  "email": "trimmed-user@coinzzickmock.dev",
                                  "fgOffset": "unused",
                                  "address": {
                                    "zipcode": "04524",
                                    "address": "서울 중구 세종대로 110",
                                    "addressDetail": " 12층 "
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value("trimmed-user"))
                .andExpect(jsonPath("$.data.memberName").value("trimmed-name"));

        mockMvc.perform(post("/api/futures/auth/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": " trimmed-user "
                                }
                                """))
                .andExpect(status().isConflict());

        MvcResult loginResult = mockMvc.perform(post("/api/futures/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": " trimmed-user ",
                                  "password": "hello@1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value("trimmed-user"))
                .andExpect(jsonPath("$.data.memberName").value("trimmed-name"))
                .andReturn();

        mockMvc.perform(get("/api/futures/auth/refresh").cookie(accessTokenCookie(loginResult)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value("trimmed-user"))
                .andExpect(jsonPath("$.data.memberName").value("trimmed-name"));
    }

    @Test
    void withdrawnMemberCannotLoginAgain() throws Exception {
        mockMvc.perform(post("/api/futures/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "withdraw-user",
                                  "password": "withdraw@1234",
                                  "name": "withdraw-user",
                                  "phoneNumber": "010-7777-8888",
                                  "email": "withdraw-user@coinzzickmock.dev",
                                  "fgOffset": "unused",
                                  "address": {
                                    "zipcode": "06236",
                                    "address": "서울 강남구 논현로 507",
                                    "addressDetail": "7층"
                                  }
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/futures/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "withdraw-user",
                                  "password": "withdraw@1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(delete("/api/futures/auth/withdraw")
                        .cookie(accessTokenCookie(loginResult))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": "withdraw-user"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        mockMvc.perform(post("/api/futures/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "withdraw-user",
                                  "password": "withdraw@1234"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void withdrawnMemberRefreshWithOldTokenFails() throws Exception {
        mockMvc.perform(post("/api/futures/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "refresh-withdraw-user",
                                  "password": "withdraw@1234",
                                  "name": "refresh-withdraw-user",
                                  "phoneNumber": "010-7777-8888",
                                  "email": "refresh-withdraw-user@coinzzickmock.dev",
                                  "fgOffset": "unused",
                                  "address": {
                                    "zipcode": "06236",
                                    "address": "서울 강남구 논현로 507",
                                    "addressDetail": "7층"
                                  }
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/futures/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "refresh-withdraw-user",
                                  "password": "withdraw@1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Cookie staleAccessToken = accessTokenCookie(loginResult);

        mockMvc.perform(delete("/api/futures/auth/withdraw")
                        .cookie(staleAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": "refresh-withdraw-user"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/futures/auth/refresh").cookie(staleAccessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void withdrawRejectsAnotherMembersId() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/futures/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "test",
                                  "password": "test@1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(delete("/api/futures/auth/withdraw")
                        .cookie(accessTokenCookie(loginResult))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": "other-user"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void withdrawRejectsBlankMemberId() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/futures/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "test",
                                  "password": "test@1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(delete("/api/futures/auth/withdraw")
                        .cookie(accessTokenCookie(loginResult))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private Cookie accessTokenCookie(MvcResult loginResult) {
        String setCookieHeader = loginResult.getResponse().getHeader("Set-Cookie");
        int tokenStart = "accessToken=".length();
        int tokenEnd = setCookieHeader.indexOf(';');
        return new Cookie("accessToken", setCookieHeader.substring(tokenStart, tokenEnd));
    }
}
