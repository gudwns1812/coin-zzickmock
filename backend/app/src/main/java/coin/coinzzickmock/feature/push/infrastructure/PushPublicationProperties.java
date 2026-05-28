package coin.coinzzickmock.feature.push.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coin.push.publisher")
public record PushPublicationProperties(
        String marketStreamKey,
        String tradingStreamKey,
        long maxLen,
        Duration marketMaxAge,
        Duration tradingMaxAge
) {
    public PushPublicationProperties {
        if (marketStreamKey == null || marketStreamKey.isBlank()) {
            marketStreamKey = "coin:push:market:v1";
        }
        if (tradingStreamKey == null || tradingStreamKey.isBlank()) {
            tradingStreamKey = "coin:push:trading:v1";
        }
        if (maxLen <= 0) {
            maxLen = 10_000;
        }
        if (marketMaxAge == null || marketMaxAge.isNegative() || marketMaxAge.isZero()) {
            marketMaxAge = Duration.ofSeconds(15);
        }
        if (tradingMaxAge == null || tradingMaxAge.isNegative() || tradingMaxAge.isZero()) {
            tradingMaxAge = Duration.ofSeconds(5);
        }
    }

    public String streamKey(coin.coinzzickmock.feature.push.application.dto.PushStream stream) {
        return switch (stream) {
            case MARKET -> marketStreamKey;
            case TRADING -> tradingStreamKey;
        };
    }
}
