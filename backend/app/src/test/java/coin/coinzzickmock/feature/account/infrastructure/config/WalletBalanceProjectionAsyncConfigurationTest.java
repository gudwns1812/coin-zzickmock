package coin.coinzzickmock.feature.account.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class WalletBalanceProjectionAsyncConfigurationTest {
    @Test
    void configuresWalletBalanceProjectionExecutorForT4gSmallRuntime() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new WalletBalanceProjectionAsyncConfiguration()
                .walletBalanceProjectionExecutor(2, 8, 1000);

        assertEquals(2, executor.getCorePoolSize());
        assertEquals(8, executor.getMaxPoolSize());
        assertEquals(1000, executor.getQueueCapacity());
        assertEquals("wallet-balance-projection-", executor.getThreadNamePrefix());
        assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);

        executor.shutdown();
    }
}
