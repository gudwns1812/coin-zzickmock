package coin.coinzzickmock.feature.market.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.feature.market.application.realtime.MarketRealtimeFeed;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.application.service.GetMarketSummaryService;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class MarketControllerTest {
    @Test
    void completesStreamSilentlyWhenInitialSendFails() {
        GetMarketSummaryService service = mock(GetMarketSummaryService.class);
        MarketRealtimeFeed feed = mock(MarketRealtimeFeed.class);
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

        MarketController controller = new TestableMarketController(service, feed);

        SseEmitter emitter = controller.stream("BTCUSDT");

        assertTrue(((FailingSseEmitter) emitter).completed);
        verify(feed, never()).subscribe(eq("BTCUSDT"), any());
    }

    @Test
    void unsubscribesAndCompletesWhenUpdateSendFails() {
        GetMarketSummaryService service = mock(GetMarketSummaryService.class);
        MarketRealtimeFeed feed = mock(MarketRealtimeFeed.class);
        MarketController controller = new MarketController(service, feed);
        FailingSseEmitter emitter = new FailingSseEmitter();
        AtomicReference<Consumer<MarketSummaryResult>> listenerReference = new AtomicReference<>();
        Consumer<MarketSummaryResult> listener = result -> { };
        listenerReference.set(listener);

        boolean sent = controller.sendEvent(
                "BTCUSDT",
                emitter,
                listenerReference,
                new MarketSummaryResult("BTCUSDT", "Bitcoin Perpetual", 74000, 74010, 74005, 0.0001, 0.2)
        );

        assertFalse(sent);
        assertTrue(emitter.completed);
        verify(feed).unsubscribe("BTCUSDT", listener);
    }

    private static class TestableMarketController extends MarketController {
        private TestableMarketController(GetMarketSummaryService service, MarketRealtimeFeed feed) {
            super(service, feed);
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
