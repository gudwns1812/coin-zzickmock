package coin.coinzzickmock.feature.market.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import coin.coinzzickmock.common.web.SseSubscriptionLimitExceededException;
import coin.coinzzickmock.providers.telemetry.NoopSseTelemetry;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class MarketCandleRealtimeSseBrokerTest {
    @Test
    void reservesRegistersAndReleasesSubscriberPermit() {
        MarketCandleRealtimeSseBroker broker = brokerWithLimits(1, 1);
        MarketCandleRealtimeSseBroker.SubscriptionKey key = key("BTCUSDT", "1m");

        MarketCandleRealtimeSseBroker.SseSubscriptionPermit releasedPermit = broker.reserve(key);
        broker.release(releasedPermit);
        MarketCandleRealtimeSseBroker.SseSubscriptionPermit registeredPermit = broker.reserve(key);
        CapturingSseEmitter emitter = new CapturingSseEmitter();

        broker.register(registeredPermit, emitter);
        broker.unregister(key, emitter);

        assertThat(broker.hasSubscriberLimit(key)).isFalse();
    }

    @Test
    void rejectsWhenPerKeyLimitIsExceeded() {
        MarketCandleRealtimeSseBroker broker = brokerWithLimits(1, 2);
        MarketCandleRealtimeSseBroker.SubscriptionKey key = key("BTCUSDT", "1m");
        broker.register(broker.reserve(key), new CapturingSseEmitter());

        SseSubscriptionLimitExceededException exception = assertThrows(SseSubscriptionLimitExceededException.class, () -> broker.reserve(key));

        assertThat(exception.reason()).isEqualTo("key_limit");
    }

    @Test
    void sendFailureReleasesLimitForReplacementSubscriber() {
        MarketCandleRealtimeSseBroker broker = brokerWithLimits(1, 1);
        MarketCandleRealtimeSseBroker.SubscriptionKey key = key("BTCUSDT", "1m");
        broker.register(broker.reserve(key), new FailingSseEmitter());

        broker.onCandleUpdated("BTCUSDT");

        assertThat(broker.hasSubscriberLimit(key)).isFalse();
    }

    @Test
    void historyFinalizedSendsOnlyAffectedInterval() {
        MarketCandleRealtimeSseBroker broker = brokerWithLimits(2, 4);
        CapturingSseEmitter oneMinute = new CapturingSseEmitter();
        CapturingSseEmitter oneHour = new CapturingSseEmitter();
        broker.register(broker.reserve(key("BTCUSDT", "1m")), oneMinute);
        broker.register(broker.reserve(key("BTCUSDT", "1H")), oneHour);

        broker.onHistoryFinalized("BTCUSDT", Instant.parse("2026-05-12T00:00:00Z"), Instant.parse("2026-05-12T00:01:00Z"));

        assertThat(oneMinute.sent).hasSize(1);
        assertThat(oneHour.sent).isEmpty();
    }

    private static MarketCandleRealtimeSseBroker brokerWithLimits(int perKey, int total) {
        MarketCandleSnapshotReader candleReader = (symbol, interval) -> Optional.of(candle());
        MarketFinalizedCandleIntervalsReader intervalsReader = (symbol, open, close) -> List.of("1m");
        return new MarketCandleRealtimeSseBroker(directExecutor(), candleReader, intervalsReader, perKey, total, NoopSseTelemetry.INSTANCE);
    }

    private static MarketCandleRealtimeSseBroker.SubscriptionKey key(String symbol, String interval) {
        return new MarketCandleRealtimeSseBroker.SubscriptionKey(symbol, interval);
    }

    private static MarketCandleResponse candle() {
        Instant open = Instant.parse("2026-05-12T00:00:00Z");
        return new MarketCandleResponse(open, open.plusSeconds(60), 1, 2, 0.5, 1.5, 100);
    }

    private static Executor directExecutor() { return Runnable::run; }

    private static class CapturingSseEmitter extends SseEmitter {
        final List<Object> sent = new ArrayList<>();
        @Override public void send(Object object) throws IOException { sent.add(object); }
    }

    private static class FailingSseEmitter extends SseEmitter {
        @Override public void send(Object object) throws IOException { throw new IOException("fail"); }
    }
}
