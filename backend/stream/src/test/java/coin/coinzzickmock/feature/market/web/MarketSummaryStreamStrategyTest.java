package coin.coinzzickmock.feature.market.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class MarketSummaryStreamStrategyTest {
    @Test
    void registersSingleSymbolAfterInitialSend() {
        MarketSummarySnapshotReader reader = mock(MarketSummarySnapshotReader.class);
        MarketRealtimeSseBroker broker = mock(MarketRealtimeSseBroker.class);
        MarketRealtimeSseBroker.SseSubscriptionPermit permit = mock(MarketRealtimeSseBroker.SseSubscriptionPermit.class);
        when(reader.getMarket("BTCUSDT")).thenReturn(summary("BTCUSDT"));
        when(broker.reserve(org.mockito.ArgumentMatchers.<Set<String>>eq(Set.of("BTCUSDT")), eq("tab-1")))
                .thenReturn(permit);
        SseEmitter emitter = new SseEmitter();

        new MarketSummaryStreamStrategy(reader, broker)
                .open(MarketSseStreamRequest.summary(Set.of("BTCUSDT"), "tab-1", emitter));

        verify(reader).getMarket("BTCUSDT");
        verify(broker).register(permit, emitter);
    }

    @Test
    void registersMultiSymbolSummaryAfterInitialSends() {
        MarketSummarySnapshotReader reader = symbol -> summary(symbol);
        MarketRealtimeSseBroker broker = mock(MarketRealtimeSseBroker.class);
        MarketRealtimeSseBroker.SseSubscriptionPermit permit = mock(MarketRealtimeSseBroker.SseSubscriptionPermit.class);
        when(broker.reserve(org.mockito.ArgumentMatchers.<Set<String>>eq(Set.of("BTCUSDT", "ETHUSDT")), eq("tab-1")))
                .thenReturn(permit);
        SseEmitter emitter = new SseEmitter();

        new MarketSummaryStreamStrategy(reader, broker)
                .open(MarketSseStreamRequest.summary(Set.of("BTCUSDT", "ETHUSDT"), "tab-1", emitter));

        verify(broker).register(permit, emitter);
    }

    @Test
    void releasesPermitWhenInitialSendFails() {
        MarketSummarySnapshotReader reader = symbol -> summary(symbol);
        MarketRealtimeSseBroker broker = mock(MarketRealtimeSseBroker.class);
        MarketRealtimeSseBroker.SseSubscriptionPermit permit = mock(MarketRealtimeSseBroker.SseSubscriptionPermit.class);
        when(broker.reserve(org.mockito.ArgumentMatchers.<Set<String>>eq(Set.of("BTCUSDT")), eq("tab-1")))
                .thenReturn(permit);
        FailingSseEmitter emitter = new FailingSseEmitter();

        new MarketSummaryStreamStrategy(reader, broker)
                .open(MarketSseStreamRequest.summary(Set.of("BTCUSDT"), "tab-1", emitter));

        assertTrue(emitter.completed);
        verify(broker).release(permit);
        verify(broker, never()).register(eq(permit), any(SseEmitter.class));
    }

    @Test
    void releasesPermitWhenLookupFails() {
        MarketSummarySnapshotReader reader = symbol -> { throw new IllegalStateException("not found"); };
        MarketRealtimeSseBroker broker = mock(MarketRealtimeSseBroker.class);
        MarketRealtimeSseBroker.SseSubscriptionPermit permit = mock(MarketRealtimeSseBroker.SseSubscriptionPermit.class);
        when(broker.reserve(org.mockito.ArgumentMatchers.<Set<String>>eq(Set.of("UNSUPPORTED")), eq("tab-1")))
                .thenReturn(permit);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                new MarketSummaryStreamStrategy(reader, broker)
                        .open(MarketSseStreamRequest.summary(Set.of("UNSUPPORTED"), "tab-1", new SseEmitter()))
        );

        verify(broker).release(permit);
        verify(broker, never()).register(eq(permit), any(SseEmitter.class));
    }

    static MarketSummaryResponse summary(String symbol) {
        return MarketSummaryResponse.of(symbol, symbol, 1, 2, 3, 0.01, 0.2, 100, Instant.parse("2026-05-12T00:00:00Z"), null, 8);
    }

    private static class FailingSseEmitter extends SseEmitter {
        boolean completed;

        @Override
        public void send(Object object) throws IOException {
            throw new IOException("failed");
        }

        @Override
        public void complete() {
            completed = true;
            super.complete();
        }
    }
}
