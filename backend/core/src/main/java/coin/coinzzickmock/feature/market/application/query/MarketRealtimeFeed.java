package coin.coinzzickmock.feature.market.application.query;

import coin.coinzzickmock.feature.market.application.implement.MarketRealtimeRefreshCoordinator;
import coin.coinzzickmock.feature.market.application.implement.MarketSnapshotStore;
import coin.coinzzickmock.feature.market.application.implement.MarketSupportedMarketRefresher;
import coin.coinzzickmock.feature.market.application.implement.RealtimeMarketDataStore;
import coin.coinzzickmock.feature.market.application.implement.RealtimeMarketSummaryProjector;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.dto.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.FundingSchedule;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MarketRealtimeFeed {

    private final MarketRealtimeRefreshCoordinator marketRealtimeRefreshCoordinator;
    private final MarketSnapshotStore marketSnapshotStore;
    private final RealtimeMarketSummaryProjector realtimeMarketSummaryProjector;

    @Autowired
    public MarketRealtimeFeed(
            MarketRealtimeRefreshCoordinator marketRealtimeRefreshCoordinator,
            MarketSnapshotStore marketSnapshotStore,
            RealtimeMarketSummaryProjector realtimeMarketSummaryProjector
    ) {
        this.marketRealtimeRefreshCoordinator = marketRealtimeRefreshCoordinator;
        this.marketSnapshotStore = marketSnapshotStore;
        this.realtimeMarketSummaryProjector = realtimeMarketSummaryProjector;
    }

    public MarketRealtimeFeed(
            MarketSupportedMarketRefresher marketSupportedMarketRefresher,
            MarketSnapshotStore marketSnapshotStore
    ) {
        this(
                new MarketRealtimeRefreshCoordinator(marketSupportedMarketRefresher),
                marketSnapshotStore,
                new RealtimeMarketSummaryProjector(new RealtimeMarketDataStore(),
                        symbol -> FundingSchedule.defaultUsdtPerpetual())
        );
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
