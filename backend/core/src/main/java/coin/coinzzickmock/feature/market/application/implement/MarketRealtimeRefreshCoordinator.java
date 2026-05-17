package coin.coinzzickmock.feature.market.application.implement;

import coin.coinzzickmock.feature.market.application.dto.MarketSummaryResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketRealtimeRefreshCoordinator {
    private final MarketSupportedMarketRefresher marketSupportedMarketRefresher;

    public List<MarketSummaryResult> refreshSupportedMarkets() {
        return marketSupportedMarketRefresher.refreshSupportedMarkets();
    }
}
