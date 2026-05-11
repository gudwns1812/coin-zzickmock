package coin.coinzzickmock.feature.market.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.feature.market.application.query.GetMarketQuery;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleProjector;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketDataStore;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.application.service.GetMarketSummaryService;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class MarketStreamBrokerTest {
    @Test
    void duplicateActiveAndOpenPositionSymbolSendsAndIndexesSummaryOnceButKeepsActiveReason() {
        MarketStreamRegistry registry = new MarketStreamRegistry();
        GetMarketSummaryService summaryService = mock(GetMarketSummaryService.class);
        when(summaryService.getMarket(any())).thenAnswer(invocation -> {
            GetMarketQuery query = invocation.getArgument(0);
            return market(query.symbol());
        });
        MarketStreamBroker broker = new MarketStreamBroker(
                registry,
                Runnable::run,
                summaryService,
                new RealtimeMarketCandleProjector(new RealtimeMarketDataStore())
        );
        RecordingSseEmitter emitter = new RecordingSseEmitter();

        broker.openSession(
                7L,
                "tab-1",
                "BTCUSDT",
                Set.of("BTCUSDT", "ETHUSDT"),
                MarketCandleInterval.ONE_MINUTE,
                emitter
        );

        verify(summaryService, times(1)).getMarket(argThat(query -> query.symbol().equals("BTCUSDT")));
        verify(summaryService, times(1)).getMarket(argThat(query -> query.symbol().equals("ETHUSDT")));
        assertEquals(2, emitter.sent.size());
        assertEquals(1, registry.summarySubscriberCount("BTCUSDT"));
        assertEquals(1, registry.summarySubscriberCount("ETHUSDT"));

        broker.removeOpenPositionReason(7L, "BTCUSDT");

        assertEquals(1, registry.summarySubscriberCount("BTCUSDT"));
        assertEquals(1, registry.summarySubscriberCount("ETHUSDT"));

        broker.removeOpenPositionReason(7L, "ETHUSDT");

        assertEquals(0, registry.summarySubscriberCount("ETHUSDT"));
    }

    private static MarketSummaryResult market(String symbol) {
        return new MarketSummaryResult(
                symbol,
                symbol + " Perpetual",
                74000,
                74010,
                74005,
                0.0001,
                0.2
        );
    }

    private static final class RecordingSseEmitter extends SseEmitter {
        private final List<Object> sent = new ArrayList<>();

        @Override
        public synchronized void send(Object object) throws IOException {
            sent.add(object);
        }
    }
}
