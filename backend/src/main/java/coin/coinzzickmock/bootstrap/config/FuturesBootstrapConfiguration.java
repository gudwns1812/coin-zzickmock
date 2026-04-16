package coin.coinzzickmock.bootstrap.config;

import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class FuturesBootstrapConfiguration {
    private static final String DEMO_MEMBER_ID = "demo-member";
    private static final String DEMO_MEMBER_NAME = "Demo Trader";

    @Bean
    RestClient bitgetRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.bitget.com")
                .build();
    }

    @Bean
    ApplicationRunner demoAccountInitializer(
            AccountRepository accountRepository,
            RewardPointRepository rewardPointRepository
    ) {
        return args -> {
            accountRepository.findByMemberId(DEMO_MEMBER_ID)
                    .orElseGet(() -> accountRepository.save(
                            new TradingAccount(DEMO_MEMBER_ID, DEMO_MEMBER_NAME, 100000, 100000)
                    ));
            rewardPointRepository.findByMemberId(DEMO_MEMBER_ID)
                    .orElseGet(() -> rewardPointRepository.save(new RewardPointWallet(DEMO_MEMBER_ID, 0)));
        };
    }
}
