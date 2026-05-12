package coin.coinzzickmock.feature.market.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.query.GetMarketQuery;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.application.service.GetMarketSummaryService;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class MarketSummaryStreamStrategyTest {

    @Test
    void registersSingleSymbolAfterInitialSend() {
        GetMarketSummaryService service = mock(GetMarketSummaryService.class);
        MarketRealtimeSseBroker broker = mock(MarketRealtimeSseBroker.class);
        MarketRealtimeSseBroker.SseSubscriptionPermit permit = mock(MarketRealtimeSseBroker.SseSubscriptionPermit.class);
        when(service.getMarket(any())).thenReturn(summary("BTCUSDT"));
        when(broker.reserve(org.mockito.ArgumentMatchers.<Set<String>>eq(Set.of("BTCUSDT")), eq("tab-1")))
                .thenReturn(permit);
        SseEmitter emitter = new SseEmitter();

        new MarketSummaryStreamStrategy(service, broker)
                .open(MarketSseStreamRequest.summary(Set.of("BTCUSDT"), "tab-1", emitter));

        verify(service).getMarket(org.mockito.ArgumentMatchers.argThat(query -> query.symbol().equals("BTCUSDT")));
        verify(broker).register(permit, emitter);
    }

    @Test
    void registersMultiSymbolSummaryAfterInitialSends() {
        GetMarketSummaryService service = mock(GetMarketSummaryService.class);
        MarketRealtimeSseBroker broker = mock(MarketRealtimeSseBroker.class);
        MarketRealtimeSseBroker.SseSubscriptionPermit permit = mock(MarketRealtimeSseBroker.SseSubscriptionPermit.class);
        when(service.getMarket(any())).thenAnswer(invocation -> {
            GetMarketQuery query = invocation.getArgument(0);
            return summary(query.symbol());
        });
        when(broker.reserve(org.mockito.ArgumentMatchers.<Set<String>>eq(Set.of("BTCUSDT", "ETHUSDT")), eq("tab-1")))
                .thenReturn(permit);
        SseEmitter emitter = new SseEmitter();

        new MarketSummaryStreamStrategy(service, broker)
                .open(MarketSseStreamRequest.summary(Set.of("BTCUSDT", "ETHUSDT"), "tab-1", emitter));

        verify(service).getMarket(org.mockito.ArgumentMatchers.argThat(query -> query.symbol().equals("BTCUSDT")));
        verify(service).getMarket(org.mockito.ArgumentMatchers.argThat(query -> query.symbol().equals("ETHUSDT")));
        verify(broker).register(permit, emitter);
    }

    @Test
    void releasesPermitWhenInitialSendFails() {
        GetMarketSummaryService service = mock(GetMarketSummaryService.class);
        MarketRealtimeSseBroker broker = mock(MarketRealtimeSseBroker.class);
        MarketRealtimeSseBroker.SseSubscriptionPermit permit = mock(MarketRealtimeSseBroker.SseSubscriptionPermit.class);
        when(service.getMarket(any())).thenReturn(summary("BTCUSDT"));
        when(broker.reserve(org.mockito.ArgumentMatchers.<Set<String>>eq(Set.of("BTCUSDT")), eq("tab-1")))
                .thenReturn(permit);
        FailingSseEmitter emitter = new FailingSseEmitter();

        new MarketSummaryStreamStrategy(service, broker)
                .open(MarketSseStreamRequest.summary(Set.of("BTCUSDT"), "tab-1", emitter));

        assertTrue(emitter.completed);
        verify(broker).release(permit);
        verify(broker, never()).register(eq(permit), any(SseEmitter.class));
    }

    @Test
    void releasesPermitWhenLookupFails() {
        GetMarketSummaryService service = mock(GetMarketSummaryService.class);
        MarketRealtimeSseBroker broker = mock(MarketRealtimeSseBroker.class);
        MarketRealtimeSseBroker.SseSubscriptionPermit permit = mock(MarketRealtimeSseBroker.SseSubscriptionPermit.class);
        when(service.getMarket(any())).thenThrow(new CoreException(ErrorCode.MARKET_NOT_FOUND));
        when(broker.reserve(org.mockito.ArgumentMatchers.<Set<String>>eq(Set.of("UNSUPPORTED")), eq("tab-1")))
                .thenReturn(permit);

        org.junit.jupiter.api.Assertions.assertThrows(CoreException.class, () ->
                new MarketSummaryStreamStrategy(service, broker)
                        .open(MarketSseStreamRequest.summary(Set.of("UNSUPPORTED"), "tab-1", new SseEmitter()))
        );

        verify(broker).release(permit);
        verify(broker, never()).register(eq(permit), any(SseEmitter.class));
    }

    private static MarketSummaryResult summary(String symbol) {
        return new MarketSummaryResult(symbol, symbol + " Perpetual", 74000, 74010, 74005, 0.0001, 0.2);
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
