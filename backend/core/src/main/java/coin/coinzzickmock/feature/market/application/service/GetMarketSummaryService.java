package coin.coinzzickmock.feature.market.application.service;

import coin.coinzzickmock.feature.market.application.query.GetMarketQuery;
import coin.coinzzickmock.feature.market.application.query.MarketRealtimeFeed;
import coin.coinzzickmock.feature.market.application.dto.MarketSummaryResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetMarketSummaryService {
    private final MarketRealtimeFeed marketRealtimeFeed;

    public List<MarketSummaryResult> getSupportedMarkets() {
        return marketRealtimeFeed.getSupportedMarkets();
    }

    public MarketSummaryResult getMarket(GetMarketQuery query) {
        return marketRealtimeFeed.getMarket(query.symbol());
    }
}
