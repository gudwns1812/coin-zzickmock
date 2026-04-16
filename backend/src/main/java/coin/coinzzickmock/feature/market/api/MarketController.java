package coin.coinzzickmock.feature.market.api;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.feature.market.application.query.GetMarketQuery;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.application.service.GetMarketSummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/futures/markets")
public class MarketController {
    private final GetMarketSummaryService getMarketSummaryService;

    public MarketController(GetMarketSummaryService getMarketSummaryService) {
        this.getMarketSummaryService = getMarketSummaryService;
    }

    @GetMapping
    public ApiResponse<List<MarketSummaryResponse>> list() {
        List<MarketSummaryResponse> responses = getMarketSummaryService.getSupportedMarkets().stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/{symbol}")
    public ApiResponse<MarketSummaryResponse> detail(@PathVariable String symbol) {
        return ApiResponse.success(toResponse(
                getMarketSummaryService.getMarket(new GetMarketQuery(symbol))
        ));
    }

    private MarketSummaryResponse toResponse(MarketSummaryResult result) {
        return new MarketSummaryResponse(
                result.symbol(),
                result.displayName(),
                result.lastPrice(),
                result.markPrice(),
                result.fundingRate(),
                result.change24h()
        );
    }
}
