package coin.coinzzickmock.providers.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BitgetConnectorConfiguration {
    @Bean
    RestClient bitgetRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.bitget.com")
                .build();
    }
}
