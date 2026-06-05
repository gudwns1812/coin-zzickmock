package coin.coinzzickmock.feature.market.candle.application.dto;

import java.time.Instant;

public record GetMarketCandlesQuery(
        String symbol,
        String interval,
        Integer limit,
        Instant beforeOpenTime
) {
}
