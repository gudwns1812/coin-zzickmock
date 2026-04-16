package coin.coinzzickmock.feature.account.infrastructure.config;

import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccountDemoSeedConfiguration {
    private static final String DEMO_MEMBER_ID = "demo-member";
    private static final String DEMO_MEMBER_NAME = "Demo Trader";

    @Bean
    ApplicationRunner demoAccountInitializer(AccountRepository accountRepository) {
        return args -> accountRepository.findByMemberId(DEMO_MEMBER_ID)
                .orElseGet(() -> accountRepository.save(
                        new TradingAccount(DEMO_MEMBER_ID, DEMO_MEMBER_NAME, 100000, 100000)
                ));
    }
}
