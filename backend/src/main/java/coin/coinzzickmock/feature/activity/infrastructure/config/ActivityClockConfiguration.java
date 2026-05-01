package coin.coinzzickmock.feature.activity.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActivityClockConfiguration {
    @Bean
    Clock activityClock() {
        return Clock.systemUTC();
    }
}
