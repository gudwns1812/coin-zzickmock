package coin.coinzzickmock.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class SseDeliveryConfigurationTest {
    @Test
    void configuresSseEventExecutorForBoundedAsyncFanout() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new SseDeliveryConfiguration()
                .sseDeliveryTaskExecutor();

        assertEquals(2, executor.getCorePoolSize());
        assertEquals(8, executor.getMaxPoolSize());
        assertEquals(200, executor.getQueueCapacity());
        assertEquals("market-sse-", executor.getThreadNamePrefix());
        assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                .isInstanceOf(java.util.concurrent.ThreadPoolExecutor.AbortPolicy.class);
    }
}
