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
        if (closeTime.isBefore(openTime)) {
            throw new IllegalArgumentException("closeTime must not be before openTime");
        }
        requireNonNegative(openPrice, "openPrice");
        requireNonNegative(highPrice, "highPrice");
        requireNonNegative(lowPrice, "lowPrice");
        requireNonNegative(closePrice, "closePrice");
        requireNonNegative(volume, "volume");
        requireNonNegative(quoteVolume, "quoteVolume");
        if (highPrice.compareTo(lowPrice) < 0) {
            throw new IllegalArgumentException("highPrice must be greater than or equal to lowPrice");
        }
        requireWithinRange(openPrice, lowPrice, highPrice, "openPrice");
        requireWithinRange(closePrice, lowPrice, highPrice, "closePrice");
    }

    private static void requireNonNegative(BigDecimal value, String fieldName) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
    }

    private static void requireWithinRange(
            BigDecimal value,
            BigDecimal lowInclusive,
            BigDecimal highInclusive,
            String fieldName
    ) {
        if (value.compareTo(lowInclusive) < 0 || value.compareTo(highInclusive) > 0) {
            throw new IllegalArgumentException(fieldName + " must be between lowPrice and highPrice");
        }
    }
}
