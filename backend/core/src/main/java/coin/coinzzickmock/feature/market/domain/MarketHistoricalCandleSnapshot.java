package coin.coinzzickmock.feature.market.domain;

import java.time.Instant;

public record MarketHistoricalCandleSnapshot(
        Instant openTime,
        Instant closeTime,
        double openPrice,
        double highPrice,
        double lowPrice,
        double closePrice,
        double volume,
        double quoteVolume
) {
}
