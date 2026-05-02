package coin.coinzzickmock.feature.member.web;

import jakarta.servlet.http.Cookie;
import com.jayway.jsonpath.JsonPath;
import coin.coinzzickmock.feature.activity.application.service.SnapshotDailyActiveUserSummaryService;
import coin.coinzzickmock.feature.activity.domain.ActivityDate;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SnapshotDailyActiveUserSummaryService snapshotDailyActiveUserSummaryService;

    @Autowired
    private EntityManager entityManager;

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
                .andExpect(jsonPath("$.data.account").value("test"))
                .andExpect(jsonPath("$.data.memberName").value("demo-trader"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.admin").value(true))
                .andReturn();

        Cookie accessTokenCookie = accessTokenCookie(loginResult);

        mockMvc.perform(get("/api/futures/account/me").cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.account").value("test"))
                .andExpect(jsonPath("$.data.memberName").value("demo-trader"));
    }

    @Test
    void loginAndAuthenticatedApiRecordDailyActivity() throws Exception {
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

        Long memberId = memberId(loginResult);

        assertThat(activityCount(memberId)).isEqualTo(1);
        assertThat(activityColumn(memberId, "first_source")).isEqualTo("LOGIN");
        assertThat(activityColumn(memberId, "last_source")).isEqualTo("LOGIN");

        mockMvc.perform(get("/api/futures/account/me").cookie(accessTokenCookie(loginResult)))
                .andExpect(status().isOk());

        assertThat(activityCount(memberId)).isEqualTo(2);
        assertThat(activityColumn(memberId, "first_source")).isEqualTo("LOGIN");
        assertThat(activityColumn(memberId, "last_source")).isEqualTo("AUTHENTICATED_API");
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
                                  "nickname": "new-user-name",
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
                .andExpect(jsonPath("$.data.account").value("new-user"))
                .andExpect(jsonPath("$.data.memberName").value("new-user-name"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.admin").value(false));

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
                                  "nickname": "account-seed-user",
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
                .andExpect(jsonPath("$.data.account").value("account-seed-user"))
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
                                  "nickname": " trimmed-name ",
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
                .andExpect(jsonPath("$.data.account").value("trimmed-user"))
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
                .andExpect(jsonPath("$.data.account").value("trimmed-user"))
                .andExpect(jsonPath("$.data.memberName").value("trimmed-name"))
                .andExpect(jsonPath("$.data.admin").value(false))
                .andReturn();

        mockMvc.perform(get("/api/futures/auth/refresh").cookie(accessTokenCookie(loginResult)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.account").value("trimmed-user"))
                .andExpect(jsonPath("$.data.memberName").value("trimmed-name"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.admin").value(false));
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
                                  "nickname": "withdraw-user",
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
        Long memberId = memberId(loginResult);
        LocalDate activityDate = LocalDate.now(ActivityDate.REPORTING_ZONE);

        snapshotDailyActiveUserSummaryService.snapshot(activityDate);
        assertThat(summaryCount(activityDate)).isEqualTo(1);

        mockMvc.perform(delete("/api/futures/auth/withdraw")
                        .cookie(accessTokenCookie(loginResult))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": %d
                                }
                                """.formatted(memberId)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        entityManager.flush();
        assertThat(memberCredentialRows(memberId)).isOne();
        assertThat(withdrawnAt(memberId)).isNotNull();
        assertThat(activityRows(memberId)).isEqualTo(1);
        assertThat(summaryCount(activityDate)).isEqualTo(1);

        mockMvc.perform(post("/api/futures/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "withdraw-user",
                                  "password": "withdraw@1234"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/futures/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "withdraw-user",
                                  "password": "withdraw@1234",
                                  "name": "withdraw-user",
                                  "nickname": "withdraw-user",
                                  "phoneNumber": "010-7777-8888",
                                  "email": "withdraw-user-again@coinzzickmock.dev",
                                  "fgOffset": "unused",
                                  "address": {
                                    "zipcode": "06236",
                                    "address": "서울 강남구 논현로 507",
                                    "addressDetail": "7층"
                                  }
                                }
                                """))
                .andExpect(status().isConflict());
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
                                  "nickname": "refresh-withdraw-user",
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
        Long memberId = memberId(loginResult);

        mockMvc.perform(delete("/api/futures/auth/withdraw")
                        .cookie(staleAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": %d
                                }
                                """.formatted(memberId)))
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
                                  "memberId": 999999
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
                .andExpect(status().isForbidden());
    }

    private Cookie accessTokenCookie(MvcResult loginResult) {
        String setCookieHeader = loginResult.getResponse().getHeader("Set-Cookie");
        int tokenStart = "accessToken=".length();
        int tokenEnd = setCookieHeader.indexOf(';');
        return new Cookie("accessToken", setCookieHeader.substring(tokenStart, tokenEnd));
    }

    private Long memberId(MvcResult result) throws Exception {
        Number value = JsonPath.read(result.getResponse().getContentAsString(), "$.data.memberId");
        return value.longValue();
    }

    private long activityCount(Long memberId) {
        return jdbcTemplate.queryForObject(
                "select activity_count from member_daily_activity where member_id = ?",
                Long.class,
                memberId
        );
    }

    private String activityColumn(Long memberId, String column) {
        return jdbcTemplate.queryForObject(
                "select " + column + " from member_daily_activity where member_id = ?",
                String.class,
                memberId
        );
    }

    private long activityRows(Long memberId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from member_daily_activity where member_id = ?",
                Long.class,
                memberId
        );
    }

    private long memberCredentialRows(Long memberId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from member_credentials where id = ?",
                Long.class,
                memberId
        );
    }

    private java.sql.Timestamp withdrawnAt(Long memberId) {
        return jdbcTemplate.queryForObject(
                "select withdrawn_at from member_credentials where id = ?",
                java.sql.Timestamp.class,
                memberId
        );
    }

    private long summaryCount(LocalDate activityDate) {
        return jdbcTemplate.queryForObject(
                "select active_user_count from daily_active_user_summary where activity_date = ?",
                Long.class,
                activityDate
        );
    }
}
