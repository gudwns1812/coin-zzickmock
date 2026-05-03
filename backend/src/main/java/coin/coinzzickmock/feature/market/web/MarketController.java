package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.feature.market.application.query.GetMarketCandlesQuery;
import coin.coinzzickmock.feature.market.application.query.GetMarketQuery;
import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleProjector;
import coin.coinzzickmock.feature.market.application.service.GetMarketCandlesService;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.application.service.GetMarketSummaryService;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/futures/markets")
public class MarketController {
    private final GetMarketSummaryService getMarketSummaryService;
    private final GetMarketCandlesService getMarketCandlesService;
    private final MarketRealtimeSseBroker marketRealtimeSseBroker;
    private final MarketCandleRealtimeSseBroker marketCandleRealtimeSseBroker;
    private final RealtimeMarketCandleProjector realtimeMarketCandleProjector;
    private final long streamTimeoutMs;

    @Autowired
    public MarketController(
            GetMarketSummaryService getMarketSummaryService,
            GetMarketCandlesService getMarketCandlesService,
            MarketRealtimeSseBroker marketRealtimeSseBroker,
            MarketCandleRealtimeSseBroker marketCandleRealtimeSseBroker,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector,
            @Value("${coin.market.sse.timeout-ms:300000}") long streamTimeoutMs
    ) {
        this.getMarketSummaryService = getMarketSummaryService;
        this.getMarketCandlesService = getMarketCandlesService;
        this.marketRealtimeSseBroker = marketRealtimeSseBroker;
        this.marketCandleRealtimeSseBroker = marketCandleRealtimeSseBroker;
        this.realtimeMarketCandleProjector = realtimeMarketCandleProjector;
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

    @GetMapping("/{symbol}/candles")
    public ApiResponse<List<MarketCandleResponse>> candles(
            @PathVariable String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Instant before
    ) {
        List<MarketCandleResult> candles = getMarketCandlesService.getCandles(
                new GetMarketCandlesQuery(symbol, interval, limit, before)
        );
        return ApiResponse.success(candles.stream().map(MarketCandleResponse::from).toList());
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

    @GetMapping(value = "/{symbol}/candles/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter candleStream(@PathVariable String symbol, @RequestParam String interval) {
        MarketCandleInterval candleInterval = MarketCandleInterval.from(interval);
        SseEmitter emitter = createEmitter();
        boolean initialSendSucceeded = realtimeMarketCandleProjector.latest(symbol, candleInterval)
                .map(candle -> sendCandleEvent(emitter, candle))
                .orElse(true);
        if (initialSendSucceeded) {
            marketCandleRealtimeSseBroker.register(symbol, candleInterval, emitter);
        }
        return emitter;
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
            log.debug("Initial market SSE send failed; completing emitter. symbol={}", result.symbol(), exception);
            emitter.complete();
            return false;
        }
    }

    boolean sendCandleEvent(SseEmitter emitter, MarketCandleResult result) {
        try {
            emitter.send(MarketCandleResponse.from(result));
            return true;
        } catch (IOException exception) {
            log.debug("Initial market candle SSE send failed; completing emitter. openTime={}",
                    result.openTime(), exception);
            emitter.complete();
            return false;
        }
    }
}
