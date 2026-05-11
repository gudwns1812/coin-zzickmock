package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.common.api.ApiResponse;
import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.web.SseClientKey;
import coin.coinzzickmock.feature.market.application.query.GetMarketCandlesQuery;
import coin.coinzzickmock.feature.market.application.query.GetMarketQuery;
import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.application.realtime.CurrentMarketCandleBootstrapper;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleProjector;
import coin.coinzzickmock.feature.market.application.service.GetMarketCandlesService;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.application.service.GetMarketSummaryService;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
    private final CurrentMarketCandleBootstrapper currentMarketCandleBootstrapper;
    private final UnifiedMarketStreamOpener unifiedMarketStreamOpener;
    private final long streamTimeoutMs;

    @Autowired
    public MarketController(
            GetMarketSummaryService getMarketSummaryService,
            GetMarketCandlesService getMarketCandlesService,
            MarketRealtimeSseBroker marketRealtimeSseBroker,
            MarketCandleRealtimeSseBroker marketCandleRealtimeSseBroker,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector,
            CurrentMarketCandleBootstrapper currentMarketCandleBootstrapper,
            UnifiedMarketStreamOpener unifiedMarketStreamOpener,
            @Value("${coin.market.sse.timeout-ms:300000}") long streamTimeoutMs
    ) {
        this.getMarketSummaryService = getMarketSummaryService;
        this.getMarketCandlesService = getMarketCandlesService;
        this.marketRealtimeSseBroker = marketRealtimeSseBroker;
        this.marketCandleRealtimeSseBroker = marketCandleRealtimeSseBroker;
        this.realtimeMarketCandleProjector = realtimeMarketCandleProjector;
        this.currentMarketCandleBootstrapper = currentMarketCandleBootstrapper;
        this.unifiedMarketStreamOpener = unifiedMarketStreamOpener;
        this.streamTimeoutMs = streamTimeoutMs;
    }

    MarketController(
            GetMarketSummaryService getMarketSummaryService,
            GetMarketCandlesService getMarketCandlesService,
            MarketRealtimeSseBroker marketRealtimeSseBroker,
            MarketCandleRealtimeSseBroker marketCandleRealtimeSseBroker,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector,
            long streamTimeoutMs
    ) {
        this(
                getMarketSummaryService,
                getMarketCandlesService,
                marketRealtimeSseBroker,
                marketCandleRealtimeSseBroker,
                realtimeMarketCandleProjector,
                null,
                null,
                streamTimeoutMs
        );
    }
    MarketController(
            GetMarketSummaryService getMarketSummaryService,
            GetMarketCandlesService getMarketCandlesService,
            MarketRealtimeSseBroker marketRealtimeSseBroker,
            MarketCandleRealtimeSseBroker marketCandleRealtimeSseBroker,
            RealtimeMarketCandleProjector realtimeMarketCandleProjector,
            CurrentMarketCandleBootstrapper currentMarketCandleBootstrapper,
            long streamTimeoutMs
    ) {
        this(
                getMarketSummaryService,
                getMarketCandlesService,
                marketRealtimeSseBroker,
                marketCandleRealtimeSseBroker,
                realtimeMarketCandleProjector,
                currentMarketCandleBootstrapper,
                null,
                streamTimeoutMs
        );
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


    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter unifiedStream(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) String clientKey
    ) {
        if (unifiedMarketStreamOpener == null) {
            throw new IllegalStateException("Unified market stream dependencies are not configured");
        }
        SseEmitter emitter = createEmitter();
        unifiedMarketStreamOpener.open(symbol, interval, clientKey, emitter);
        return emitter;
    }


    @GetMapping(value = "/summary/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter summaryStream(
            @RequestParam String symbols,
            @RequestParam(required = false) String clientKey
    ) {
        Set<String> resolvedSymbols = parseSummarySymbols(symbols);
        String resolvedClientKey = SseClientKey.resolve(clientKey).value();
        MarketRealtimeSseBroker.SseSubscriptionPermit permit = marketRealtimeSseBroker.reserve(
                resolvedSymbols,
                resolvedClientKey
        );
        SseEmitter emitter = createEmitter();
        try {
            boolean initialSendSucceeded = true;
            for (String symbol : resolvedSymbols) {
                MarketSummaryResult currentMarket = getMarketSummaryService.getMarket(new GetMarketQuery(symbol));
                if (!sendEvent(emitter, currentMarket)) {
                    initialSendSucceeded = false;
                    break;
                }
            }

            if (initialSendSucceeded) {
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

    public SseEmitter stream(String symbol) {
        return stream(symbol, null);
    }

    @GetMapping(value = "/{symbol}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @PathVariable String symbol,
            @RequestParam(required = false) String clientKey
    ) {
        String resolvedClientKey = SseClientKey.resolve(clientKey).value();
        MarketRealtimeSseBroker.SseSubscriptionPermit permit = marketRealtimeSseBroker.reserve(symbol, resolvedClientKey);
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

    public SseEmitter candleStream(String symbol, String interval) {
        return candleStream(symbol, interval, null);
    }

    @GetMapping(value = "/{symbol}/candles/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter candleStream(
            @PathVariable String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) String clientKey
    ) {
        String resolvedClientKey = SseClientKey.resolve(clientKey).value();
        MarketCandleInterval candleInterval = MarketCandleInterval.from(interval);
        MarketCandleRealtimeSseBroker.SubscriptionKey key =
                new MarketCandleRealtimeSseBroker.SubscriptionKey(symbol, candleInterval);
        MarketCandleRealtimeSseBroker.SseSubscriptionPermit permit = marketCandleRealtimeSseBroker.reserve(
                key,
                resolvedClientKey
        );
        SseEmitter emitter = createEmitter();
        try {
            if (currentMarketCandleBootstrapper != null) {
                currentMarketCandleBootstrapper.bootstrapIfNeeded(symbol, candleInterval);
            }
            boolean initialSendSucceeded = realtimeMarketCandleProjector.latest(symbol, candleInterval)
                    .map(candle -> sendCandleEvent(emitter, candle))
                    .orElse(true);
            if (initialSendSucceeded) {
                marketCandleRealtimeSseBroker.register(permit, emitter);
            } else {
                marketCandleRealtimeSseBroker.release(permit);
            }
            return emitter;
        } catch (RuntimeException exception) {
            marketCandleRealtimeSseBroker.release(permit);
            throw exception;
        }
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
