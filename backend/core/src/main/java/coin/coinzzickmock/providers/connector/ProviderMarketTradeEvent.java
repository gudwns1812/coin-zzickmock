package coin.coinzzickmock.providers.connector;

import java.math.BigDecimal;
import java.time.Instant;

public record ProviderMarketTradeEvent(
        String symbol,
        String tradeId,
        BigDecimal price,
        BigDecimal size,
        String side,
        Instant sourceEventTime,
        Instant receivedAt
) implements ProviderMarketRealtimeEvent {
}
