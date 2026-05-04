package coin.coinzzickmock.feature.market.application.realtime;

import java.time.Instant;

public record MarketHistoryFinalizedEvent(
        String symbol,
        Instant openTime,
        Instant closeTime
) {
}
