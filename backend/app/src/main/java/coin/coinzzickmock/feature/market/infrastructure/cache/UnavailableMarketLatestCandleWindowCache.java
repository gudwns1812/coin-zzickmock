package coin.coinzzickmock.feature.market.infrastructure.cache;

import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowCache;
import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowCacheRead;
import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowKey;
import coin.coinzzickmock.feature.market.application.latestwindow.MarketLatestCandleWindowPage;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "coin.cache.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
class UnavailableMarketLatestCandleWindowCache implements MarketLatestCandleWindowCache {
    @Override
    public MarketLatestCandleWindowCacheRead read(MarketLatestCandleWindowKey key) {
        return MarketLatestCandleWindowCacheRead.unavailable();
    }

    @Override
    public boolean write(MarketLatestCandleWindowKey key, MarketLatestCandleWindowPage page, Duration ttl) {
        return false;
    }
}
