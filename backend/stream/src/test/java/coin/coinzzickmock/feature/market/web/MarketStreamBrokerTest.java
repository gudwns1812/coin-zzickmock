package coin.coinzzickmock.feature.market.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class MarketStreamBrokerTest {
    @Test
    void duplicateActiveAndOpenPositionSymbolSendsAndIndexesSummaryOnceButKeepsActiveReason() {
        MarketStreamRegistry registry = new MarketStreamRegistry();
        MarketStreamBroker broker = broker(registry);
        RecordingSseEmitter emitter = new RecordingSseEmitter();

        broker.openSession(7L, "tab-1", "BTCUSDT", Set.of("BTCUSDT", "ETHUSDT"), "1m", emitter);

        assertEquals(3, emitter.sent.size());
        assertEquals(1, registry.summarySubscriberCount("BTCUSDT"));
        assertEquals(1, registry.summarySubscriberCount("ETHUSDT"));

        broker.removeOpenPositionReason(7L, "BTCUSDT");
        assertEquals(1, registry.summarySubscriberCount("BTCUSDT"));
        broker.removeOpenPositionReason(7L, "ETHUSDT");
        assertEquals(0, registry.summarySubscriberCount("ETHUSDT"));
    }

    @Test
    void initialSnapshotFailureReleasesSessionAndCompletesEmitter() {
        MarketStreamRegistry registry = new MarketStreamRegistry();
        MarketStreamBroker broker = broker(registry);
        FailingSseEmitter emitter = new FailingSseEmitter();

        assertThatThrownBy(() -> broker.openSession(7L, "tab-1", "BTCUSDT", Set.of(), "1m", emitter))
                .isInstanceOf(IllegalStateException.class);

        assertEquals(0, registry.activeSessionCount());
        assertEquals(0, registry.summarySubscriberCount("BTCUSDT"));
        assertEquals(0, registry.candleSubscriberCount(new CandleSubscription("BTCUSDT", "1m")));
        assertThat(emitter.completed()).isTrue();
    }

    @Test
    void historyFinalizedFanOutTargetsOnlyMatchingUnifiedCandleSubscribers() {
        MarketStreamRegistry registry = new MarketStreamRegistry();
        MarketStreamBroker broker = broker(registry);
        RecordingSseEmitter btcEmitter = new RecordingSseEmitter();
        RecordingSseEmitter ethEmitter = new RecordingSseEmitter();
        broker.openSession(1L, "tab-1", "BTCUSDT", Set.of(), "1m", btcEmitter);
        broker.openSession(2L, "tab-2", "ETHUSDT", Set.of(), "1m", ethEmitter);

        broker.onHistoryFinalized(
                "BTCUSDT",
                Instant.parse("2026-05-12T00:00:00Z"),
                Instant.parse("2026-05-12T00:01:00Z")
        );

        assertThat(btcEmitter.sent).anyMatch(event -> event instanceof MarketStreamEventResponse response
                && response.kind() == MarketStreamEventType.MARKET_HISTORY_FINALIZED);
        assertThat(ethEmitter.sent).noneMatch(event -> event instanceof MarketStreamEventResponse response
                && response.kind() == MarketStreamEventType.MARKET_HISTORY_FINALIZED);
    }

    private static MarketStreamBroker broker(MarketStreamRegistry registry) {
        MarketSummarySnapshotReader summaryReader = MarketSummaryStreamStrategyTest::summary;
        MarketCandleSnapshotReader candleReader = (symbol, interval) -> Optional.of(new MarketCandleResponse(
                Instant.parse("2026-05-12T00:00:00Z"), Instant.parse("2026-05-12T00:01:00Z"), 1, 2, 0.5, 1.5, 100));
        MarketCurrentCandleBootstrapper bootstrapper = (symbol, interval) -> true;
        return new MarketStreamBroker(registry, Runnable::run, summaryReader, candleReader, bootstrapper);
    }

    private static class RecordingSseEmitter extends SseEmitter {
        final List<Object> sent = new ArrayList<>();
        @Override public void send(Object object) throws IOException { sent.add(object); }
    }

    private static class FailingSseEmitter extends SseEmitter {
        private boolean completed;
        @Override public void send(Object object) throws IOException { throw new IOException("fail"); }
        @Override public void complete() { completed = true; super.complete(); }
        boolean completed() { return completed; }
    }
}
