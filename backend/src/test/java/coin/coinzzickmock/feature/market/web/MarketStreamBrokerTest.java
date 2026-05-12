package coin.coinzzickmock.feature.market.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.feature.market.application.query.GetMarketQuery;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleProjector;
import coin.coinzzickmock.feature.market.application.realtime.MarketHistoryFinalizedEvent;
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


    @Test
    void initialSnapshotFailureReleasesSessionAndCompletesEmitter() {
        MarketStreamRegistry registry = new MarketStreamRegistry();
        GetMarketSummaryService summaryService = mock(GetMarketSummaryService.class);
        when(summaryService.getMarket(any())).thenReturn(market("BTCUSDT"));
        MarketStreamBroker broker = new MarketStreamBroker(
                registry,
                Runnable::run,
                summaryService,
                new RealtimeMarketCandleProjector(new RealtimeMarketDataStore())
        );
        FailingSseEmitter emitter = new FailingSseEmitter();

        assertThatThrownBy(() -> broker.openSession(
                7L,
                "tab-1",
                "BTCUSDT",
                Set.of(),
                MarketCandleInterval.ONE_MINUTE,
                emitter
        )).isInstanceOf(IllegalStateException.class);

        assertEquals(0, registry.activeSessionCount());
        assertEquals(0, registry.summarySubscriberCount("BTCUSDT"));
        assertEquals(0, registry.candleSubscriberCount(new CandleSubscription("BTCUSDT", MarketCandleInterval.ONE_MINUTE)));
        assertThat(emitter.completed()).isTrue();
    }

    @Test
    void historyFinalizedFanOutTargetsOnlyMatchingUnifiedCandleSubscribers() {
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
        RecordingSseEmitter btcEmitter = new RecordingSseEmitter();
        RecordingSseEmitter ethEmitter = new RecordingSseEmitter();
        broker.openSession(
                7L,
                "tab-btc",
                "BTCUSDT",
                Set.of(),
                MarketCandleInterval.ONE_MINUTE,
                btcEmitter
        );
        broker.openSession(
                8L,
                "tab-eth",
                "ETHUSDT",
                Set.of(),
                MarketCandleInterval.ONE_MINUTE,
                ethEmitter
        );

        broker.onHistoryFinalized(new MarketHistoryFinalizedEvent(
                "BTCUSDT",
                java.time.Instant.parse("2026-04-30T04:00:00Z"),
                java.time.Instant.parse("2026-04-30T04:01:00Z")
        ));

        assertThat(btcEmitter.sent).hasSize(2);
        assertThat(ethEmitter.sent).hasSize(1);
        MarketStreamEventResponse response = (MarketStreamEventResponse) btcEmitter.sent.get(1);
        assertThat(response.type()).isEqualTo(MarketStreamEventType.MARKET_HISTORY_FINALIZED);
        assertThat(response.symbol()).isEqualTo("BTCUSDT");
        assertThat(response.interval()).isEqualTo("1m");
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


    private static final class FailingSseEmitter extends SseEmitter {
        private boolean completed;

        @Override
        public synchronized void send(Object object) throws IOException {
            throw new IOException("client disconnected");
        }

        @Override
        public synchronized void complete() {
            completed = true;
            super.complete();
        }

        private boolean completed() {
            return completed;
        }
    }

    private static final class RecordingSseEmitter extends SseEmitter {
        private final List<Object> sent = new ArrayList<>();

        @Override
        public synchronized void send(Object object) throws IOException {
            sent.add(object);
        }
    }
}
