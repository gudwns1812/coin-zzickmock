package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.feature.market.application.query.GetMarketCandlesQuery;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.query.GetMarketQuery;
import coin.coinzzickmock.feature.market.application.dto.MarketCandleResult;
import coin.coinzzickmock.feature.market.application.dto.MarketSummaryResult;
import coin.coinzzickmock.feature.market.application.service.GetMarketCandlesService;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import coin.coinzzickmock.feature.market.application.service.GetMarketSummaryService;
import java.time.Instant;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/futures/markets")
public class MarketController {
    private static final String CANDLE_INTERVAL_PATTERN = "^(1m|3m|5m|15m|1h|4h|12h|1D|1W|1M)$";

    private final GetMarketSummaryService getMarketSummaryService;
    private final GetMarketCandlesService getMarketCandlesService;

    public MarketController(
            GetMarketSummaryService getMarketSummaryService,
            GetMarketCandlesService getMarketCandlesService
    ) {
        this.getMarketSummaryService = getMarketSummaryService;
        this.getMarketCandlesService = getMarketCandlesService;
    }

    @GetMapping
    public ApiResponse<List<MarketSummaryHttpResponse>> list() {
        List<MarketSummaryHttpResponse> responses = getMarketSummaryService.getSupportedMarkets().stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/{symbol}")
    public ApiResponse<MarketSummaryHttpResponse> detail(@NotBlank @PathVariable String symbol) {
        return ApiResponse.success(toResponse(
                getMarketSummaryService.getMarket(new GetMarketQuery(symbol))
        ));
    }

    @GetMapping("/{symbol}/candles")
    public ApiResponse<List<MarketCandleHttpResponse>> candles(
            @NotBlank @PathVariable String symbol,
            @Pattern(regexp = CANDLE_INTERVAL_PATTERN) @RequestParam String interval,
            @Min(1) @Max(240) @RequestParam(defaultValue = "120") int limit,
            @RequestParam(required = false) Instant before
    ) {
        List<MarketCandleResult> candles = getMarketCandlesService.getCandles(
                new GetMarketCandlesQuery(symbol, parseInterval(interval), limit, before)
        );
        return ApiResponse.success(candles.stream().map(this::toCandleResponse).toList());
    }

    private MarketCandleInterval parseInterval(String interval) {
        try {
            return MarketCandleInterval.from(interval);
        } catch (CoreException exception) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    private MarketSummaryHttpResponse toResponse(MarketSummaryResult result) {
        return MarketHttpResponseMapper.toResponse(result);
    }

    private MarketCandleHttpResponse toCandleResponse(MarketCandleResult result) {
        return MarketHttpResponseMapper.toResponse(result);
    }

}
