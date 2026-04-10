package stock.stockzzickmock.storage.redis.market;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import stock.stockzzickmock.core.domain.market.MarketStatus;

@Component
public class MarketStatusRedisRepository {

    private final ValueOperations<String, Object> valueOperations;
    private final String statusKey;

    public MarketStatusRedisRepository(
            RedisTemplate<String, Object> redisTemplate,
            @Value("${redis.market-status.krx.key:market:krx:status}") String statusKey
    ) {
        this.valueOperations = redisTemplate.opsForValue();
        this.statusKey = statusKey;
    }

    public void setStatus(MarketStatus status) {
        valueOperations.set(statusKey, status.name());
    }
}
