package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.util.Objects;

public record CandleSubscription(String symbol, MarketCandleInterval interval) {
    public CandleSubscription {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        Objects.requireNonNull(interval, "interval must not be null");
    }
}
