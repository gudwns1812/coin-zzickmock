package coin.coinzzickmock.common.web;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
public class SseDeliveryConfiguration {
    @Bean("sseDeliveryTaskExecutor")
    Executor sseDeliveryTaskExecutor(
            @Value("${coin.sse.delivery.executor.core-pool-size:4}") int corePoolSize,
            @Value("${coin.sse.delivery.executor.max-pool-size:16}") int maxPoolSize,
            @Value("${coin.sse.delivery.executor.queue-capacity:1000}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("market-sse-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setRejectedExecutionHandler(new LoggingAbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    static final class LoggingAbortPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
            log.warn(
                    "SSE delivery executor rejected task. activeCount={} poolSize={} queueSize={}",
                    executor.getActiveCount(),
                    executor.getPoolSize(),
                    executor.getQueue().size()
            );
            throw new RejectedExecutionException("SSE delivery executor rejected task");
        }
    }
}
