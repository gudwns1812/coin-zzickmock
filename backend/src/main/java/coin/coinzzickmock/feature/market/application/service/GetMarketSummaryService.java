package coin.coinzzickmock.feature.market.application.service;

import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.feature.market.application.query.GetMarketQuery;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class GetMarketSummaryService {
    private final MarketRealtimeService marketRealtimeService;

    public GetMarketSummaryService(MarketRealtimeService marketRealtimeService) {
        this.marketRealtimeService = marketRealtimeService;
    }

    public List<MarketSummaryResult> getSupportedMarkets() {
        return marketRealtimeService.getSupportedMarkets();
    }

    public MarketSummaryResult getMarket(GetMarketQuery query) {
        if (query.symbol() == null || query.symbol().isBlank()) {
            throw new CoreException(ErrorCode.MARKET_NOT_FOUND, "지원하지 않는 심볼입니다: " + query.symbol());
        }

        return marketRealtimeService.getMarket(query.symbol());
    }

    public void subscribe(String symbol, Consumer<MarketSummaryResult> listener) {
        marketRealtimeService.subscribe(symbol, listener);
    }

    public void unsubscribe(String symbol, Consumer<MarketSummaryResult> listener) {
        marketRealtimeService.unsubscribe(symbol, listener);
    }
}
