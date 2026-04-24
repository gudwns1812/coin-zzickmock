package coin.coinzzickmock.feature.position.infrastructure.config;

import coin.coinzzickmock.feature.position.domain.LiquidationPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiquidationPolicyConfiguration {
    @Bean
    LiquidationPolicy liquidationPolicy() {
        return new LiquidationPolicy();
    }
}
