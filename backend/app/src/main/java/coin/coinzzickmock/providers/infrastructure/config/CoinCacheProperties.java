package coin.coinzzickmock.providers.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coin.cache")
public class CoinCacheProperties {
    private final Redis redis = new Redis();

    public Redis getRedis() {
        return redis;
    }

    public static class Redis {
        private boolean enabled = false;
        private Duration defaultTtl = Duration.ofMinutes(5);
        private Duration historicalCandleTtl = Duration.ofMinutes(30);
        private String keyPrefix = "coinzzickmock::";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getDefaultTtl() {
            return defaultTtl;
        }

        public void setDefaultTtl(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
        }

        public Duration getHistoricalCandleTtl() {
            return historicalCandleTtl;
        }

        public void setHistoricalCandleTtl(Duration historicalCandleTtl) {
            this.historicalCandleTtl = historicalCandleTtl;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }
}
