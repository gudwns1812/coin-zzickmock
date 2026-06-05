package coin.coinzzickmock.feature.market.history.application.dto;

import java.time.Instant;

public record MarketHistoryFinalizedEvent(
        String symbol,
        Instant openTime,
        Instant closeTime
) {
}
