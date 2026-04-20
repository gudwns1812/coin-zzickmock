package coin.coinzzickmock.feature.market.domain;

import java.time.Instant;

public record MarketMinuteCandleSnapshot(
        Instant openTime,
        Instant closeTime,
        double openPrice,
        double highPrice,
        double lowPrice,
        double closePrice,
        double volume,
        double quoteVolume
) {
    public MarketHistoryCandle toHistoryCandle(long symbolId) {
        return new MarketHistoryCandle(
                symbolId,
                openTime,
                closeTime,
                openPrice,
                highPrice,
                lowPrice,
                closePrice,
                volume,
                quoteVolume
        );
    }
}
