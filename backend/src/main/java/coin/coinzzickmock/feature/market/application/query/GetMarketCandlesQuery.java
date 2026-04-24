package coin.coinzzickmock.feature.market.application.query;

import java.time.Instant;

public record GetMarketCandlesQuery(
        String symbol,
        String interval,
        Integer limit,
        Instant beforeOpenTime
) {
}
