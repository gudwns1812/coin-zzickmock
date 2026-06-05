package coin.coinzzickmock.feature.market.latestwindow.application.implement;

import coin.coinzzickmock.feature.market.latestwindow.application.dto.MarketLatestCandleWindowKey;
import coin.coinzzickmock.feature.market.latestwindow.application.dto.MarketLatestCandleWindowPage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class MarketLatestCandleWindowSingleflight {
    private final ConcurrentMap<String, CompletableFuture<MarketLatestCandleWindowPage>> fills = new ConcurrentHashMap<>();

    public MarketLatestCandleWindowPage load(MarketLatestCandleWindowKey key, Supplier<MarketLatestCandleWindowPage> supplier) {
        CompletableFuture<MarketLatestCandleWindowPage> fill = new CompletableFuture<>();
        CompletableFuture<MarketLatestCandleWindowPage> existing = fills.putIfAbsent(key.cacheKey(), fill);
        if (existing != null) {
            return existing.join();
        }

        try {
            MarketLatestCandleWindowPage page = supplier.get();
            fill.complete(page);
            return page;
        } catch (RuntimeException exception) {
            fill.completeExceptionally(exception);
            throw exception;
        } finally {
            fills.remove(key.cacheKey());
        }
    }
}
