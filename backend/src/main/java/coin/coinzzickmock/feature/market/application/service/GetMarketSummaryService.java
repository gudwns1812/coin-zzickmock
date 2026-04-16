package coin.coinzzickmock.feature.market.application.service;

import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.error.NotFoundException;
import coin.coinzzickmock.feature.market.application.query.GetMarketQuery;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.providers.Providers;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetMarketSummaryService {
    private final Providers providers;

    public GetMarketSummaryService(Providers providers) {
        this.providers = providers;
    }

    public List<MarketSummaryResult> getSupportedMarkets() {
        return providers.connector().marketDataGateway().loadSupportedMarkets().stream()
                .map(this::toResult)
                .toList();
    }

    public MarketSummaryResult getMarket(GetMarketQuery query) {
        MarketSnapshot snapshot = providers.connector().marketDataGateway().loadMarket(query.symbol());
        if (snapshot == null) {
            throw new NotFoundException(ErrorCode.MARKET_NOT_FOUND, "지원하지 않는 심볼입니다: " + query.symbol());
        }
        return toResult(snapshot);
    }

    private MarketSummaryResult toResult(MarketSnapshot snapshot) {
        return new MarketSummaryResult(
                snapshot.symbol(),
                snapshot.displayName(),
                snapshot.lastPrice(),
                snapshot.markPrice(),
                snapshot.fundingRate(),
                snapshot.change24h()
        );
    }
}
