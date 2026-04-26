package coin.coinzzickmock.feature.market.application.history;

import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.providers.infrastructure.config.CoinCacheNames;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MarketHistoricalCandleSegmentStore {
    private final MarketHistoricalCandleTelemetry telemetry;
    private final Cache cache;

    public MarketHistoricalCandleSegmentStore(
            MarketHistoricalCandleTelemetry telemetry,
            @Qualifier("distributedCacheManager") ObjectProvider<CacheManager> distributedCacheManagerProvider
    ) {
        this.telemetry = telemetry;
        CacheManager cacheManager = distributedCacheManagerProvider.getIfAvailable();
        this.cache = cacheManager == null
                ? null
                : cacheManager.getCache(CoinCacheNames.MARKET_HISTORICAL_CANDLES_DISTRIBUTED_CACHE);
    }

    public List<MarketCandleResult> read(MarketHistoricalCandleSegment segment) {
        if (cache == null) {
            telemetry.record("market.history.redis.miss", segment, "redis", "unavailable");
            return null;
        }

        try {
            Cache.ValueWrapper wrapper = cache.get(segment.cacheKey());
            if (wrapper == null) {
                telemetry.record("market.history.redis.miss", segment, "redis", "miss");
                return null;
            }
            Object value = wrapper.get();
            if (value instanceof MarketHistoricalCandlePage page) {
                telemetry.record("market.history.redis.hit", segment, "redis", "hit");
                return page.candles();
            }

            telemetry.record("market.history.redis.miss", segment, "redis", "unexpected_value");
            return null;
        } catch (RuntimeException exception) {
            telemetry.record("market.history.redis.miss", segment, "redis", "unavailable");
            log.warn("Failed to read historical candle segment cache. key={}", segment.cacheKey(), exception);
            return null;
        }
    }

    public void write(MarketHistoricalCandleSegment segment, List<MarketCandleResult> candles) {
        if (cache == null || candles.isEmpty()) {
            return;
        }

        try {
            cache.put(segment.cacheKey(), new MarketHistoricalCandlePage(candles));
        } catch (RuntimeException exception) {
            telemetry.record("market.history.redis.miss", segment, "redis", "write_unavailable");
            log.warn("Failed to write historical candle segment cache. key={}", segment.cacheKey(), exception);
        }
    }
}
