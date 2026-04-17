package coin.coinzzickmock.feature.market.api;

import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.application.service.GetMarketSummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketControllerTest {
    @Test
    void completesStreamSilentlyWhenInitialSendFails() {
        GetMarketSummaryService service = mock(GetMarketSummaryService.class);
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

        MarketController controller = new TestableMarketController(service);

        SseEmitter emitter = controller.stream("BTCUSDT");

        assertTrue(((FailingSseEmitter) emitter).completed);
        verify(service, never()).subscribe(org.mockito.ArgumentMatchers.eq("BTCUSDT"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unsubscribesAndCompletesWhenUpdateSendFails() {
        GetMarketSummaryService service = mock(GetMarketSummaryService.class);
        MarketController controller = new MarketController(service);
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
        verify(service).unsubscribe("BTCUSDT", listener);
    }

    private static class TestableMarketController extends MarketController {
        private final GetMarketSummaryService service;

        private TestableMarketController(GetMarketSummaryService service) {
            super(service);
            this.service = service;
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
