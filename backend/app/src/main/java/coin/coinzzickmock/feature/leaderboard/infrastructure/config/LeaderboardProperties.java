package coin.coinzzickmock.feature.leaderboard.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coin.leaderboard")
public class LeaderboardProperties {
    private final Redis redis = new Redis();

    public Redis getRedis() {
        return redis;
    }

    public static class Redis {
        private boolean enabled = false;
        private String keyPrefix = "coin:leaderboard:";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }
}
