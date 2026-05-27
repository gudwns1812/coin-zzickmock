package coin.coinzzickmock.feature.market.application.latestwindow;

import java.time.Duration;

public interface MarketLatestCandleWindowCache {
    MarketLatestCandleWindowCacheRead read(MarketLatestCandleWindowKey key);

    boolean write(MarketLatestCandleWindowKey key, MarketLatestCandleWindowPage page, Duration ttl);
}
