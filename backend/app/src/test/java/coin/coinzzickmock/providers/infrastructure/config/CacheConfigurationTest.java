package coin.coinzzickmock.providers.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.application.history.MarketHistoricalCandlePage;
import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

class CacheConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CoinCacheConfiguration.class)
            .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class, RedisAutoConfiguration.class))
            .withPropertyValues(
                    "coin.cache.redis.enabled=true",
                    "spring.data.redis.host=127.0.0.1",
                    "spring.data.redis.port=6379"
            );

    @Test
    void createsDistributedCacheManagerWhenRedisCacheIsEnabled() {
        contextRunner.run(context -> assertThat(context.containsBean("distributedCacheManager")).isTrue());
    }

    @Test
    void configuresHistoricalCandleDistributedCache() {
        contextRunner.withPropertyValues("coin.cache.redis.historical-candle-ttl=30m")
                .run(context -> {
                    RedisCacheManager cacheManager = context.getBean("distributedCacheManager",
                            RedisCacheManager.class);

                    assertThat(cacheManager.getCache(
                            CoinCacheNames.MARKET_HISTORICAL_CANDLES_DISTRIBUTED_CACHE
                    )).isNotNull();
                });
    }

    @Test
    void redisValueSerializerSupportsHistoricalCandleInstants() {
        GenericJackson2JsonRedisSerializer serializer = CoinCacheConfiguration.redisValueSerializer();
        MarketHistoricalCandlePage page = new MarketHistoricalCandlePage(List.of(new MarketCandleResult(
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-03-01T00:01:00Z"),
                100,
                101,
                99,
                100.5,
                10
        )));

        Object deserialized = serializer.deserialize(serializer.serialize(page));

        assertThat(deserialized).isEqualTo(page);
    }

    @Test
    void historicalCandleTtlDefaultsToThirtyMinutes() {
        CoinCacheProperties properties = new CoinCacheProperties();

        assertThat(properties.getRedis().getHistoricalCandleTtl()).isEqualTo(Duration.ofMinutes(30));
    }
}
