package coin.coinzzickmock.feature.market.catalog.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.catalog.application.dto.MarketSummaryResult;
import coin.coinzzickmock.feature.market.catalog.application.implement.MarketRealtimeRefreshCoordinator;
import coin.coinzzickmock.feature.market.catalog.application.implement.MarketSnapshotStore;
import coin.coinzzickmock.feature.market.catalog.application.implement.RealtimeMarketSummaryProjector;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MarketRealtimeFeed {

    private final MarketRealtimeRefreshCoordinator marketRealtimeRefreshCoordinator;
    private final MarketSnapshotStore marketSnapshotStore;
    private final RealtimeMarketSummaryProjector realtimeMarketSummaryProjector;

    public MarketRealtimeFeed(
            MarketRealtimeRefreshCoordinator marketRealtimeRefreshCoordinator,
            MarketSnapshotStore marketSnapshotStore,
            RealtimeMarketSummaryProjector realtimeMarketSummaryProjector
    ) {
        this.marketRealtimeRefreshCoordinator = marketRealtimeRefreshCoordinator;
        this.marketSnapshotStore = marketSnapshotStore;
        this.realtimeMarketSummaryProjector = realtimeMarketSummaryProjector;
    }

    public void refreshSupportedMarkets() {
        marketRealtimeRefreshCoordinator.refreshSupportedMarkets();
    }

    public MarketSummaryResult getMarket(String symbol) {
        MarketSummaryResult cached = marketSnapshotStore.getMarket(symbol).orElse(null);
        MarketSummaryResult realtime = realtimeMarketSummaryProjector.project(symbol, cached).orElse(null);
        if (realtime != null) {
            return realtime;
        }
        if (cached != null) {
            return cached;
        }

        return getSupportedMarkets().stream()
                .filter(result -> result.symbol().equals(symbol))
                .findFirst()
                .orElseThrow(() -> new CoreException(ErrorCode.MARKET_NOT_FOUND));
    }

    public List<MarketSummaryResult> getSupportedMarkets() {
        if (!marketSnapshotStore.hasSupportedMarkets()) {
            marketRealtimeRefreshCoordinator.refreshSupportedMarkets();
        }

        return marketSnapshotStore.getSupportedMarkets().stream()
                .map(cached -> realtimeMarketSummaryProjector.project(cached.symbol(), cached).orElse(cached))
                .toList();
    }
}
