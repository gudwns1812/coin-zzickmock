package coin.coinzzickmock.feature.market.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
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
        when(broker.reserve("BTCUSDT")).thenReturn(permit);

        MarketController controller = new TestableMarketController(service, mock(GetMarketCandlesService.class), broker, SSE_TIMEOUT_MS);

        SseEmitter emitter = controller.stream("BTCUSDT");

        assertTrue(((FailingSseEmitter) emitter).completed);
        verify(broker).reserve("BTCUSDT");
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
        when(broker.reserve("BTCUSDT")).thenReturn(permit);

        MarketController controller = new MarketController(service, mock(GetMarketCandlesService.class), broker, SSE_TIMEOUT_MS);

        SseEmitter emitter = controller.stream("BTCUSDT");

        assertTrue(emitter != null);
        verify(broker).reserve("BTCUSDT");
        verify(broker).register(eq(permit), any(SseEmitter.class));
    }

    @Test
    void doesNotRegisterCandleStreamWhenInitialSendFails() {
        GetMarketSummaryService summaryService = mock(GetMarketSummaryService.class);
        GetMarketCandlesService candleService = mock(GetMarketCandlesService.class);
        MarketRealtimeSseBroker marketBroker = mock(MarketRealtimeSseBroker.class);
        MarketCandleRealtimeSseBroker candleBroker = mock(MarketCandleRealtimeSseBroker.class);
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
        verify(candleBroker, never()).register(eq("BTCUSDT"), eq(MarketCandleInterval.ONE_MINUTE), any(SseEmitter.class));
    }

    @Test
    void failsBeforeEmitterCreationWhenCapacityIsExceeded() {
        GetMarketSummaryService service = mock(GetMarketSummaryService.class);
        MarketRealtimeSseBroker broker = mock(MarketRealtimeSseBroker.class);
        MarketController controller = new MarketController(service, mock(GetMarketCandlesService.class), broker, SSE_TIMEOUT_MS);
        CoreException exception = new CoreException(ErrorCode.TOO_MANY_REQUESTS);
        org.mockito.Mockito.doThrow(exception).when(broker).reserve("BTCUSDT");

        CoreException thrown = assertThrows(CoreException.class, () -> controller.stream("BTCUSDT"));

        assertTrue(thrown.errorCode() == ErrorCode.TOO_MANY_REQUESTS);
        verify(service, never()).getMarket(any());
        verify(broker).reserve("BTCUSDT");
        verify(broker, never()).register(any(), any());
    }

    @Test
    void failsWithMarketNotFoundBeforeEmitterRegistration() {
        GetMarketSummaryService service = mock(GetMarketSummaryService.class);
        MarketRealtimeSseBroker broker = mock(MarketRealtimeSseBroker.class);
        MarketRealtimeSseBroker.SseSubscriptionPermit permit = mock(MarketRealtimeSseBroker.SseSubscriptionPermit.class);
        MarketController controller = new MarketController(service, mock(GetMarketCandlesService.class), broker, SSE_TIMEOUT_MS);
        when(broker.reserve("UNSUPPORTED")).thenReturn(permit);
        when(service.getMarket(any())).thenThrow(new CoreException(ErrorCode.MARKET_NOT_FOUND));

        CoreException thrown = assertThrows(CoreException.class, () -> controller.stream("UNSUPPORTED"));

        assertTrue(thrown.errorCode() == ErrorCode.MARKET_NOT_FOUND);
        verify(broker).reserve("UNSUPPORTED");
        verify(broker).release(permit);
        verify(broker, never()).register(eq(permit), any(SseEmitter.class));
    }

    @Test
    void createsEmitterWithFiniteTimeout() {
        MarketController controller = new MarketController(
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
        MarketController controller = new MarketController(summaryService, candleService, broker, SSE_TIMEOUT_MS);
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
            super(service, candleService, broker, timeoutMs);
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
