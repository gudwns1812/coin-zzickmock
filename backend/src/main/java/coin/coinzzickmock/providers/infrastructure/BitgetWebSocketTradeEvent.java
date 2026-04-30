package coin.coinzzickmock.providers.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;

public record BitgetWebSocketTradeEvent(
        String symbol,
        String tradeId,
        BigDecimal price,
        BigDecimal size,
        String side,
        Instant sourceEventTime,
        Instant receivedAt
) implements BitgetWebSocketMarketEvent {
}
