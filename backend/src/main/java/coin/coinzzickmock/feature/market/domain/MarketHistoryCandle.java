package coin.coinzzickmock.feature.market.domain;

import java.time.Instant;

public record MarketHistoryCandle(
        long symbolId,
        Instant openTime,
        Instant closeTime,
        double openPrice,
        double highPrice,
        double lowPrice,
        double closePrice,
        double volume,
        double quoteVolume
) {
    public static MarketHistoryCandle first(long symbolId, Instant openTime, Instant closeTime, double price) {
        return new MarketHistoryCandle(
                symbolId,
                openTime,
                closeTime,
                price,
                price,
                price,
                price,
                0.0,
                0.0
        );
    }

    public MarketHistoryCandle mergeLatestPrice(double latestPrice) {
        return new MarketHistoryCandle(
                symbolId,
                openTime,
                closeTime,
                openPrice,
                Math.max(highPrice, latestPrice),
                Math.min(lowPrice, latestPrice),
                latestPrice,
                volume,
                quoteVolume
        );
    }
}
