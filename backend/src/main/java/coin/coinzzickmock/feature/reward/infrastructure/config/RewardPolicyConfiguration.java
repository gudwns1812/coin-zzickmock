package coin.coinzzickmock.feature.reward.infrastructure.config;

import coin.coinzzickmock.feature.reward.domain.RewardPointPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RewardPolicyConfiguration {
    @Bean
    RewardPointPolicy rewardPointPolicy() {
        return new RewardPointPolicy();
    }
}
