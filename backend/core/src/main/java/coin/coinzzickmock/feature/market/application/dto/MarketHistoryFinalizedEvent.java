package coin.coinzzickmock.feature.market.application.dto;

import java.time.Instant;

public record MarketHistoryFinalizedEvent(
        String symbol,
        Instant openTime,
        Instant closeTime
) {
}
