package coin.coinzzickmock.feature.market.infrastructure.queue;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.market.application.repair.MarketHistoryRepairQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = CoinZzickmockApplication.class,
        properties = {
                "coin.market.history-repair.worker.enabled=false",
                "coin.market.history-repair.queue-key=coin:market:history-repair:test:autopilot"
        }
)
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "coin.redis.integration", matches = "true")
class MarketHistoryRepairQueueAdapterRedisIntegrationTest {
    static final String QUEUE_KEY = "coin:market:history-repair:test:autopilot";
    static final long EVENT_ID = 2026050701L;

    @Autowired
    private MarketHistoryRepairQueue marketHistoryRepairQueue;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate.delete(QUEUE_KEY);
    }

    @Test
    void pushesRepairEventIdToRealRedisList() {
        marketHistoryRepairQueue.push(EVENT_ID);

        assertThat(redisTemplate.opsForList().range(QUEUE_KEY, 0, -1))
                .containsExactly(Long.toString(EVENT_ID));
    }
}
