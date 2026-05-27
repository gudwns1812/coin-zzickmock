package coin.coinzzickmock.feature.activity.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class ActivityAsyncConfigurationTest {
    @Test
    void configuresActivityRecordExecutorForT4gSmallRuntime() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new ActivityAsyncConfiguration()
                .activityRecordExecutor(2, 8, 1000);

        assertEquals(2, executor.getCorePoolSize());
        assertEquals(8, executor.getMaxPoolSize());
        assertEquals(1000, executor.getQueueCapacity());
        assertEquals("activity-record-", executor.getThreadNamePrefix());
        assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                .isInstanceOf(ThreadPoolExecutor.AbortPolicy.class);

        executor.shutdown();
    }
}
