package coin.coinzzickmock.feature.market.application.history;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Instant;

public record MarketHistoricalCandleSegment(
        String symbol,
        MarketCandleInterval interval,
        String granularity,
        Instant startInclusive,
        Instant endExclusive,
        int size
) {
    String cacheKey() {
        return "market:historical-candles:%s:%s:%d:size%d".formatted(
                symbol,
                granularity,
                startInclusive.toEpochMilli(),
                size
        );
    }
}
