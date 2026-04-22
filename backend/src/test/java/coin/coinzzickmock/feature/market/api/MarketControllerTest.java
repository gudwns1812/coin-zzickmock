package coin.coinzzickmock.feature.market.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.service.GetMarketCandlesService;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.application.service.GetMarketSummaryService;
import java.io.IOException;
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

    private static class TestableMarketController extends MarketController {
        private TestableMarketController(
                GetMarketSummaryService service,
                GetMarketCandlesService candleService,
                MarketRealtimeSseBroker broker,
                long timeoutMs
        ) {
            super(service, candleService, broker, timeoutMs);
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
