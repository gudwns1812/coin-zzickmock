package coin.coinzzickmock.feature.order.infrastructure.config;

import coin.coinzzickmock.feature.order.domain.OrderPreviewPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderPolicyConfiguration {
    @Bean
    OrderPreviewPolicy orderPreviewPolicy() {
        return new OrderPreviewPolicy();
    }
}
