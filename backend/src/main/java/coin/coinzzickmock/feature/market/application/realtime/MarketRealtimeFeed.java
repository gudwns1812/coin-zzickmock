package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.FundingSchedule;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MarketRealtimeFeed {

    private final MarketSupportedMarketRefresher marketSupportedMarketRefresher;
    private final MarketSnapshotStore marketSnapshotStore;
    private final RealtimeMarketSummaryProjector realtimeMarketSummaryProjector;

    @Autowired
    public MarketRealtimeFeed(
            MarketSupportedMarketRefresher marketSupportedMarketRefresher,
            MarketSnapshotStore marketSnapshotStore,
            RealtimeMarketSummaryProjector realtimeMarketSummaryProjector
    ) {
        this.marketSupportedMarketRefresher = marketSupportedMarketRefresher;
        this.marketSnapshotStore = marketSnapshotStore;
        this.realtimeMarketSummaryProjector = realtimeMarketSummaryProjector;
    }

    MarketRealtimeFeed(
            MarketSupportedMarketRefresher marketSupportedMarketRefresher,
            MarketSnapshotStore marketSnapshotStore
    ) {
        this(
                marketSupportedMarketRefresher,
                marketSnapshotStore,
                new RealtimeMarketSummaryProjector(new RealtimeMarketDataStore(),
                        symbol -> FundingSchedule.defaultUsdtPerpetual())
        );
    }

    @Scheduled(fixedDelayString = "${coin.market.refresh-delay-ms:1000}")
    public void refreshSupportedMarkets() {
        marketSupportedMarketRefresher.refreshSupportedMarkets();
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
            marketSupportedMarketRefresher.refreshSupportedMarkets();
        }

        return marketSnapshotStore.getSupportedMarkets().stream()
                .map(cached -> realtimeMarketSummaryProjector.project(cached.symbol(), cached).orElse(cached))
                .toList();
    }
}
