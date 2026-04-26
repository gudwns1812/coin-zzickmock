package coin.coinzzickmock.feature.leaderboard.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LeaderboardProperties.class)
public class LeaderboardConfiguration {
}
