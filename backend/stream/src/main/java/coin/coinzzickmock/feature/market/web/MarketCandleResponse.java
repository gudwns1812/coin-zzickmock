package coin.coinzzickmock.feature.market.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record MarketCandleResponse(
        Instant openTime,
        Instant closeTime,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        BigDecimal volume
) {
    public MarketCandleResponse {
        Objects.requireNonNull(openTime, "openTime must not be null");
        Objects.requireNonNull(closeTime, "closeTime must not be null");
        Objects.requireNonNull(openPrice, "openPrice must not be null");
        Objects.requireNonNull(highPrice, "highPrice must not be null");
        Objects.requireNonNull(lowPrice, "lowPrice must not be null");
        Objects.requireNonNull(closePrice, "closePrice must not be null");
        Objects.requireNonNull(volume, "volume must not be null");
        if (openTime.isAfter(closeTime)) {
            throw new IllegalArgumentException("openTime must not be after closeTime");
        }
        if (isNegative(openPrice) || isNegative(highPrice) || isNegative(lowPrice) || isNegative(closePrice)) {
            throw new IllegalArgumentException("price fields must be non-negative");
        }
        if (isNegative(volume)) {
            throw new IllegalArgumentException("volume must be non-negative");
        }
        if (highPrice.compareTo(lowPrice) < 0) {
            throw new IllegalArgumentException("highPrice must be greater than or equal to lowPrice");
        }
        if (highPrice.compareTo(openPrice) < 0 || highPrice.compareTo(closePrice) < 0) {
            throw new IllegalArgumentException("highPrice must be greater than or equal to openPrice and closePrice");
        }
        if (lowPrice.compareTo(openPrice) > 0 || lowPrice.compareTo(closePrice) > 0) {
            throw new IllegalArgumentException("lowPrice must be less than or equal to openPrice and closePrice");
        }
    }

    public MarketCandleResponse(
            Instant openTime,
            Instant closeTime,
            double openPrice,
            double highPrice,
            double lowPrice,
            double closePrice,
            double volume
    ) {
        this(
                openTime,
                closeTime,
                BigDecimal.valueOf(openPrice),
                BigDecimal.valueOf(highPrice),
                BigDecimal.valueOf(lowPrice),
                BigDecimal.valueOf(closePrice),
                BigDecimal.valueOf(volume)
        );
    }

    private static boolean isNegative(BigDecimal value) {
        return value.signum() < 0;
    }
}
