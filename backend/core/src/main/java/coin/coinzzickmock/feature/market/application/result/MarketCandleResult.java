package coin.coinzzickmock.feature.market.application.result;

import java.time.Instant;

public record MarketCandleResult(
        Instant openTime,
        Instant closeTime,
        double openPrice,
        double highPrice,
        double lowPrice,
        double closePrice,
        double volume
) {
}
