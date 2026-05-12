package coin.coinzzickmock.providers.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;

public record BitgetWebSocketTickerEvent(
        String symbol,
        BigDecimal lastPrice,
        BigDecimal markPrice,
        BigDecimal indexPrice,
        BigDecimal fundingRate,
        Instant nextFundingTime,
        Instant sourceEventTime,
        Instant receivedAt
) implements BitgetWebSocketMarketEvent {
}
