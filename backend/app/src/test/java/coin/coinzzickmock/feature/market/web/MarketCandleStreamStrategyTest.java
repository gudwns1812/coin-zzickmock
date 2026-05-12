package coin.coinzzickmock.feature.market.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.realtime.CurrentMarketCandleBootstrapper;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleProjector;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleUpdate;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketDataStore;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class MarketCandleStreamStrategyTest {

    @Test
    void bootstrapsAndRegistersAfterInitialSend() {
        MarketCandleRealtimeSseBroker broker = mock(MarketCandleRealtimeSseBroker.class);
        CurrentMarketCandleBootstrapper bootstrapper = mock(CurrentMarketCandleBootstrapper.class);
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        RealtimeMarketCandleProjector projector = new RealtimeMarketCandleProjector(store);
        acceptCandle(store);
        MarketCandleRealtimeSseBroker.SubscriptionKey key = key();
        MarketCandleRealtimeSseBroker.SseSubscriptionPermit permit = mock(MarketCandleRealtimeSseBroker.SseSubscriptionPermit.class);
        when(broker.reserve(eq(key), eq("tab-1"))).thenReturn(permit);
        SseEmitter emitter = new SseEmitter();

        new MarketCandleStreamStrategy(broker, projector, bootstrapper)
                .open(MarketSseStreamRequest.candle("BTCUSDT", MarketCandleInterval.ONE_MINUTE, "tab-1", emitter));

        InOrder inOrder = inOrder(broker, bootstrapper);
        inOrder.verify(broker).reserve(eq(key), eq("tab-1"));
        inOrder.verify(bootstrapper).bootstrapIfNeeded("BTCUSDT", MarketCandleInterval.ONE_MINUTE);
        inOrder.verify(broker).register(permit, emitter);
        verify(broker, never()).release(permit);
    }

    @Test
    void releasesPermitWhenInitialSendFails() {
        MarketCandleRealtimeSseBroker broker = mock(MarketCandleRealtimeSseBroker.class);
        CurrentMarketCandleBootstrapper bootstrapper = mock(CurrentMarketCandleBootstrapper.class);
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        RealtimeMarketCandleProjector projector = new RealtimeMarketCandleProjector(store);
        acceptCandle(store);
        MarketCandleRealtimeSseBroker.SseSubscriptionPermit permit = mock(MarketCandleRealtimeSseBroker.SseSubscriptionPermit.class);
        when(broker.reserve(eq(key()), eq("tab-1"))).thenReturn(permit);
        FailingSseEmitter emitter = new FailingSseEmitter();

        new MarketCandleStreamStrategy(broker, projector, bootstrapper)
                .open(MarketSseStreamRequest.candle("BTCUSDT", MarketCandleInterval.ONE_MINUTE, "tab-1", emitter));

        assertTrue(emitter.completed);
        verify(broker).release(permit);
        verify(broker, never()).register(eq(permit), any(SseEmitter.class));
    }

    @Test
    void capacityFailurePropagatesBeforeBootstrap() {
        MarketCandleRealtimeSseBroker broker = mock(MarketCandleRealtimeSseBroker.class);
        CurrentMarketCandleBootstrapper bootstrapper = mock(CurrentMarketCandleBootstrapper.class);
        RealtimeMarketCandleProjector projector = new RealtimeMarketCandleProjector(new RealtimeMarketDataStore());
        CoreException exception = new CoreException(ErrorCode.TOO_MANY_REQUESTS);
        org.mockito.Mockito.doThrow(exception).when(broker).reserve(eq(key()), eq("tab-1"));

        CoreException thrown = org.junit.jupiter.api.Assertions.assertThrows(CoreException.class, () ->
                new MarketCandleStreamStrategy(broker, projector, bootstrapper)
                        .open(MarketSseStreamRequest.candle("BTCUSDT", MarketCandleInterval.ONE_MINUTE, "tab-1", new SseEmitter()))
        );

        assertTrue(thrown.errorCode() == ErrorCode.TOO_MANY_REQUESTS);
        verify(bootstrapper, never()).bootstrapIfNeeded(any(), any());
        verify(broker, never()).register(any(), any());
    }

    private static MarketCandleRealtimeSseBroker.SubscriptionKey key() {
        return new MarketCandleRealtimeSseBroker.SubscriptionKey("BTCUSDT", MarketCandleInterval.ONE_MINUTE);
    }

    private static void acceptCandle(RealtimeMarketDataStore store) {
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
