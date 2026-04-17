package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.providers.Providers;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MarketRealtimeFeed {
    private static final Logger log = LoggerFactory.getLogger(MarketRealtimeFeed.class);

    private final Providers providers;
    private final ConcurrentMap<String, MarketSummaryResult> latestMarkets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CopyOnWriteArrayList<Consumer<MarketSummaryResult>>> subscribers =
            new ConcurrentHashMap<>();
    private volatile List<String> supportedSymbols = List.of("BTCUSDT", "ETHUSDT");

    public MarketRealtimeFeed(Providers providers) {
        this.providers = providers;
    }

    @PostConstruct
    void initializeCache() {
        refreshSupportedMarkets();
    }

    @Scheduled(fixedDelayString = "${coin.market.refresh-delay-ms:3000}")
    public void refreshSupportedMarkets() {
        List<MarketSummaryResult> refreshedMarkets = providers.connector().marketDataGateway().loadSupportedMarkets()
                .stream()
                .filter(Objects::nonNull)
                .map(this::toResult)
                .toList();

        if (refreshedMarkets.isEmpty()) {
            return;
        }

        supportedSymbols = refreshedMarkets.stream()
                .map(MarketSummaryResult::symbol)
                .toList();

        refreshedMarkets.forEach(this::cacheAndPublish);
    }

    public List<MarketSummaryResult> getSupportedMarkets() {
        if (latestMarkets.isEmpty()) {
            refreshSupportedMarkets();
        }

        return supportedSymbols.stream()
                .map(latestMarkets::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public MarketSummaryResult getMarket(String symbol) {
        MarketSummaryResult cached = latestMarkets.get(symbol);
        if (cached != null) {
            return cached;
        }

        MarketSnapshot loaded = providers.connector().marketDataGateway().loadMarket(symbol);
        if (loaded == null) {
            throw new CoreException(ErrorCode.MARKET_NOT_FOUND, "지원하지 않는 심볼입니다: " + symbol);
        }

        MarketSummaryResult result = toResult(loaded);
        latestMarkets.put(symbol, result);
        return result;
    }

    public void subscribe(String symbol, Consumer<MarketSummaryResult> listener) {
        subscribers.computeIfAbsent(symbol, key -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void unsubscribe(String symbol, Consumer<MarketSummaryResult> listener) {
        CopyOnWriteArrayList<Consumer<MarketSummaryResult>> symbolSubscribers = subscribers.get(symbol);
        if (symbolSubscribers == null) {
            return;
        }

        symbolSubscribers.remove(listener);
        if (symbolSubscribers.isEmpty()) {
            subscribers.remove(symbol, symbolSubscribers);
        }
    }

    private void cacheAndPublish(MarketSummaryResult result) {
        log.debug("Publishing market data: symbol={}, lastPrice={}", result.symbol(), result.lastPrice());
        latestMarkets.put(result.symbol(), result);

        CopyOnWriteArrayList<Consumer<MarketSummaryResult>> symbolSubscribers = subscribers.get(result.symbol());
        if (symbolSubscribers == null) {
            return;
        }

        symbolSubscribers.forEach(listener -> listener.accept(result));
    }

    private MarketSummaryResult toResult(MarketSnapshot snapshot) {
        return new MarketSummaryResult(
                snapshot.symbol(),
                snapshot.displayName(),
                snapshot.lastPrice(),
                snapshot.markPrice(),
                snapshot.indexPrice(),
                snapshot.fundingRate(),
                snapshot.change24h()
        );
    }
}
