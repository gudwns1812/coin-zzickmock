package coin.coinzzickmock.feature.activity.infrastructure.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ActivityAsyncConfiguration {
    @Bean("activityRecordExecutor")
    @Profile("!test")
    Executor activityRecordExecutor(
            @Value("${coin.activity.record.executor.core-pool-size:2}") int corePoolSize,
            @Value("${coin.activity.record.executor.max-pool-size:8}") int maxPoolSize,
            @Value("${coin.activity.record.executor.queue-capacity:1000}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("activity-record-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("activityRecordExecutor")
    @Profile("test")
    Executor testActivityRecordExecutor() {
        return new SyncTaskExecutor();
    }
}
