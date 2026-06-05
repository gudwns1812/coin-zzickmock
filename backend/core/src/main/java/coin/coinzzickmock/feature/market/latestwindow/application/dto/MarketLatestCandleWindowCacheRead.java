package coin.coinzzickmock.feature.market.latestwindow.application.dto;

import java.util.Optional;

public record MarketLatestCandleWindowCacheRead(
        Optional<MarketLatestCandleWindowPage> page,
        String result
) {
    public static MarketLatestCandleWindowCacheRead hit(MarketLatestCandleWindowPage page) {
        return new MarketLatestCandleWindowCacheRead(Optional.of(page), "hit");
    }

    public static MarketLatestCandleWindowCacheRead miss() {
        return new MarketLatestCandleWindowCacheRead(Optional.empty(), "miss");
    }

    public static MarketLatestCandleWindowCacheRead unavailable() {
        return new MarketLatestCandleWindowCacheRead(Optional.empty(), "unavailable");
    }
}
