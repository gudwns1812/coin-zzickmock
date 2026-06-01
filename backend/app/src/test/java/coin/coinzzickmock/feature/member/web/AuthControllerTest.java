package coin.coinzzickmock.feature.member.web;

import jakarta.servlet.http.Cookie;
import com.jayway.jsonpath.JsonPath;
import coin.coinzzickmock.feature.activity.application.service.SnapshotDailyActiveUserSummaryService;
import coin.coinzzickmock.feature.activity.domain.ActivityDate;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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
    private GoogleOAuthPendingTokenCodec googleOAuthPendingTokenCodec;

    @Autowired
    private EntityManager entityManager;

    @Test
    void demoAccountCanLoginAndReadAccountSummary() throws Exception {
        MvcResult loginResult = mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/login")
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
        MvcResult loginResult = mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/login")
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
        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "new-user",
                                  "password": "hello@1234",
                                  "name": "new-user-name",
                                  "nickname": "new-user-name",
                                  "phoneNumber": "010-5555-6666",
                                  "email": "new-user@coinzzickmock.dev",
                                  "fgOffset": "unused"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.account").value("new-user"))
                .andExpect(jsonPath("$.data.memberName").value("new-user-name"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.admin").value(false));

        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/duplicate")
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
        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "account-seed-user",
                                  "password": "hello@1234",
                                  "name": "account-seed-user",
                                  "nickname": "account-seed-user",
                                  "phoneNumber": "010-5555-6666",
                                  "email": "account-seed-user@coinzzickmock.dev",
                                  "fgOffset": "unused"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/login")
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
        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": " trimmed-user ",
                                  "password": "hello@1234",
                                  "name": " trimmed-name ",
                                  "nickname": " trimmed-name ",
                                  "phoneNumber": "010-5555-6666",
                                  "email": "trimmed-user@coinzzickmock.dev",
                                  "fgOffset": "unused"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.account").value("trimmed-user"))
                .andExpect(jsonPath("$.data.memberName").value("trimmed-name"));

        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": " trimmed-user "
                                }
                                """))
                .andExpect(status().isConflict());

        MvcResult loginResult = mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/login")
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
    void meReadsAuthenticatedUserWithoutRefreshingCookie() throws Exception {
        MvcResult loginResult = mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "test",
                                  "password": "test@1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(get("/api/futures/auth/me").cookie(accessTokenCookie(loginResult)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.account").value("test"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.admin").value(true));
    }


    @Test
    void authRecoveryEndpointsIgnoreMalformedAccessTokenCookie() throws Exception {
        Cookie malformedCookie = new Cookie("accessToken", "not-a-jwt");

        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/login")
                        .cookie(malformedCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "test",
                                  "password": "test@1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("accessToken=")));

        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/logout")
                        .cookie(malformedCookie))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test
    void publicReadAllowsAnonymousAndRejectsMalformedCookie() throws Exception {
        mockMvc.perform(get("/api/futures/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/futures/leaderboard")
                        .cookie(new Cookie("accessToken", "not-a-jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    void publicReadRejectsStaleWithdrawnMemberToken() throws Exception {
        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "public-stale-user",
                                  "password": "withdraw@1234",
                                  "name": "public-stale-user",
                                  "nickname": "public-stale-user",
                                  "phoneNumber": "010-7777-9999",
                                  "email": "public-stale-user@coinzzickmock.dev",
                                  "fgOffset": "unused"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "public-stale-user",
                                  "password": "withdraw@1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Cookie staleAccessToken = accessTokenCookie(loginResult);
        Long memberId = memberId(loginResult);

        mockMvc.perform(deleteWithTrustedOrigin("/api/futures/auth/withdraw")
                        .cookie(staleAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": %d
                                }
                                """.formatted(memberId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/futures/leaderboard").cookie(staleAccessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void adminApisRejectAuthenticatedNonAdminInSecurityLayer() throws Exception {
        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "plain-user-admin-denied",
                                  "password": "hello@1234",
                                  "name": "plain-user-admin-denied",
                                  "nickname": "plain-user-admin-denied",
                                  "phoneNumber": "010-2222-3333",
                                  "email": "plain-user-admin-denied@coinzzickmock.dev",
                                  "fgOffset": "unused"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "plain-user-admin-denied",
                                  "password": "hello@1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(get("/api/futures/admin/shop-items")
                        .cookie(accessTokenCookie(loginResult)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void meRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/futures/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void withdrawnMemberCannotLoginAgain() throws Exception {
        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "withdraw-user",
                                  "password": "withdraw@1234",
                                  "name": "withdraw-user",
                                  "nickname": "withdraw-user",
                                  "phoneNumber": "010-7777-8888",
                                  "email": "withdraw-user@coinzzickmock.dev",
                                  "fgOffset": "unused"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/login")
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

        mockMvc.perform(deleteWithTrustedOrigin("/api/futures/auth/withdraw")
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

        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "withdraw-user",
                                  "password": "withdraw@1234"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "withdraw-user",
                                  "password": "withdraw@1234",
                                  "name": "withdraw-user",
                                  "nickname": "withdraw-user",
                                  "phoneNumber": "010-7777-8888",
                                  "email": "withdraw-user-again@coinzzickmock.dev",
                                  "fgOffset": "unused"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void withdrawnMemberRefreshWithOldTokenFails() throws Exception {
        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "refresh-withdraw-user",
                                  "password": "withdraw@1234",
                                  "name": "refresh-withdraw-user",
                                  "nickname": "refresh-withdraw-user",
                                  "phoneNumber": "010-7777-8888",
                                  "email": "refresh-withdraw-user@coinzzickmock.dev",
                                  "fgOffset": "unused"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/login")
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

        mockMvc.perform(deleteWithTrustedOrigin("/api/futures/auth/withdraw")
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
    void googleOnboardingRequiresPendingCookieAndExpiresItWhenMissing() throws Exception {
        mockMvc.perform(get("/api/futures/auth/google/onboarding"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("googleOAuthPending=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(jsonPath("$.code").value("OAUTH_ONBOARDING_EXPIRED"));
    }

    @Test
    void googleLinkAttachesIdentityToExistingMemberAndPreservesMemberId() throws Exception {
        GoogleOAuthPendingTokenCodec.PendingToken pendingToken = createPendingGoogleLink(
                "google-sub-link-existing",
                "existing-google@coinzzickmock.dev",
                "Existing Google User"
        );
        Long demoMemberId = jdbcTemplate.queryForObject(
                "select id from member_credentials where account = ?",
                Long.class,
                "test"
        );

        MvcResult result = mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/google/link")
                        .cookie(pendingCookie(pendingToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "test",
                                  "password": "test@1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value(demoMemberId))
                .andReturn();

        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(header -> assertThat(header).contains("accessToken="));
        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(header -> assertThat(header).contains("googleOAuthPending=").contains("Max-Age=0"));
        assertThat(identityMemberId("google-sub-link-existing")).isEqualTo(demoMemberId);
    }

    @Test
    void googleLinkTerminalIdentityConflictConsumesPendingLink() throws Exception {
        GoogleOAuthPendingTokenCodec.PendingToken pendingToken = createPendingGoogleLink(
                "google-sub-conflict-existing",
                "conflict-google@coinzzickmock.dev",
                "Conflict Google User"
        );
        Long demoMemberId = jdbcTemplate.queryForObject(
                "select id from member_credentials where account = ?",
                Long.class,
                "test"
        );
        jdbcTemplate.update(
                """
                        insert into member_oauth_identities
                            (member_id, provider, provider_subject, provider_email, provider_name)
                        values (?, 'google', ?, ?, ?)
                        """,
                demoMemberId,
                "google-sub-conflict-existing",
                "conflict-google@coinzzickmock.dev",
                "Conflict Google User"
        );

        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/google/link")
                        .cookie(pendingCookie(pendingToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "test",
                                  "password": "test@1234"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("googleOAuthPending=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(jsonPath("$.code").value("OAUTH_IDENTITY_ALREADY_LINKED"));

        entityManager.flush();
        assertThat(pendingConsumedAt(pendingToken.tokenHash())).isNotNull();
    }

    @Test
    void googleLinkExpiresPendingCookieAfterTooManyInvalidCredentialAttempts() throws Exception {
        GoogleOAuthPendingTokenCodec.PendingToken pendingToken = createPendingGoogleLink(
                "google-sub-link-too-many-attempts",
                "too-many-attempts@coinzzickmock.dev",
                "Too Many Attempts"
        );
        Cookie pendingCookie = pendingCookie(pendingToken);

        for (int attempt = 1; attempt < 5; attempt++) {
            mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/google/link")
                            .cookie(pendingCookie)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "account": "test",
                                      "password": "wrong-password"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/google/link")
                        .cookie(pendingCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "test",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("googleOAuthPending=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(jsonPath("$.code").value("OAUTH_LINK_TOO_MANY_ATTEMPTS"));
    }

    @Test
    void googleSignupCreatesGoogleOnlyMemberAndProvisionedTradingAccount() throws Exception {
        GoogleOAuthPendingTokenCodec.PendingToken pendingToken = createPendingGoogleLink(
                "google-sub-new-member",
                "new-google@coinzzickmock.dev",
                "New Google User"
        );

        MvcResult result = mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/google/signup")
                        .cookie(pendingCookie(pendingToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "New Google User",
                                  "nickname": "New Google Nick",
                                  "phoneNumber": "010-3333-4444",
                                  "email": "new-google@coinzzickmock.dev",
                                  "agreement": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.account").doesNotExist())
                .andExpect(jsonPath("$.data.nickname").value("New Google Nick"))
                .andReturn();

        Long memberId = memberId(result);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from member_credentials where id = ? and account is null and password_hash is null",
                Long.class,
                memberId
        )).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from trading_accounts where member_id = ?",
                Long.class,
                memberId
        )).isEqualTo(1L);
        assertThat(identityMemberId("google-sub-new-member")).isEqualTo(memberId);
    }

    @Test
    void googleSignupRejectsEmailChangedFromPendingGoogleIdentity() throws Exception {
        GoogleOAuthPendingTokenCodec.PendingToken pendingToken = createPendingGoogleLink(
                "google-sub-email-tampered",
                "real-google@coinzzickmock.dev",
                "Real Google User"
        );

        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/google/signup")
                        .cookie(pendingCookie(pendingToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Real Google User",
                                  "nickname": "Real Google Nick",
                                  "phoneNumber": "010-3333-4444",
                                  "email": "attacker@coinzzickmock.dev",
                                  "agreement": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from member_oauth_identities where provider_subject = ?",
                Long.class,
                "google-sub-email-tampered"
        )).isEqualTo(0L);
    }

    @Test
    void meRejectsStaleWithdrawnMemberToken() throws Exception {
        mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "me-withdraw-user",
                                  "password": "withdraw@1234",
                                  "name": "me-withdraw-user",
                                  "nickname": "me-withdraw-user",
                                  "phoneNumber": "010-7777-8888",
                                  "email": "me-withdraw-user@coinzzickmock.dev",
                                  "fgOffset": "unused"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "me-withdraw-user",
                                  "password": "withdraw@1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Cookie staleAccessToken = accessTokenCookie(loginResult);
        Long memberId = memberId(loginResult);

        mockMvc.perform(deleteWithTrustedOrigin("/api/futures/auth/withdraw")
                        .cookie(staleAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": %d
                                }
                                """.formatted(memberId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/futures/auth/me").cookie(staleAccessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void withdrawRejectsAnotherMembersId() throws Exception {
        MvcResult loginResult = mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "test",
                                  "password": "test@1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(deleteWithTrustedOrigin("/api/futures/auth/withdraw")
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
        MvcResult loginResult = mockMvc.perform(postWithTrustedOrigin("/api/futures/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "test",
                                  "password": "test@1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(deleteWithTrustedOrigin("/api/futures/auth/withdraw")
                        .cookie(accessTokenCookie(loginResult))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": "   "
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private GoogleOAuthPendingTokenCodec.PendingToken createPendingGoogleLink(
            String providerSubject,
            String providerEmail,
            String providerName
    ) {
        GoogleOAuthPendingTokenCodec.PendingToken pendingToken = googleOAuthPendingTokenCodec.issue();
        jdbcTemplate.update(
                """
                        insert into member_oauth_pending_links
                            (token_hash, provider, provider_subject, provider_email, provider_name, expires_at)
                        values (?, 'google', ?, ?, ?, ?)
                        """,
                pendingToken.tokenHash(),
                providerSubject,
                providerEmail,
                providerName,
                Timestamp.from(Instant.now().plusSeconds(600))
        );
        return pendingToken;
    }

    private Cookie pendingCookie(GoogleOAuthPendingTokenCodec.PendingToken pendingToken) {
        return new Cookie(GoogleOAuthPendingCookieFactory.COOKIE_NAME, pendingToken.rawToken());
    }

    private Long identityMemberId(String providerSubject) {
        return jdbcTemplate.queryForObject(
                "select member_id from member_oauth_identities where provider = 'google' and provider_subject = ?",
                Long.class,
                providerSubject
        );
    }

    private java.sql.Timestamp pendingConsumedAt(String tokenHash) {
        return jdbcTemplate.queryForObject(
                "select consumed_at from member_oauth_pending_links where token_hash = ?",
                java.sql.Timestamp.class,
                tokenHash
        );
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

    private static MockHttpServletRequestBuilder postWithTrustedOrigin(String urlTemplate, Object... uriVars) {
        return post(urlTemplate, uriVars)
                .header(HttpHeaders.ORIGIN, "http://localhost:3000");
    }

    private static MockHttpServletRequestBuilder deleteWithTrustedOrigin(String urlTemplate, Object... uriVars) {
        return delete(urlTemplate, uriVars)
                .header(HttpHeaders.ORIGIN, "http://localhost:3000");
    }
}
