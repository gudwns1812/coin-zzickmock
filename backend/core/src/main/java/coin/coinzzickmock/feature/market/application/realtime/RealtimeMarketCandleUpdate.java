package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record RealtimeMarketCandleUpdate(
        String symbol,
        MarketCandleInterval interval,
        Instant openTime,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        BigDecimal baseVolume,
        BigDecimal quoteVolume,
        BigDecimal usdtVolume,
        Instant sourceEventTime,
        Instant receivedAt
) {
    public RealtimeMarketCandleUpdate {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        Objects.requireNonNull(interval, "interval must not be null");
        Objects.requireNonNull(openTime, "openTime must not be null");
        Objects.requireNonNull(openPrice, "openPrice must not be null");
        Objects.requireNonNull(highPrice, "highPrice must not be null");
        Objects.requireNonNull(lowPrice, "lowPrice must not be null");
        Objects.requireNonNull(closePrice, "closePrice must not be null");
        Objects.requireNonNull(baseVolume, "baseVolume must not be null");
        Objects.requireNonNull(quoteVolume, "quoteVolume must not be null");
        Objects.requireNonNull(usdtVolume, "usdtVolume must not be null");
        Objects.requireNonNull(sourceEventTime, "sourceEventTime must not be null");
        Objects.requireNonNull(receivedAt, "receivedAt must not be null");
    }
}
