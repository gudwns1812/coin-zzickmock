package coin.coinzzickmock.feature.market.application.query;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Instant;

public record GetMarketCandlesQuery(
        String symbol,
        MarketCandleInterval interval,
        int limit,
        Instant beforeOpenTime
) {
}
