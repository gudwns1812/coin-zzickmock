package coin.coinzzickmock.feature.order.infrastructure.config;

import coin.coinzzickmock.feature.order.domain.OrderPlacementPolicy;
import coin.coinzzickmock.feature.order.domain.OrderPreviewPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderPolicyConfiguration {
    @Bean
    OrderPlacementPolicy orderPlacementPolicy() {
        return new OrderPlacementPolicy();
    }

    @Bean
    OrderPreviewPolicy orderPreviewPolicy(OrderPlacementPolicy orderPlacementPolicy) {
        return new OrderPreviewPolicy(orderPlacementPolicy);
    }
}
