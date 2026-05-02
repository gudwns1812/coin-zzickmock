package coin.coinzzickmock.feature.market.web;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.application.realtime.MarketCandleUpdatedEvent;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleProjector;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleUpdate;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketDataStore;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.providers.telemetry.SseTelemetry;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class MarketCandleRealtimeSseBrokerTest {
    @Test
    void deliversProjectedCandleToMatchingIntervalSubscribers() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        MarketCandleRealtimeSseBroker broker = new MarketCandleRealtimeSseBroker(
                Runnable::run,
                new RealtimeMarketCandleProjector(store)
        );
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        broker.register("BTCUSDT", MarketCandleInterval.ONE_MINUTE, emitter);
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

        broker.onCandleUpdated(new MarketCandleUpdatedEvent("BTCUSDT"));

        assertThat(emitter.events()).hasSize(1);
        assertThat(((MarketCandleResponse) emitter.events().get(0)).closePrice()).isEqualTo(102);
    }

    @Test
    void removesFailedEmitterWithoutCompletingIt() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        MarketCandleRealtimeSseBroker broker = new MarketCandleRealtimeSseBroker(
                Runnable::run,
                new RealtimeMarketCandleProjector(store)
        );
        FailingSseEmitter failingEmitter = new FailingSseEmitter();
        CapturingSseEmitter healthyEmitter = new CapturingSseEmitter();
        Instant open = Instant.parse("2026-04-30T04:00:00Z");
        broker.register("BTCUSDT", MarketCandleInterval.ONE_MINUTE, failingEmitter);
        broker.register("BTCUSDT", MarketCandleInterval.ONE_MINUTE, healthyEmitter);
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

        broker.onCandleUpdated(new MarketCandleUpdatedEvent("BTCUSDT"));
        broker.onCandleUpdated(new MarketCandleUpdatedEvent("BTCUSDT"));

        assertThat(failingEmitter.sendAttempts()).isEqualTo(1);
        assertThat(failingEmitter.completed()).isFalse();
        assertThat(healthyEmitter.events()).hasSize(2);
    }

    @Test
    void recordsConnectionSendAndExecutorTelemetry() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        RecordingSseTelemetry telemetry = new RecordingSseTelemetry();
        MarketCandleRealtimeSseBroker broker = new MarketCandleRealtimeSseBroker(
                Runnable::run,
                new RealtimeMarketCandleProjector(store),
                telemetry
        );
        FailingSseEmitter failingEmitter = new FailingSseEmitter();
        Instant open = Instant.parse("2026-04-30T04:00:00Z");
        broker.register("BTCUSDT", MarketCandleInterval.ONE_MINUTE, failingEmitter);
        store.acceptCandle(candle(open));

        broker.onCandleUpdated(new MarketCandleUpdatedEvent("BTCUSDT"));

        assertThat(telemetry.events()).contains(
                "opened:market_candle",
                "send:market_candle:failure",
                "closed:market_candle:send_failure"
        );

        RecordingSseTelemetry rejectedTelemetry = new RecordingSseTelemetry();
        MarketCandleRealtimeSseBroker rejectedBroker = new MarketCandleRealtimeSseBroker(
                command -> {
                    throw new RejectedExecutionException("queue full");
                },
                new RealtimeMarketCandleProjector(store),
                rejectedTelemetry
        );
        rejectedBroker.register("BTCUSDT", MarketCandleInterval.ONE_MINUTE, new CapturingSseEmitter());

        rejectedBroker.onCandleUpdated(new MarketCandleUpdatedEvent("BTCUSDT"));

        assertThat(rejectedTelemetry.events()).contains("executor:market_candle");
    }

    private RealtimeMarketCandleUpdate candle(Instant open) {
        return new RealtimeMarketCandleUpdate(
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
        );
    }

    private static class CapturingSseEmitter extends SseEmitter {
        private final List<Object> events = new ArrayList<>();

        @Override
        public void send(Object object) throws IOException {
            events.add(object);
        }

        private List<Object> events() {
            return events;
        }
    }

    private static class FailingSseEmitter extends SseEmitter {
        private int sendAttempts;
        private boolean completed;

        @Override
        public void send(Object object) throws IOException {
            sendAttempts++;
            throw new IOException("client disconnected");
        }

        @Override
        public synchronized void complete() {
            completed = true;
            super.complete();
        }

        private int sendAttempts() {
            return sendAttempts;
        }

        private boolean completed() {
            return completed;
        }
    }

    private static class RecordingSseTelemetry implements SseTelemetry {
        private final List<String> events = new ArrayList<>();

        @Override
        public void connectionOpened(String stream) {
            events.add("opened:" + stream);
        }

        @Override
        public void connectionClosed(String stream, String reason) {
            events.add("closed:" + stream + ":" + reason);
        }

        @Override
        public void connectionRejected(String stream, String reason) {
            events.add("rejected:" + stream + ":" + reason);
        }

        @Override
        public void sendRecorded(String stream, String result, Duration duration) {
            events.add("send:" + stream + ":" + result);
        }

        @Override
        public void executorRejected(String stream) {
            events.add("executor:" + stream);
        }

        private List<String> events() {
            return List.copyOf(events);
        }
    }
}
