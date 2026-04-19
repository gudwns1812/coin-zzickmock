package coin.coinzzickmock.feature.market.api;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.feature.market.application.query.GetMarketQuery;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.application.service.GetMarketSummaryService;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/futures/markets")
public class MarketController {
    private final GetMarketSummaryService getMarketSummaryService;
    private final MarketRealtimeSseBroker marketRealtimeSseBroker;
    private final long streamTimeoutMs;

    public MarketController(
            GetMarketSummaryService getMarketSummaryService,
            MarketRealtimeSseBroker marketRealtimeSseBroker,
            @Value("${coin.market.sse.timeout-ms:300000}") long streamTimeoutMs
    ) {
        this.getMarketSummaryService = getMarketSummaryService;
        this.marketRealtimeSseBroker = marketRealtimeSseBroker;
        this.streamTimeoutMs = streamTimeoutMs;
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
        return ApiResponse.success(MarketSummaryResponse.from(
                getMarketSummaryService.getMarket(new GetMarketQuery(symbol))
        ));
    }

    @GetMapping(value = "/{symbol}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String symbol) {
        MarketRealtimeSseBroker.SseSubscriptionPermit permit = marketRealtimeSseBroker.reserve(symbol);
        SseEmitter emitter = createEmitter();
        try {
            MarketSummaryResult currentMarket = getMarketSummaryService.getMarket(new GetMarketQuery(symbol));

            if (sendEvent(emitter, currentMarket)) {
                marketRealtimeSseBroker.register(permit, emitter);
            } else {
                marketRealtimeSseBroker.release(permit);
            }
            return emitter;
        } catch (RuntimeException exception) {
            marketRealtimeSseBroker.release(permit);
            throw exception;
        }
    }

    private MarketSummaryResponse toResponse(MarketSummaryResult result) {
        return MarketSummaryResponse.from(result);
    }

    SseEmitter createEmitter() {
        return new SseEmitter(streamTimeoutMs);
    }

    boolean sendEvent(
            SseEmitter emitter,
            MarketSummaryResult result
    ) {
        try {
            emitter.send(toResponse(result));
            return true;
        } catch (IOException exception) {
            emitter.complete();
            return false;
        }
    }
}
