package coin.coinzzickmock.feature.market.infrastructure.cache;

import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowCache;
import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowCacheRead;
import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowKey;
import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "coin.cache.redis", name = "enabled", havingValue = "true")
class MarketLatestCandleWindowCacheStore implements MarketLatestCandleWindowCache {
    private static final String KEY_PREFIX = "marketLatestCandleWindow::";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;

    MarketLatestCandleWindowCacheStore(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${coin.cache.redis.key-prefix:coinzzickmock::}") String keyPrefix
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public MarketLatestCandleWindowCacheRead read(MarketLatestCandleWindowKey key) {
        try {
            String payload = redisTemplate.opsForValue().get(redisKey(key));
            if (payload == null) {
                return MarketLatestCandleWindowCacheRead.miss();
            }
            return MarketLatestCandleWindowCacheRead.hit(objectMapper.readValue(payload, MarketLatestCandleWindowPage.class));
        } catch (RuntimeException exception) {
            log.warn("Failed to read latest candle window cache. cache=market_latest_candles symbol={} interval={} limit={}",
                    key.symbol(), key.interval().value(), key.limit(), exception);
            return MarketLatestCandleWindowCacheRead.unavailable();
        } catch (Exception exception) {
            log.warn("Failed to deserialize latest candle window cache. cache=market_latest_candles symbol={} interval={} limit={}",
                    key.symbol(), key.interval().value(), key.limit(), exception);
            return MarketLatestCandleWindowCacheRead.unavailable();
        }
    }

    @Override
    public boolean write(MarketLatestCandleWindowKey key, MarketLatestCandleWindowPage page, Duration ttl) {
        if (page.candles().isEmpty() || ttl == null || ttl.isNegative() || ttl.isZero()) {
            return false;
        }
        try {
            redisTemplate.opsForValue().set(redisKey(key), objectMapper.writeValueAsString(page), ttl);
            return true;
        } catch (RuntimeException exception) {
            log.warn("Failed to write latest candle window cache. cache=market_latest_candles symbol={} interval={} limit={}",
                    key.symbol(), key.interval().value(), key.limit(), exception);
            return false;
        } catch (Exception exception) {
            log.warn("Failed to serialize latest candle window cache. cache=market_latest_candles symbol={} interval={} limit={}",
                    key.symbol(), key.interval().value(), key.limit(), exception);
            return false;
        }
    }

    private String redisKey(MarketLatestCandleWindowKey key) {
        return keyPrefix + KEY_PREFIX + key.cacheKey();
    }
}
