package coin.coinzzickmock.feature.push.infrastructure;

import coin.coinzzickmock.feature.push.application.PushServerProperties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PushServerProperties.class)
class PushServerConfiguration {
    @Bean(destroyMethod = "close")
    ExecutorService pushVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
