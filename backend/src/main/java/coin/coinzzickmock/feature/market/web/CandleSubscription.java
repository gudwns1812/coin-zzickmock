package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;

public record CandleSubscription(String symbol, MarketCandleInterval interval) {
    public CandleSubscription {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        if (interval == null) {
            throw new IllegalArgumentException("interval is required");
        }
        symbol = symbol.toUpperCase();
    }
}
