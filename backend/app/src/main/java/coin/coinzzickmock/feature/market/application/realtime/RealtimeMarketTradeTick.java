package coin.coinzzickmock.feature.market.application.realtime;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record RealtimeMarketTradeTick(
        String symbol,
        String tradeId,
        BigDecimal price,
        BigDecimal size,
        String side,
        Instant sourceEventTime,
        Instant receivedAt
) {
    public RealtimeMarketTradeTick {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(sourceEventTime, "sourceEventTime must not be null");
        Objects.requireNonNull(receivedAt, "receivedAt must not be null");
    }
}
