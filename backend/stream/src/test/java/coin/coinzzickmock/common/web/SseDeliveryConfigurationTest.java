package coin.coinzzickmock.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class SseDeliveryConfigurationTest {
    @Test
    void configuresSseEventExecutorForBoundedAsyncFanout() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new SseDeliveryConfiguration()
                .sseDeliveryTaskExecutor(4, 16, 1000);
        executor.initialize();

        assertEquals(4, executor.getCorePoolSize());
        assertEquals(16, executor.getMaxPoolSize());
        assertEquals(1000, executor.getQueueCapacity());
        assertEquals("market-sse-", executor.getThreadNamePrefix());
        assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                .isInstanceOf(SseDeliveryConfiguration.LoggingAbortPolicy.class);

        executor.shutdown();
    }
}
