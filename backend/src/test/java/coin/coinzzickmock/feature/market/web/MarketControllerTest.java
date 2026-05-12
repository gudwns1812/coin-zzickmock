package coin.coinzzickmock.feature.market.web;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.application.service.GetMarketCandlesService;
import coin.coinzzickmock.feature.market.application.service.GetMarketSummaryService;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class MarketControllerTest {
    private static final long SSE_TIMEOUT_MS = 30_000L;

    @Test
    void summaryEndpointRoutesThroughMarketSseRouter() {
        MarketSseStreamRouter router = mock(MarketSseStreamRouter.class);
        MarketController controller = controller(router);

        SseEmitter emitter = controller.summaryStream("BTCUSDT, ETHUSDT", " tab-1 ");

        verify(router).open(argThat(request ->
                request.kind() == MarketSseStreamKind.SUMMARY
                        && request.summarySymbols().containsAll(List.of("BTCUSDT", "ETHUSDT"))
                        && request.clientKey().equals("tab-1")
                        && request.emitter() == emitter
        ));
    }

    @Test
    void symbolSummaryEndpointRoutesThroughMarketSseRouter() {
        MarketSseStreamRouter router = mock(MarketSseStreamRouter.class);
        MarketController controller = controller(router);

        SseEmitter emitter = controller.stream("BTCUSDT", "tab-1");

        verify(router).open(argThat(request ->
                request.kind() == MarketSseStreamKind.SUMMARY
                        && request.summarySymbols().equals(Set.of("BTCUSDT"))
                        && request.clientKey().equals("tab-1")
                        && request.emitter() == emitter
        ));
    }

    @Test
    void candleEndpointRoutesThroughMarketSseRouter() {
        MarketSseStreamRouter router = mock(MarketSseStreamRouter.class);
        MarketController controller = controller(router);

        SseEmitter emitter = controller.candleStream("BTCUSDT", "1m", "tab-1");

        verify(router).open(argThat(request ->
                request.kind() == MarketSseStreamKind.CANDLE
                        && request.activeSymbol().equals("BTCUSDT")
                        && request.candleInterval() == MarketCandleInterval.ONE_MINUTE
                        && request.clientKey().equals("tab-1")
                        && request.emitter() == emitter
        ));
    }

    @Test
    void unifiedEndpointRoutesThroughMarketSseRouter() {
        MarketSseStreamRouter router = mock(MarketSseStreamRouter.class);
        MarketController controller = controller(router);

        SseEmitter emitter = controller.unifiedStream("BTCUSDT", "1m", "tab-1");

        verify(router).open(argThat(request ->
                request.kind() == MarketSseStreamKind.UNIFIED
                        && request.activeSymbol().equals("BTCUSDT")
                        && request.candleInterval() == MarketCandleInterval.ONE_MINUTE
                        && request.clientKey().equals("tab-1")
                        && request.emitter() == emitter
        ));
    }

    @Test
    void createsEmitterWithFiniteTimeout() {
        MarketController controller = controller(mock(MarketSseStreamRouter.class));

        SseEmitter emitter = controller.createEmitter();

        assertInstanceOf(SseEmitter.class, emitter);
        assertTrue(emitter.getTimeout() != null);
        assertTrue(emitter.getTimeout() > 0L);
        assertTrue(emitter.getTimeout().equals(SSE_TIMEOUT_MS));
    }

    @Test
    void mapsFundingScheduleFieldsToMarketSummaryResponse() {
        Instant serverTime = Instant.parse("2026-04-26T23:59:30Z");
        Instant nextFundingAt = Instant.parse("2026-04-27T00:00:00Z");
        MarketSummaryResult market = new MarketSummaryResult(
                "BTCUSDT",
                "Bitcoin Perpetual",
                74000,
                74010,
                74005,
                0.0001,
                0.2,
                6_400_000_000d,
                serverTime,
                nextFundingAt,
                8
        );

        MarketSummaryResponse response = MarketSummaryResponse.from(market);

        assertTrue(response.turnover24hUsdt() == 6_400_000_000d);
        assertTrue(response.volume24h() == 6_400_000_000d);
        assertTrue(response.serverTime().equals(serverTime));
        assertTrue(response.nextFundingAt().equals(nextFundingAt));
        assertTrue(response.fundingIntervalHours() == 8);
    }

    @Test
    void passesBeforeCursorToCandleQuery() {
        GetMarketSummaryService summaryService = mock(GetMarketSummaryService.class);
        GetMarketCandlesService candleService = mock(GetMarketCandlesService.class);
        MarketController controller = new MarketController(
                summaryService,
                candleService,
                mock(MarketSseStreamRouter.class),
                SSE_TIMEOUT_MS
        );
        Instant before = Instant.parse("2026-04-21T00:05:00Z");

        when(candleService.getCandles(org.mockito.Mockito.any())).thenReturn(List.of());

        controller.candles("BTCUSDT", "1m", 120, before);

        verify(candleService).getCandles(argThat(query ->
                query.symbol().equals("BTCUSDT")
                        && query.interval().equals("1m")
                        && query.limit().equals(120)
                        && before.equals(query.beforeOpenTime())
        ));
    }

    private static MarketController controller(MarketSseStreamRouter router) {
        return new MarketController(
                mock(GetMarketSummaryService.class),
                mock(GetMarketCandlesService.class),
                router,
                SSE_TIMEOUT_MS
        );
    }
}
