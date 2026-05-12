package coin.coinzzickmock.feature.reward.web;

import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import com.jayway.jsonpath.JsonPath;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.mail.host="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RewardControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RewardPointRepository rewardPointRepository;

    @Autowired
    private MemberCredentialRepository memberCredentialRepository;

    @Test
    void adminLegacyAndNewTransitionEndpointsStayCompatible() throws Exception {
        Cookie adminCookie = login("test", "test@1234");
        rewardPointRepository.save(new RewardPointWallet(memberId("test"), 300));

        String rejectedRequestId = createRedemption(adminCookie);
        mockMvc.perform(post("/api/futures/admin/reward-redemptions/{requestId}/reject", rejectedRequestId)
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memo": "reject"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        mockMvc.perform(get("/api/futures/admin/reward-redemptions?status=CANCELLED_REFUNDED")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("REJECTED"));

        String approvedRequestId = createRedemption(adminCookie);
        mockMvc.perform(post("/api/futures/admin/reward-redemptions/{requestId}/send", approvedRequestId)
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memo": "legacy approve"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/futures/admin/reward-redemptions?status=SENT")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("APPROVED"));
    }

    @Test
    void ownerHistoryAndCancelExposePreciseHttpErrors() throws Exception {
        register("reward-owner");
        register("reward-other");
        Cookie ownerCookie = login("reward-owner", "hello@1234");
        Cookie otherCookie = login("reward-other", "hello@1234");
        rewardPointRepository.save(new RewardPointWallet(memberId("reward-owner"), 300));

        String requestId = createRedemption(ownerCookie);

        mockMvc.perform(get("/api/futures/shop/redemptions").cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].requestId").value(requestId))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));

        mockMvc.perform(post("/api/futures/shop/redemptions/{requestId}/cancel", requestId)
                        .cookie(otherCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/futures/shop/redemptions/{requestId}/cancel", "missing-request")
                        .cookie(ownerCookie))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/futures/shop/redemptions/{requestId}/cancel", requestId)
                        .cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(post("/api/futures/shop/redemptions/{requestId}/cancel", requestId)
                        .cookie(ownerCookie))
                .andExpect(status().isConflict());
    }

    private String createRedemption(Cookie cookie) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/futures/shop/redemptions")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itemCode": "voucher.coffee",
                                  "phoneNumber": "010-1234-5678"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.requestId");
    }

    private void register(String account) throws Exception {
        mockMvc.perform(post("/api/futures/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "%s",
                                  "password": "hello@1234",
                                  "name": "%s",
                                  "nickname": "%s",
                                  "phoneNumber": "010-5555-6666",
                                  "email": "%s@coinzzickmock.dev",
                                  "fgOffset": "unused",
                                  "address": {
                                    "zipcode": "04524",
                                    "address": "서울 중구 세종대로 110",
                                    "addressDetail": "12층"
                                  }
                                }
                                """.formatted(account, account, account, account)))
                .andExpect(status().isOk());
    }

    private Long memberId(String account) {
        return memberCredentialRepository.findActiveByAccount(account).orElseThrow().memberId();
    }

    private Cookie login(String account, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/futures/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "%s",
                                  "password": "%s"
                                }
                                """.formatted(account, password)))
                .andExpect(status().isOk())
                .andReturn();
        return accessTokenCookie(loginResult);
    }

    private Cookie accessTokenCookie(MvcResult loginResult) {
        Cookie accessToken = loginResult.getResponse().getCookie("accessToken");
        if (accessToken == null) {
            throw new AssertionError("accessToken cookie is required");
        }
        return accessToken;
    }
}
