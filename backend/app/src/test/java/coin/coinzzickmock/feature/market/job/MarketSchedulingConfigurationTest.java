package coin.coinzzickmock.feature.market.job;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class MarketSchedulingConfigurationTest {
    @Test
    void configuresSchedulerPoolSoHistoryRetryDoesNotBlockTickerRefresh() {
        ThreadPoolTaskScheduler scheduler = new MarketSchedulingConfiguration().marketTaskScheduler();

        assertEquals(4, scheduler.getScheduledThreadPoolExecutor().getCorePoolSize());
        assertEquals("market-scheduler-", scheduler.getThreadNamePrefix());
    }
}
