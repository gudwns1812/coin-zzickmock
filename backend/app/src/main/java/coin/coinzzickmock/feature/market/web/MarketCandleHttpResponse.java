package coin.coinzzickmock.feature.market.web;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketCandleHttpResponse(
        Instant openTime,
        Instant closeTime,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        BigDecimal volume
) {
}
