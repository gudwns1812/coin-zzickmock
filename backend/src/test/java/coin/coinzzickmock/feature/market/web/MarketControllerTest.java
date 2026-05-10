package coin.coinzzickmock.feature.market.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.query.GetMarketQuery;
import coin.coinzzickmock.feature.market.application.realtime.CurrentMarketCandleBootstrapper;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleProjector;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleUpdate;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketDataStore;
import coin.coinzzickmock.feature.market.application.service.GetMarketCandlesService;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.application.service.GetMarketSummaryService;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class MarketControllerTest {
    private static final long SSE_TIMEOUT_MS = 30_000L;

    @Test
    void doesNotDependOnMarketRealtimeFeedDirectly() {
        boolean dependsOnMarketRealtimeFeed = Arrays.stream(MarketController.class.getDeclaredFields())
                .map(field -> field.getType())
                .anyMatch(type -> type.getSimpleName().equals("MarketRealtimeFeed"));

        assertFalse(dependsOnMarketRealtimeFeed);
    }

    @Test
    void completesStreamSilentlyWhenInitialSendFails() {
        GetMarketSummaryService service = mock(GetMarketSummaryService.class);
        MarketRealtimeSseBroker broker = mock(MarketRealtimeSseBroker.class);
        MarketRealtimeSseBroker.SseSubscriptionPermit permit = mock(MarketRealtimeSseBroker.SseSubscriptionPermit.class);
        MarketSummaryResult market = new MarketSummaryResult(
                "BTCUSDT",
                "Bitcoin Perpetual",
                74000,
                74010,
                74005,
                0.0001,
                0.2
        );
        when(service.getMarket(org.mockito.ArgumentMatchers.any())).thenReturn(market);
        when(broker.reserve(eq("BTCUSDT"), org.mockito.ArgumentMatchers.anyString())).thenReturn(permit);

        MarketController controller = new TestableMarketController(service, mock(GetMarketCandlesService.class), broker, SSE_TIMEOUT_MS);

        SseEmitter emitter = controller.stream("BTCUSDT");

        assertTrue(((FailingSseEmitter) emitter).completed);
        verify(broker).reserve(eq("BTCUSDT"), org.mockito.ArgumentMatchers.anyString());
        verify(broker).release(permit);
        verify(broker, never()).register(eq(permit), any(SseEmitter.class));
    }

    @Test
    void registersEmitterWhenInitialSendSucceeds() {
        GetMarketSummaryService service = mock(GetMarketSummaryService.class);
        MarketRealtimeSseBroker broker = mock(MarketRealtimeSseBroker.class);
        MarketRealtimeSseBroker.SseSubscriptionPermit permit = mock(MarketRealtimeSseBroker.SseSubscriptionPermit.class);
        MarketSummaryResult market = new MarketSummaryResult(
                "BTCUSDT",
                "Bitcoin Perpetual",
                74000,
                74010,
                74005,
                0.0001,
                0.2
        );
        when(service.getMarket(org.mockito.ArgumentMatchers.any())).thenReturn(market);
        when(broker.reserve(eq("BTCUSDT"), org.mockito.ArgumentMatchers.anyString())).thenReturn(permit);

        MarketController controller = controller(service, mock(GetMarketCandlesService.class), broker, SSE_TIMEOUT_MS);

        SseEmitter emitter = controller.stream("BTCUSDT");

        assertTrue(emitter != null);
        verify(broker).reserve(eq("BTCUSDT"), org.mockito.ArgumentMatchers.anyString());
        verify(broker).register(eq(permit), any(SseEmitter.class));
    }

    @Test
    void registersOneSummaryEmitterForMultipleSymbols() {
        GetMarketSummaryService service = mock(GetMarketSummaryService.class);
        MarketRealtimeSseBroker broker = mock(MarketRealtimeSseBroker.class);
        MarketRealtimeSseBroker.SseSubscriptionPermit permit = mock(MarketRealtimeSseBroker.SseSubscriptionPermit.class);
        when(broker.reserve(eq(Set.of("BTCUSDT", "ETHUSDT")), eq("tab-1"))).thenReturn(permit);
        when(service.getMarket(any())).thenAnswer(invocation -> {
            GetMarketQuery query = invocation.getArgument(0);
            return new MarketSummaryResult(
                    query.symbol(),
                    query.symbol() + " Perpetual",
                    74000,
                    74010,
                    74005,
                    0.0001,
                    0.2
            );
        });
        MarketController controller = controller(service, mock(GetMarketCandlesService.class), broker, SSE_TIMEOUT_MS);

        SseEmitter emitter = controller.summaryStream("BTCUSDT,ETHUSDT", "tab-1");

        assertTrue(emitter != null);
        verify(broker).reserve(eq(Set.of("BTCUSDT", "ETHUSDT")), eq("tab-1"));
        verify(service).getMarket(argThat(query -> query.symbol().equals("BTCUSDT")));
        verify(service).getMarket(argThat(query -> query.symbol().equals("ETHUSDT")));
        verify(broker).register(eq(permit), any(SseEmitter.class));
    }

    @Test
    void releasesSummaryPermitWhenMultiSymbolInitialSendFails() {
        GetMarketSummaryService service = mock(GetMarketSummaryService.class);
        MarketRealtimeSseBroker broker = mock(MarketRealtimeSseBroker.class);
        MarketRealtimeSseBroker.SseSubscriptionPermit permit = mock(MarketRealtimeSseBroker.SseSubscriptionPermit.class);
        when(broker.reserve(eq(Set.of("BTCUSDT", "ETHUSDT")), eq("tab-1"))).thenReturn(permit);
        when(service.getMarket(any())).thenAnswer(invocation -> {
            GetMarketQuery query = invocation.getArgument(0);
            return new MarketSummaryResult(
                    query.symbol(),
                    query.symbol() + " Perpetual",
                    74000,
                    74010,
                    74005,
                    0.0001,
                    0.2
            );
        });
        MarketController controller = new TestableMarketController(
                service,
                mock(GetMarketCandlesService.class),
                broker,
                SSE_TIMEOUT_MS
        );

        SseEmitter emitter = controller.summaryStream("BTCUSDT,ETHUSDT", "tab-1");

        assertTrue(((FailingSseEmitter) emitter).completed);
        verify(broker).reserve(eq(Set.of("BTCUSDT", "ETHUSDT")), eq("tab-1"));
        verify(broker).release(permit);
        verify(broker, never()).register(eq(permit), any(SseEmitter.class));
    }

    @Test
    void doesNotRegisterCandleStreamWhenInitialSendFails() {
        GetMarketSummaryService summaryService = mock(GetMarketSummaryService.class);
        GetMarketCandlesService candleService = mock(GetMarketCandlesService.class);
        MarketRealtimeSseBroker marketBroker = mock(MarketRealtimeSseBroker.class);
        MarketCandleRealtimeSseBroker candleBroker = mock(MarketCandleRealtimeSseBroker.class);
        MarketCandleRealtimeSseBroker.SubscriptionKey key = new MarketCandleRealtimeSseBroker.SubscriptionKey(
                "BTCUSDT",
                MarketCandleInterval.ONE_MINUTE
        );
        MarketCandleRealtimeSseBroker.SseSubscriptionPermit permit =
                mock(MarketCandleRealtimeSseBroker.SseSubscriptionPermit.class);
        when(candleBroker.reserve(eq(key), org.mockito.ArgumentMatchers.anyString())).thenReturn(permit);
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        RealtimeMarketCandleProjector projector = new RealtimeMarketCandleProjector(store);
        Instant open = Instant.parse("2026-04-30T04:00:00Z");
        store.acceptCandle(new RealtimeMarketCandleUpdate(
                "BTCUSDT",
                MarketCandleInterval.ONE_MINUTE,
                open,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(105),
                BigDecimal.valueOf(99),
                BigDecimal.valueOf(102),
                BigDecimal.ONE,
                BigDecimal.valueOf(102),
                BigDecimal.valueOf(102),
                open.plusSeconds(1),
                open.plusSeconds(1)
        ));
        MarketController controller = new TestableMarketController(
                summaryService,
                candleService,
                marketBroker,
                candleBroker,
                projector,
                SSE_TIMEOUT_MS
        );

        SseEmitter emitter = controller.candleStream("BTCUSDT", "1m");

        assertTrue(((FailingSseEmitter) emitter).completed);
        verify(candleBroker).reserve(eq(key), org.mockito.ArgumentMatchers.anyString());
        verify(candleBroker).release(permit);
        verify(candleBroker, never()).register(eq(permit), any(SseEmitter.class));
    }

    @Test
    void bootstrapsCurrentCandleBeforeCandleStreamInitialSend() {
        GetMarketSummaryService summaryService = mock(GetMarketSummaryService.class);
        GetMarketCandlesService candleService = mock(GetMarketCandlesService.class);
        MarketRealtimeSseBroker marketBroker = mock(MarketRealtimeSseBroker.class);
        MarketCandleRealtimeSseBroker candleBroker = mock(MarketCandleRealtimeSseBroker.class);
        CurrentMarketCandleBootstrapper bootstrapper = mock(CurrentMarketCandleBootstrapper.class);
        MarketCandleRealtimeSseBroker.SubscriptionKey key = new MarketCandleRealtimeSseBroker.SubscriptionKey(
                "BTCUSDT",
                MarketCandleInterval.ONE_MINUTE
        );
        MarketCandleRealtimeSseBroker.SseSubscriptionPermit permit =
                mock(MarketCandleRealtimeSseBroker.SseSubscriptionPermit.class);
        when(candleBroker.reserve(eq(key), org.mockito.ArgumentMatchers.anyString())).thenReturn(permit);
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        RealtimeMarketCandleProjector projector = new RealtimeMarketCandleProjector(store);
        Instant open = Instant.parse("2026-04-30T04:00:00Z");
        org.mockito.Mockito.doAnswer(invocation -> {
            store.acceptBootstrapCandle(new RealtimeMarketCandleUpdate(
                    "BTCUSDT",
                    MarketCandleInterval.ONE_MINUTE,
                    open,
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(105),
                    BigDecimal.valueOf(99),
                    BigDecimal.valueOf(102),
                    BigDecimal.ONE,
                    BigDecimal.valueOf(102),
                    BigDecimal.valueOf(102),
                    open.plusSeconds(1),
                    open.plusSeconds(1)
            ));
            return true;
        }).when(bootstrapper).bootstrapIfNeeded("BTCUSDT", MarketCandleInterval.ONE_MINUTE);
        MarketController controller = new MarketController(
                summaryService,
                candleService,
                marketBroker,
                candleBroker,
                projector,
                bootstrapper,
                SSE_TIMEOUT_MS
        );

        controller.candleStream("BTCUSDT", "1m");

        org.mockito.InOrder inOrder = inOrder(candleBroker, bootstrapper);
        inOrder.verify(candleBroker).reserve(eq(key), org.mockito.ArgumentMatchers.anyString());
        inOrder.verify(bootstrapper).bootstrapIfNeeded("BTCUSDT", MarketCandleInterval.ONE_MINUTE);
        inOrder.verify(candleBroker).register(eq(permit), any(SseEmitter.class));
        verify(candleBroker, never()).release(permit);
    }

    @Test
    void failsCandleStreamBeforeEmitterCreationWhenCapacityIsExceeded() {
        GetMarketSummaryService summaryService = mock(GetMarketSummaryService.class);
        GetMarketCandlesService candleService = mock(GetMarketCandlesService.class);
        MarketRealtimeSseBroker marketBroker = mock(MarketRealtimeSseBroker.class);
        MarketCandleRealtimeSseBroker candleBroker = mock(MarketCandleRealtimeSseBroker.class);
        RealtimeMarketCandleProjector projector = new RealtimeMarketCandleProjector(new RealtimeMarketDataStore());
        MarketController controller = new TestableMarketController(
                summaryService,
                candleService,
                marketBroker,
                candleBroker,
                projector,
                SSE_TIMEOUT_MS
        );
        MarketCandleRealtimeSseBroker.SubscriptionKey key = new MarketCandleRealtimeSseBroker.SubscriptionKey(
                "BTCUSDT",
                MarketCandleInterval.ONE_MINUTE
        );
        CoreException exception = new CoreException(ErrorCode.TOO_MANY_REQUESTS);
        org.mockito.Mockito.doThrow(exception).when(candleBroker).reserve(eq(key), org.mockito.ArgumentMatchers.anyString());

        CoreException thrown = assertThrows(CoreException.class, () -> controller.candleStream("BTCUSDT", "1m"));

        assertTrue(thrown.errorCode() == ErrorCode.TOO_MANY_REQUESTS);
        verify(candleBroker).reserve(eq(key), org.mockito.ArgumentMatchers.anyString());
        verify(candleBroker, never()).register(any(), any());
    }

    @Test
    void failsBeforeEmitterCreationWhenCapacityIsExceeded() {
        GetMarketSummaryService service = mock(GetMarketSummaryService.class);
        MarketRealtimeSseBroker broker = mock(MarketRealtimeSseBroker.class);
        MarketController controller = controller(service, mock(GetMarketCandlesService.class), broker, SSE_TIMEOUT_MS);
        CoreException exception = new CoreException(ErrorCode.TOO_MANY_REQUESTS);
        org.mockito.Mockito.doThrow(exception).when(broker).reserve(eq("BTCUSDT"), org.mockito.ArgumentMatchers.anyString());

        CoreException thrown = assertThrows(CoreException.class, () -> controller.stream("BTCUSDT"));

        assertTrue(thrown.errorCode() == ErrorCode.TOO_MANY_REQUESTS);
        verify(service, never()).getMarket(any());
        verify(broker).reserve(eq("BTCUSDT"), org.mockito.ArgumentMatchers.anyString());
        verify(broker, never()).register(any(), any());
    }

    @Test
    void failsWithMarketNotFoundBeforeEmitterRegistration() {
        GetMarketSummaryService service = mock(GetMarketSummaryService.class);
        MarketRealtimeSseBroker broker = mock(MarketRealtimeSseBroker.class);
        MarketRealtimeSseBroker.SseSubscriptionPermit permit = mock(MarketRealtimeSseBroker.SseSubscriptionPermit.class);
        MarketController controller = controller(service, mock(GetMarketCandlesService.class), broker, SSE_TIMEOUT_MS);
        when(broker.reserve(eq("UNSUPPORTED"), org.mockito.ArgumentMatchers.anyString())).thenReturn(permit);
        when(service.getMarket(any())).thenThrow(new CoreException(ErrorCode.MARKET_NOT_FOUND));

        CoreException thrown = assertThrows(CoreException.class, () -> controller.stream("UNSUPPORTED"));

        assertTrue(thrown.errorCode() == ErrorCode.MARKET_NOT_FOUND);
        verify(broker).reserve(eq("UNSUPPORTED"), org.mockito.ArgumentMatchers.anyString());
        verify(broker).release(permit);
        verify(broker, never()).register(eq(permit), any(SseEmitter.class));
    }

    @Test
    void createsEmitterWithFiniteTimeout() {
        MarketController controller = controller(
                mock(GetMarketSummaryService.class),
                mock(GetMarketCandlesService.class),
                mock(MarketRealtimeSseBroker.class),
                SSE_TIMEOUT_MS
        );

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
        MarketRealtimeSseBroker broker = mock(MarketRealtimeSseBroker.class);
        MarketController controller = controller(summaryService, candleService, broker, SSE_TIMEOUT_MS);
        Instant before = Instant.parse("2026-04-21T00:05:00Z");

        when(candleService.getCandles(any())).thenReturn(java.util.List.of());

        controller.candles("BTCUSDT", "1m", 120, before);

        verify(candleService).getCandles(argThat(query ->
                query.symbol().equals("BTCUSDT")
                        && query.interval().equals("1m")
                        && query.limit().equals(120)
                        && before.equals(query.beforeOpenTime())
        ));
    }

    private static class TestableMarketController extends MarketController {
        private TestableMarketController(
                GetMarketSummaryService service,
                GetMarketCandlesService candleService,
                MarketRealtimeSseBroker broker,
                long timeoutMs
        ) {
            super(service, candleService, broker, candleBroker(), candleProjector(), timeoutMs);
        }

        private TestableMarketController(
                GetMarketSummaryService service,
                GetMarketCandlesService candleService,
                MarketRealtimeSseBroker marketBroker,
                MarketCandleRealtimeSseBroker candleBroker,
                RealtimeMarketCandleProjector projector,
                long timeoutMs
        ) {
            super(service, candleService, marketBroker, candleBroker, projector, timeoutMs);
        }

        @Override
        SseEmitter createEmitter() {
            return new FailingSseEmitter();
        }
    }

    private static MarketController controller(
            GetMarketSummaryService service,
            GetMarketCandlesService candleService,
            MarketRealtimeSseBroker broker,
            long timeoutMs
    ) {
        return new MarketController(service, candleService, broker, candleBroker(), candleProjector(), timeoutMs);
    }

    private static MarketCandleRealtimeSseBroker candleBroker() {
        return new MarketCandleRealtimeSseBroker(Runnable::run, candleProjector());
    }

    private static RealtimeMarketCandleProjector candleProjector() {
        return new RealtimeMarketCandleProjector(new RealtimeMarketDataStore());
    }

    private static class FailingSseEmitter extends SseEmitter {
        private boolean completed;

        private FailingSseEmitter() {
            super(0L);
        }

        @Override
        public synchronized void send(Object object) throws IOException {
            throw new IOException("client disconnected");
        }

        @Override
        public synchronized void complete() {
            this.completed = true;
            super.complete();
        }
    }
}
