package coin.coinzzickmock.feature.market.infrastructure.cache;

import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowCache;
import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowCacheRead;
import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowKey;
import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowPage;
import coin.coinzzickmock.providers.infrastructure.config.CoinCacheProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class MarketLatestCandleWindowCacheStore implements MarketLatestCandleWindowCache {
    private static final String KEY_PREFIX = "marketLatestCandleWindow::";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CoinCacheProperties cacheProperties;

    @Override
    public MarketLatestCandleWindowCacheRead read(MarketLatestCandleWindowKey key) {
        try {
            String payload = redisTemplate.opsForValue().get(redisKey(key));
            if (payload == null) {
                return MarketLatestCandleWindowCacheRead.miss();
            }
            return MarketLatestCandleWindowCacheRead.hit(objectMapper.readValue(payload, MarketLatestCandleWindowPage.class));
        } catch (Exception exception) {
            log.warn("Failed to read latest candle window cache. cache=market_latest_candles symbol={} interval={} limit={}",
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
        } catch (Exception exception) {
            log.warn("Failed to write latest candle window cache. cache=market_latest_candles symbol={} interval={} limit={}",
                    key.symbol(), key.interval().value(), key.limit(), exception);
            return false;
        }
    }

    private String redisKey(MarketLatestCandleWindowKey key) {
        return cacheProperties.getRedis().getKeyPrefix() + KEY_PREFIX + key.cacheKey();
    }
}
