package coin.coinzzickmock.feature.market.web;

import java.util.Locale;

public record CandleSubscription(String symbol, String interval) {
    public CandleSubscription {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        if (interval == null || interval.isBlank()) {
            throw new IllegalArgumentException("interval is required");
        }
        symbol = symbol.toUpperCase(Locale.ROOT);
        interval = interval.trim();
    }
}
