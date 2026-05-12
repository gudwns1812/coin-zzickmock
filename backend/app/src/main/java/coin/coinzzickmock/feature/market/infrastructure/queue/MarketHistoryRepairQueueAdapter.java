package coin.coinzzickmock.feature.market.infrastructure.queue;

import coin.coinzzickmock.feature.market.application.repair.MarketHistoryRepairQueue;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketHistoryRepairQueueAdapter implements MarketHistoryRepairQueue {
    private final StringRedisTemplate redisTemplate;

    @Value("${coin.market.history-repair.queue-key:coin:market:history-repair:events}")
    private String queueKey;

    @Override
    public void push(long eventId) {
        redisTemplate.opsForList().leftPush(queueKey, Long.toString(eventId));
    }

    @Override
    public Optional<Long> pop(Duration timeout) {
        String value = redisTemplate.opsForList().rightPop(queueKey, timeout);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(value));
    }
}
