package coin.coinzzickmock.feature.push.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PushPublicationProperties.class)
class PushPublisherConfiguration {
}
