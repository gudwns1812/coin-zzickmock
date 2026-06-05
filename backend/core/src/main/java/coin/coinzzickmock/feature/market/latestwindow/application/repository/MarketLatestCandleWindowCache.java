package coin.coinzzickmock.feature.market.latestwindow.application.repository;

import coin.coinzzickmock.feature.market.latestwindow.application.dto.MarketLatestCandleWindowCacheRead;
import coin.coinzzickmock.feature.market.latestwindow.application.dto.MarketLatestCandleWindowKey;
import coin.coinzzickmock.feature.market.latestwindow.application.dto.MarketLatestCandleWindowPage;
import java.time.Duration;

public interface MarketLatestCandleWindowCache {
    MarketLatestCandleWindowCacheRead read(MarketLatestCandleWindowKey key);

    boolean write(MarketLatestCandleWindowKey key, MarketLatestCandleWindowPage page, Duration ttl);
}
