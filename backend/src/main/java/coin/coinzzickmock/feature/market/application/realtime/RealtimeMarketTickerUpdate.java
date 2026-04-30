package coin.coinzzickmock.feature.market.application.realtime;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record RealtimeMarketTickerUpdate(
        String symbol,
        BigDecimal lastPrice,
        BigDecimal markPrice,
        BigDecimal indexPrice,
        BigDecimal fundingRate,
        Instant nextFundingTime,
        Instant sourceEventTime,
        Instant receivedAt
) {
    public RealtimeMarketTickerUpdate {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        Objects.requireNonNull(lastPrice, "lastPrice must not be null");
        Objects.requireNonNull(markPrice, "markPrice must not be null");
        Objects.requireNonNull(indexPrice, "indexPrice must not be null");
        Objects.requireNonNull(sourceEventTime, "sourceEventTime must not be null");
        Objects.requireNonNull(receivedAt, "receivedAt must not be null");
    }
}
