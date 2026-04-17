package coin.coinzzickmock.feature.market.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.query.GetMarketQuery;
import coin.coinzzickmock.feature.market.application.realtime.MarketRealtimeFeed;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
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
        if (query.symbol() == null || query.symbol().isBlank()) {
            throw new CoreException(ErrorCode.MARKET_NOT_FOUND, "지원하지 않는 심볼입니다: " + query.symbol());
        }

        return marketRealtimeFeed.getMarket(query.symbol());
    }
}
