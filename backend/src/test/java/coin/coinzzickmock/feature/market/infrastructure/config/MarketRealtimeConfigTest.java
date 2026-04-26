package coin.coinzzickmock.feature.market.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class MarketRealtimeConfigTest {
    @Test
    void configuresSchedulerPoolSoHistoryRetryDoesNotBlockTickerRefresh() {
        ThreadPoolTaskScheduler scheduler = new MarketRealtimeConfig().marketTaskScheduler();

        assertEquals(4, scheduler.getScheduledThreadPoolExecutor().getCorePoolSize());
        assertEquals("market-scheduler-", scheduler.getThreadNamePrefix());
    }
}
