package coin.coinzzickmock.feature.positionpeek.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.feature.reward.application.repository.RewardItemBalanceRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardShopItemRepository;
import coin.coinzzickmock.feature.reward.domain.RewardItemBalance;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = CoinZzickmockApplication.class, properties = {
        "spring.task.scheduling.enabled=false",
        "spring.mail.host="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PositionPeekControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberCredentialRepository memberCredentialRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private RewardItemBalanceRepository rewardItemBalanceRepository;

    @Autowired
    private RewardShopItemRepository rewardShopItemRepository;

    @Test
    void leaderboardSearchAndPeekFlowExposeTokensAndPersistSnapshots() throws Exception {
        register("peek-viewer");
        register("peek-target");

        Cookie viewerCookie = login("peek-viewer", "hello@1234");
        Long viewerMemberId = memberId("peek-viewer");
        Long targetMemberId = memberId("peek-target");
        RewardShopItem peekItem = rewardShopItemRepository.findByCode("position.peek").orElseThrow();

        rewardItemBalanceRepository.save(RewardItemBalance.empty(viewerMemberId, peekItem.id()).addOne());
        positionRepository.save(targetMemberId, PositionSnapshot.open(
                "BTCUSDT",
                "LONG",
                "CROSS",
                10,
                1,
                100_000,
                120_000
        ));

        MvcResult leaderboardResult = mockMvc.perform(get("/api/futures/leaderboard")
                        .param("mode", "profitRate")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> leaderboardEntries = JsonPath.read(
                leaderboardResult.getResponse().getContentAsString(),
                "$.data.entries"
        );
        assertThat(leaderboardEntries)
                .isNotEmpty()
                .allSatisfy(entry -> assertThat((String) entry.get("targetToken")).isNotBlank());

        MvcResult searchResult = mockMvc.perform(get("/api/futures/leaderboard/search")
                        .param("mode", "profitRate")
                        .param("query", "peek-target")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andReturn();
        String targetToken = JsonPath.read(searchResult.getResponse().getContentAsString(), "$.data[0].targetToken");
        assertThat(targetToken).isNotBlank();

        MvcResult latestBeforeConsume = mockMvc.perform(post("/api/futures/position-peeks/latest")
                        .cookie(viewerCookie)
                        .contentType("application/json")
                        .content("""
                                {
                                  "targetToken": "%s"
                                }
                                """.formatted(targetToken)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat((Integer) JsonPath.read(latestBeforeConsume.getResponse().getContentAsString(), "$.data.remainingPeekItemCount"))
                .isEqualTo(1);
        assertThat((Object) JsonPath.read(latestBeforeConsume.getResponse().getContentAsString(), "$.data.latestSnapshot")).isNull();

        MvcResult consumeResult = mockMvc.perform(post("/api/futures/position-peeks")
                        .cookie(viewerCookie)
                        .contentType("application/json")
                        .content("""
                                {
                                  "targetToken": "%s"
                                }
                                """.formatted(targetToken)))
                .andExpect(status().isOk())
                .andReturn();
        String peekId = JsonPath.read(consumeResult.getResponse().getContentAsString(), "$.data.peekId");
        assertThat(peekId).isNotBlank();
        assertThat((List<?>) JsonPath.read(consumeResult.getResponse().getContentAsString(), "$.data.positions")).hasSize(1);
        assertThat((Integer) JsonPath.read(consumeResult.getResponse().getContentAsString(), "$.data.remainingPeekItemCount"))
                .isEqualTo(0);
        assertThat(rewardItemBalanceRepository.findByMemberIdAndShopItemId(viewerMemberId, peekItem.id()))
                .map(RewardItemBalance::remainingQuantity)
                .hasValue(0);

        MvcResult snapshotResult = mockMvc.perform(get("/api/futures/position-peeks/{peekId}", peekId)
                        .cookie(viewerCookie))
                .andExpect(status().isOk())
                .andReturn();
        assertThat((String) JsonPath.read(snapshotResult.getResponse().getContentAsString(), "$.data.peekId")).isEqualTo(peekId);
        assertThat((List<?>) JsonPath.read(snapshotResult.getResponse().getContentAsString(), "$.data.positions")).hasSize(1);
    }

    private void register(String account) throws Exception {
        mockMvc.perform(post("/api/futures/auth/register")
                        .contentType("application/json")
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
                        .contentType("application/json")
                        .content("""
                                {
                                  "account": "%s",
                                  "password": "%s"
                                }
                                """.formatted(account, password)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie accessToken = loginResult.getResponse().getCookie("accessToken");
        if (accessToken == null) {
            throw new AssertionError("accessToken cookie is required");
        }
        return accessToken;
    }
}
