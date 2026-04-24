package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketRealtimeFeed {

    private final MarketSupportedMarketRefresher marketSupportedMarketRefresher;
    private final MarketSnapshotStore marketSnapshotStore;

    @Scheduled(fixedDelayString = "${coin.market.refresh-delay-ms:1000}")
    public void refreshSupportedMarkets() {
        refreshSupportedMarkets(Instant.now());
    }

    void refreshSupportedMarkets(Instant observedAt) {
        List<MarketSummaryResult> refreshedMarkets = marketSupportedMarketRefresher.refreshSupportedMarkets();
        if (refreshedMarkets.isEmpty()) {
            return;
        }
    }

    public List<MarketSummaryResult> getSupportedMarkets() {
        if (!marketSnapshotStore.hasSupportedMarkets()) {
            marketSupportedMarketRefresher.refreshSupportedMarkets();
        }

        return marketSnapshotStore.getSupportedMarkets();
    }

    public MarketSummaryResult getMarket(String symbol) {
        MarketSummaryResult cached = marketSnapshotStore.getMarket(symbol).orElse(null);
        if (cached != null) {
            return cached;
        }

        return getSupportedMarkets().stream()
                .filter(result -> result.symbol().equals(symbol))
                .findFirst()
                .orElseThrow(() -> new CoreException(ErrorCode.MARKET_NOT_FOUND));
    }
}
