package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.common.cache.CoinCacheNames;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class MarketSnapshotStore {
    private static final String SUPPORTED_SYMBOLS_KEY = "supported-symbols";

    private final Cache marketSnapshotCache;
    private final Cache marketSupportedSymbolsCache;

    public MarketSnapshotStore(@Qualifier("localCacheManager") CacheManager cacheManager) {
        this.marketSnapshotCache = requireCache(cacheManager, CoinCacheNames.MARKET_SNAPSHOT_LOCAL_CACHE);
        this.marketSupportedSymbolsCache = requireCache(cacheManager,
                CoinCacheNames.MARKET_SUPPORTED_SYMBOLS_LOCAL_CACHE);
    }

    public void putSupportedMarkets(List<MarketSummaryResult> markets) {
        List<String> nextSymbols = markets.stream()
                .map(MarketSummaryResult::symbol)
                .toList();
        Set<String> nextSymbolSet = new LinkedHashSet<>(nextSymbols);

        getSupportedSymbols().stream()
                .filter(symbol -> !nextSymbolSet.contains(symbol))
                .forEach(marketSnapshotCache::evict);

        markets.forEach(this::putMarket);
        marketSupportedSymbolsCache.put(SUPPORTED_SYMBOLS_KEY, new SupportedSymbols(nextSymbols));
    }

    public boolean hasSupportedMarkets() {
        return !getSupportedSymbols().isEmpty();
    }

    public List<MarketSummaryResult> getSupportedMarkets() {
        return getSupportedSymbols().stream()
                .map(this::getMarket)
                .flatMap(Optional::stream)
                .toList();
    }

    private List<String> getSupportedSymbols() {
        SupportedSymbols cached = marketSupportedSymbolsCache.get(SUPPORTED_SYMBOLS_KEY, SupportedSymbols.class);
        if (cached == null) {
            return List.of();
        }
        return cached.symbols();
    }

    public Optional<MarketSummaryResult> getMarket(String symbol) {
        return Optional.ofNullable(marketSnapshotCache.get(symbol, MarketSummaryResult.class));
    }

    public void putMarket(MarketSummaryResult result) {
        marketSnapshotCache.put(result.symbol(), result);
    }

    private Cache requireCache(CacheManager cacheManager, String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new IllegalStateException("Required cache is not configured: " + cacheName);
        }
        return cache;
    }

    private record SupportedSymbols(List<String> symbols) {
    }
}
