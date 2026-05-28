package coin.coinzzickmock.feature.push.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coin.push.server")
public record PushServerProperties(
        boolean enabled,
        String consumerName,
        String marketStreamKey,
        String tradingStreamKey,
        String marketGroup,
        String tradingGroup,
        long pollFixedDelayMs,
        int batchSize,
        Duration defaultMarketMaxAge,
        Duration defaultTradingMaxAge,
        long sseTimeoutMs,
        int maxSubscribersPerKey,
        int maxTotalSubscribers
) {
    public PushServerProperties {
        if (consumerName == null || consumerName.isBlank()) {
            consumerName = "local-push-app";
        }
        if (marketStreamKey == null || marketStreamKey.isBlank()) {
            marketStreamKey = "coin:push:market:v1";
        }
        if (tradingStreamKey == null || tradingStreamKey.isBlank()) {
            tradingStreamKey = "coin:push:trading:v1";
        }
        if (marketGroup == null || marketGroup.isBlank()) {
            marketGroup = "push-server-market-v1";
        }
        if (tradingGroup == null || tradingGroup.isBlank()) {
            tradingGroup = "push-server-trading-v1";
        }
        if (pollFixedDelayMs <= 0) {
            pollFixedDelayMs = 100;
        }
        if (batchSize <= 0) {
            batchSize = 50;
        }
        if (defaultMarketMaxAge == null || defaultMarketMaxAge.isNegative() || defaultMarketMaxAge.isZero()) {
            defaultMarketMaxAge = Duration.ofSeconds(15);
        }
        if (defaultTradingMaxAge == null || defaultTradingMaxAge.isNegative() || defaultTradingMaxAge.isZero()) {
            defaultTradingMaxAge = Duration.ofSeconds(5);
        }
        if (sseTimeoutMs <= 0) {
            sseTimeoutMs = 300_000;
        }
        if (maxSubscribersPerKey <= 0) {
            maxSubscribersPerKey = 100;
        }
        if (maxTotalSubscribers <= 0) {
            maxTotalSubscribers = 1000;
        }
    }
}
