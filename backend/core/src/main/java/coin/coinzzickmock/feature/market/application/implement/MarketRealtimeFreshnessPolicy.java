package coin.coinzzickmock.feature.market.application.implement;

import coin.coinzzickmock.feature.market.application.dto.MarketRealtimeSourceSnapshot;
import coin.coinzzickmock.feature.market.application.dto.MarketRealtimeSourceType;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record MarketRealtimeFreshnessPolicy(
        Duration maxAge,
        boolean allowRestRecovery
) {
    public MarketRealtimeFreshnessPolicy {
        Objects.requireNonNull(maxAge, "maxAge must not be null");
        if (maxAge.isNegative() || maxAge.isZero()) {
            throw new IllegalArgumentException("maxAge must be positive");
        }
    }

    public boolean accepts(MarketRealtimeSourceSnapshot snapshot, Instant now) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        if (!snapshot.isFresh(now, maxAge)) {
            return false;
        }
        if (snapshot.isWebSocketSource()) {
            return true;
        }
        return allowRestRecovery && snapshot.sourceType() == MarketRealtimeSourceType.REST_RECOVERY;
    }
}
