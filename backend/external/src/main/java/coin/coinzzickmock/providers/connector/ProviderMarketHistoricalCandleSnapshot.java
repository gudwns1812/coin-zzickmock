package coin.coinzzickmock.providers.connector;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record ProviderMarketHistoricalCandleSnapshot(
        Instant openTime,
        Instant closeTime,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        BigDecimal volume,
        BigDecimal quoteVolume
) {
    public ProviderMarketHistoricalCandleSnapshot {
        Objects.requireNonNull(openTime, "openTime must not be null");
        Objects.requireNonNull(closeTime, "closeTime must not be null");
        Objects.requireNonNull(openPrice, "openPrice must not be null");
        Objects.requireNonNull(highPrice, "highPrice must not be null");
        Objects.requireNonNull(lowPrice, "lowPrice must not be null");
        Objects.requireNonNull(closePrice, "closePrice must not be null");
        Objects.requireNonNull(volume, "volume must not be null");
        Objects.requireNonNull(quoteVolume, "quoteVolume must not be null");
    }
}
