package coin.coinzzickmock.feature.market.job;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class MarketSchedulingConfiguration {
    @Bean(name = "taskScheduler")
    ThreadPoolTaskScheduler marketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("market-scheduler-");
        scheduler.setPoolSize(4);
        scheduler.initialize();
        return scheduler;
    }
}
