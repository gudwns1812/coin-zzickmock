package coin.coinzzickmock.providers.infrastructure.config;

import coin.coinzzickmock.common.cache.CoinCacheNames;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableConfigurationProperties(CoinCacheProperties.class)
public class CoinCacheConfiguration {
    @Bean("localCacheManager")
    @Primary
    CacheManager localCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                CoinCacheNames.MARKET_SNAPSHOT_LOCAL_CACHE,
                CoinCacheNames.MARKET_SUPPORTED_SYMBOLS_LOCAL_CACHE
        );
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

    @Bean("distributedCacheManager")
    @ConditionalOnProperty(prefix = "coin.cache.redis", name = "enabled", havingValue = "true")
    CacheManager distributedCacheManager(
            RedisConnectionFactory redisConnectionFactory,
            CoinCacheProperties cacheProperties
    ) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(cacheProperties.getRedis().getDefaultTtl())
                .computePrefixWith(cacheName -> cacheProperties.getRedis().getKeyPrefix() + cacheName + "::")
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        redisValueSerializer()
                ));

        RedisCacheConfiguration historicalCandles = defaults.entryTtl(
                cacheProperties.getRedis().getHistoricalCandleTtl()
        );

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(
                        CoinCacheNames.MARKET_HISTORICAL_CANDLES_DISTRIBUTED_CACHE,
                        historicalCandles
                ))
                .build();
    }

    static GenericJackson2JsonRedisSerializer redisValueSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        return GenericJackson2JsonRedisSerializer.builder()
                .objectMapper(objectMapper)
                .defaultTyping(true)
                .build();
    }
}
