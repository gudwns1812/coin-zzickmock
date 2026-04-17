package coin.coinzzickmock.feature.market.api;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.feature.market.application.query.GetMarketQuery;
import coin.coinzzickmock.feature.market.application.realtime.MarketRealtimeFeed;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.application.service.GetMarketSummaryService;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/futures/markets")
@RequiredArgsConstructor
public class MarketController {
    private final GetMarketSummaryService getMarketSummaryService;
    private final MarketRealtimeFeed marketRealtimeFeed;

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

    @GetMapping(value = "/{symbol}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String symbol) {
        MarketSummaryResult currentMarket = getMarketSummaryService.getMarket(new GetMarketQuery(symbol));
        SseEmitter emitter = createEmitter();
        AtomicReference<Consumer<MarketSummaryResult>> listenerReference = new AtomicReference<>();
        Consumer<MarketSummaryResult> listener = result -> sendEvent(symbol, emitter, listenerReference, result);
        listenerReference.set(listener);

        emitter.onCompletion(() -> marketRealtimeFeed.unsubscribe(symbol, listenerReference.get()));
        emitter.onTimeout(() -> {
            marketRealtimeFeed.unsubscribe(symbol, listenerReference.get());
            emitter.complete();
        });
        emitter.onError(error -> marketRealtimeFeed.unsubscribe(symbol, listenerReference.get()));

        if (sendEvent(symbol, emitter, listenerReference, currentMarket)) {
            marketRealtimeFeed.subscribe(symbol, listener);
        }

        return emitter;
    }

    private MarketSummaryResponse toResponse(MarketSummaryResult result) {
        return new MarketSummaryResponse(
                result.symbol(),
                result.displayName(),
                result.lastPrice(),
                result.markPrice(),
                result.indexPrice(),
                result.fundingRate(),
                result.change24h()
        );
    }

    SseEmitter createEmitter() {
        return new SseEmitter(0L);
    }

    boolean sendEvent(
            String symbol,
            SseEmitter emitter,
            AtomicReference<Consumer<MarketSummaryResult>> listenerReference,
            MarketSummaryResult result
    ) {
        try {
            emitter.send(toResponse(result));
            return true;
        } catch (IOException exception) {
            marketRealtimeFeed.unsubscribe(symbol, listenerReference.get());
            emitter.complete();
            return false;
        }
    }
}
