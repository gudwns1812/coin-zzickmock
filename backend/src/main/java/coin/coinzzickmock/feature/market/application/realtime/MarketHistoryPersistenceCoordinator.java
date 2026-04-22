package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MarketHistoryPersistenceCoordinator {
    private final MarketHistoryRecorder marketHistoryRecorder;

    @Transactional
    public void refreshHistory(List<MarketSummaryResult> refreshedMarkets, Instant observedAt) {
        List<MarketSummaryResult> distinctMarkets = distinctMarkets(refreshedMarkets);
        if (distinctMarkets.isEmpty()) {
            return;
        }

        marketHistoryRecorder.recordSnapshots(distinctMarkets, observedAt);
    }

    private List<MarketSummaryResult> distinctMarkets(List<MarketSummaryResult> refreshedMarkets) {
        if (refreshedMarkets == null || refreshedMarkets.isEmpty()) {
            return List.of();
        }

        LinkedHashMap<String, MarketSummaryResult> distinctMarkets = new LinkedHashMap<>();
        refreshedMarkets.stream()
                .filter(market -> market != null && market.symbol() != null)
                .forEach(market -> distinctMarkets.putIfAbsent(market.symbol(), market));
        return distinctMarkets.values().stream().toList();
    }
}
