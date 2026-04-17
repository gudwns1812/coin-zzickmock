package coin.coinzzickmock.providers.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

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
}
