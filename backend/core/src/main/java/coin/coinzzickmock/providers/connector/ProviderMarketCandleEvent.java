package coin.coinzzickmock.providers.connector;

import java.math.BigDecimal;
import java.time.Instant;

public record ProviderMarketCandleEvent(
        String symbol,
        ProviderMarketCandleInterval interval,
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
) implements ProviderMarketRealtimeEvent {
}
