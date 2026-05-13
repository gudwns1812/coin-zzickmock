package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.query.GetMarketCandlesQuery;
import coin.coinzzickmock.feature.market.application.query.GetMarketQuery;
import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.application.service.GetMarketCandlesService;
import coin.coinzzickmock.feature.market.application.service.GetMarketSummaryService;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/futures/markets")
public class MarketController {
    private final GetMarketSummaryService getMarketSummaryService;
    private final GetMarketCandlesService getMarketCandlesService;
    private final MarketStreamGateway marketStreamGateway;
    private final long streamTimeoutMs;

    @Autowired
    public MarketController(
            GetMarketSummaryService getMarketSummaryService,
            GetMarketCandlesService getMarketCandlesService,
            MarketStreamGateway marketStreamGateway,
            @Value("${coin.market.sse.timeout-ms:300000}") long streamTimeoutMs
    ) {
        this.getMarketSummaryService = getMarketSummaryService;
        this.getMarketCandlesService = getMarketCandlesService;
        this.marketStreamGateway = marketStreamGateway;
        this.streamTimeoutMs = streamTimeoutMs;
    }

    @GetMapping
    public ApiResponse<List<MarketSummaryHttpResponse>> list() {
        List<MarketSummaryHttpResponse> responses = getMarketSummaryService.getSupportedMarkets().stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/{symbol}")
    public ApiResponse<MarketSummaryHttpResponse> detail(@PathVariable String symbol) {
        return ApiResponse.success(toResponse(
                getMarketSummaryService.getMarket(new GetMarketQuery(symbol))
        ));
    }

    @GetMapping("/{symbol}/candles")
    public ApiResponse<List<MarketCandleHttpResponse>> candles(
            @PathVariable String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Instant before
    ) {
        List<MarketCandleResult> candles = getMarketCandlesService.getCandles(
                new GetMarketCandlesQuery(symbol, interval, limit, before)
        );
        return ApiResponse.success(candles.stream().map(this::toCandleResponse).toList());
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter unifiedStream(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) String clientKey
    ) {
        SseEmitter emitter = createEmitter();
        marketStreamGateway.openUnified(symbol, MarketCandleInterval.from(interval).value(), clientKey, emitter);
        return emitter;
    }

    @GetMapping(value = "/summary/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter summaryStream(
            @RequestParam String symbols,
            @RequestParam(required = false) String clientKey
    ) {
        SseEmitter emitter = createEmitter();
        marketStreamGateway.openSummary(parseSummarySymbols(symbols), clientKey, emitter);
        return emitter;
    }

    public SseEmitter stream(String symbol) {
        return stream(symbol, null);
    }

    @GetMapping(value = "/{symbol}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @PathVariable String symbol,
            @RequestParam(required = false) String clientKey
    ) {
        SseEmitter emitter = createEmitter();
        marketStreamGateway.openSummary(Set.of(symbol), clientKey, emitter);
        return emitter;
    }

    public SseEmitter candleStream(String symbol, String interval) {
        return candleStream(symbol, interval, null);
    }

    @GetMapping(value = "/{symbol}/candles/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter candleStream(
            @PathVariable String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) String clientKey
    ) {
        SseEmitter emitter = createEmitter();
        marketStreamGateway.openCandle(symbol, MarketCandleInterval.from(interval).value(), clientKey, emitter);
        return emitter;
    }

    private Set<String> parseSummarySymbols(String rawSymbols) {
        if (rawSymbols == null || rawSymbols.isBlank()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        Set<String> symbols = new LinkedHashSet<>();
        for (String rawSymbol : rawSymbols.split(",")) {
            String symbol = rawSymbol.trim();
            if (symbol.isBlank()) {
                throw new CoreException(ErrorCode.INVALID_REQUEST);
            }
            symbols.add(symbol);
        }
        return symbols;
    }

    private MarketSummaryHttpResponse toResponse(MarketSummaryResult result) {
        return MarketHttpResponseMapper.toResponse(result);
    }

    private MarketCandleHttpResponse toCandleResponse(MarketCandleResult result) {
        return MarketHttpResponseMapper.toResponse(result);
    }

    SseEmitter createEmitter() {
        return new SseEmitter(streamTimeoutMs);
    }
}
