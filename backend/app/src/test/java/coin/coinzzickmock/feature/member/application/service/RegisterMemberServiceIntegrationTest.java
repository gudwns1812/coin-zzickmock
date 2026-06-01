package coin.coinzzickmock.feature.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.account.application.repository.AccountRefillStateRepository;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.event.TradingAccountOpenedEvent;
import coin.coinzzickmock.feature.account.application.service.AccountRefillDatePolicy;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import coin.coinzzickmock.feature.member.application.dto.MemberProfileResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {
        CoinZzickmockApplication.class,
        RegisterMemberServiceIntegrationTest.WalletEventCaptureConfiguration.class
})
@ActiveProfiles("test")
class RegisterMemberServiceIntegrationTest {
    @Autowired
    private RegisterMemberService registerMemberService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountRefillStateRepository accountRefillStateRepository;

    @Autowired
    private AccountRefillDatePolicy accountRefillDatePolicy;

    @Autowired
    private CapturedWalletEvents capturedWalletEvents;

    @BeforeEach
    void setUp() {
        capturedWalletEvents.clear();
    }

    @Test
    void registerCreatesDefaultTradingAccountThroughSynchronousEventAndPublishesLeaderboardEventAfterCommit() {
        String suffix = Long.toString(System.nanoTime());

        MemberProfileResult result = registerMemberService.register(
                "signup-" + suffix,
                "hello@1234",
                "Signup Member",
                "Signup Member",
                "signup-" + suffix + "@coinzzickmock.dev",
                "010-1111-2222"
        );

        TradingAccount account = accountRepository.findByMemberId(result.memberId()).orElseThrow();
        assertThat(account.walletBalance()).isEqualTo(TradingAccount.INITIAL_WALLET_BALANCE);
        assertThat(account.availableMargin()).isEqualTo(TradingAccount.INITIAL_WALLET_BALANCE);
        assertThat(accountRefillStateRepository.findByMemberIdAndRefillDate(
                result.memberId(),
                accountRefillDatePolicy.currentRefillDate()
        ))
                .get()
                .extracting(state -> state.remainingCount())
                .isEqualTo(1);
        assertThat(capturedWalletEvents.openedMemberIds()).contains(result.memberId());
        assertThat(capturedWalletEvents.balanceChangedMemberIds()).doesNotContain(result.memberId());
    }

    @TestConfiguration
    static class WalletEventCaptureConfiguration {
        @Bean
        CapturedWalletEvents capturedWalletEvents() {
            return new CapturedWalletEvents();
        }
    }

    static class CapturedWalletEvents {
        private final List<Long> openedMemberIds = new ArrayList<>();
        private final List<Long> balanceChangedMemberIds = new ArrayList<>();

        @EventListener
        void on(TradingAccountOpenedEvent event) {
            openedMemberIds.add(event.memberId());
        }

        @EventListener
        void on(WalletBalanceChangedEvent event) {
            balanceChangedMemberIds.add(event.memberId());
        }

        void clear() {
            openedMemberIds.clear();
            balanceChangedMemberIds.clear();
        }

        List<Long> openedMemberIds() {
            return openedMemberIds;
        }

        List<Long> balanceChangedMemberIds() {
            return balanceChangedMemberIds;
        }
    }
}
