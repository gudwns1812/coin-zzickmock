package coin.coinzzickmock.providers.connector;

import java.math.BigDecimal;
import java.util.Objects;

public record ProviderMarketSnapshot(
        String symbol,
        String displayName,
        BigDecimal lastPrice,
        BigDecimal markPrice,
        BigDecimal indexPrice,
        BigDecimal fundingRate,
        BigDecimal change24h,
        BigDecimal turnover24hUsdt
) {
    public ProviderMarketSnapshot {
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(lastPrice, "lastPrice must not be null");
        Objects.requireNonNull(markPrice, "markPrice must not be null");
        Objects.requireNonNull(indexPrice, "indexPrice must not be null");
        Objects.requireNonNull(fundingRate, "fundingRate must not be null");
        Objects.requireNonNull(change24h, "change24h must not be null");
        Objects.requireNonNull(turnover24hUsdt, "turnover24hUsdt must not be null");
    }
}
