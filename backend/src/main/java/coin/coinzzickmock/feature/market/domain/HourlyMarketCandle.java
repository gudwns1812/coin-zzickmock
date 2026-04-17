package coin.coinzzickmock.feature.market.domain;

import java.time.Instant;

public record HourlyMarketCandle(
        long symbolId,
        Instant openTime,
        Instant closeTime,
        double openPrice,
        double highPrice,
        double lowPrice,
        double closePrice,
        double volume,
        double quoteVolume,
        int tradeCount,
        Instant sourceMinuteOpenTime,
        Instant sourceMinuteCloseTime
) {
}
