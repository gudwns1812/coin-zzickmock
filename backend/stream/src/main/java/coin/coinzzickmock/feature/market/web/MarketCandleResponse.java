package coin.coinzzickmock.feature.market.web;

import java.time.Instant;

public record MarketCandleResponse(
        Instant openTime,
        Instant closeTime,
        double openPrice,
        double highPrice,
        double lowPrice,
        double closePrice,
        double volume
) {
}
