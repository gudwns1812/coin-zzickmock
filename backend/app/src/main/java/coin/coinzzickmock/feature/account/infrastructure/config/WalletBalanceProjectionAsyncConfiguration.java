package coin.coinzzickmock.feature.account.infrastructure.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class WalletBalanceProjectionAsyncConfiguration {
    @Bean("walletBalanceProjectionExecutor")
    @Profile("!test")
    Executor walletBalanceProjectionExecutor(
            @Value("${coin.account.wallet-balance-projection.executor.core-pool-size:2}") int corePoolSize,
            @Value("${coin.account.wallet-balance-projection.executor.max-pool-size:8}") int maxPoolSize,
            @Value("${coin.account.wallet-balance-projection.executor.queue-capacity:1000}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("wallet-balance-projection-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("walletBalanceProjectionExecutor")
    @Profile("test")
    Executor testWalletBalanceProjectionExecutor() {
        return new SyncTaskExecutor();
    }
}
