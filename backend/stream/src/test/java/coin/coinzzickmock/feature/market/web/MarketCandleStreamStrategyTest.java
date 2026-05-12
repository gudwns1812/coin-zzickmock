package coin.coinzzickmock.feature.market.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.common.web.SseSubscriptionLimitExceededException;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class MarketCandleStreamStrategyTest {
    @Test
    void bootstrapsAndRegistersAfterInitialSend() {
        MarketCandleRealtimeSseBroker broker = mock(MarketCandleRealtimeSseBroker.class);
        MarketCurrentCandleBootstrapper bootstrapper = mock(MarketCurrentCandleBootstrapper.class);
        MarketCandleSnapshotReader reader = (symbol, interval) -> Optional.of(candle());
        MarketCandleRealtimeSseBroker.SubscriptionKey key = key();
        MarketCandleRealtimeSseBroker.SseSubscriptionPermit permit = mock(MarketCandleRealtimeSseBroker.SseSubscriptionPermit.class);
        when(broker.reserve(eq(key), eq("tab-1"))).thenReturn(permit);
        SseEmitter emitter = new SseEmitter();

        new MarketCandleStreamStrategy(broker, reader, bootstrapper)
                .open(MarketSseStreamRequest.candle("BTCUSDT", "1m", "tab-1", emitter));

        InOrder inOrder = inOrder(broker, bootstrapper);
        inOrder.verify(broker).reserve(eq(key), eq("tab-1"));
        inOrder.verify(bootstrapper).bootstrapIfNeeded("BTCUSDT", "1m");
        inOrder.verify(broker).register(permit, emitter);
        verify(broker, never()).release(permit);
    }

    @Test
    void releasesPermitWhenInitialSendFails() {
        MarketCandleRealtimeSseBroker broker = mock(MarketCandleRealtimeSseBroker.class);
        MarketCurrentCandleBootstrapper bootstrapper = mock(MarketCurrentCandleBootstrapper.class);
        MarketCandleSnapshotReader reader = (symbol, interval) -> Optional.of(candle());
        MarketCandleRealtimeSseBroker.SseSubscriptionPermit permit = mock(MarketCandleRealtimeSseBroker.SseSubscriptionPermit.class);
        when(broker.reserve(eq(key()), eq("tab-1"))).thenReturn(permit);
        FailingSseEmitter emitter = new FailingSseEmitter();

        new MarketCandleStreamStrategy(broker, reader, bootstrapper)
                .open(MarketSseStreamRequest.candle("BTCUSDT", "1m", "tab-1", emitter));

        assertTrue(emitter.completed);
        verify(broker).release(permit);
        verify(broker, never()).register(eq(permit), any(SseEmitter.class));
    }

    @Test
    void capacityFailurePropagatesBeforeBootstrap() {
        MarketCandleRealtimeSseBroker broker = mock(MarketCandleRealtimeSseBroker.class);
        MarketCurrentCandleBootstrapper bootstrapper = mock(MarketCurrentCandleBootstrapper.class);
        MarketCandleSnapshotReader reader = (symbol, interval) -> Optional.empty();
        SseSubscriptionLimitExceededException exception = new SseSubscriptionLimitExceededException("key_limit");
        org.mockito.Mockito.doThrow(exception).when(broker).reserve(eq(key()), eq("tab-1"));

        org.junit.jupiter.api.Assertions.assertThrows(SseSubscriptionLimitExceededException.class, () ->
                new MarketCandleStreamStrategy(broker, reader, bootstrapper)
                        .open(MarketSseStreamRequest.candle("BTCUSDT", "1m", "tab-1", new SseEmitter()))
        );

        verify(bootstrapper, never()).bootstrapIfNeeded(any(), any());
        verify(broker, never()).register(any(), any());
    }

    private static MarketCandleRealtimeSseBroker.SubscriptionKey key() {
        return new MarketCandleRealtimeSseBroker.SubscriptionKey("BTCUSDT", "1m");
    }

    private static MarketCandleResponse candle() {
        Instant open = Instant.parse("2026-04-30T04:00:00Z");
        return new MarketCandleResponse(open, open.plusSeconds(60), 1, 2, 0.5, 1.5, 100);
    }

    private static class FailingSseEmitter extends SseEmitter {
        boolean completed;
        @Override public void send(Object object) throws IOException { throw new IOException("failed"); }
        @Override public void complete() { completed = true; super.complete(); }
    }
}
