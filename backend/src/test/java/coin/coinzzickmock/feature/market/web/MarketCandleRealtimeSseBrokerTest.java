package coin.coinzzickmock.feature.market.web;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.application.realtime.MarketCandleUpdatedEvent;
import coin.coinzzickmock.feature.market.application.realtime.MarketHistoryFinalizedEvent;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleProjector;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleUpdate;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketDataStore;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import coin.coinzzickmock.providers.telemetry.NoopSseTelemetry;
import coin.coinzzickmock.providers.telemetry.SseTelemetry;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    void removesAlreadyCompletedEmitterWithoutPropagatingFailure() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        RecordingSseTelemetry telemetry = new RecordingSseTelemetry();
        MarketCandleRealtimeSseBroker broker = new MarketCandleRealtimeSseBroker(
                Runnable::run,
                new RealtimeMarketCandleProjector(store),
                telemetry
        );
        AlreadyCompletedSseEmitter completedEmitter = new AlreadyCompletedSseEmitter();
        CapturingSseEmitter healthyEmitter = new CapturingSseEmitter();
        Instant open = Instant.parse("2026-04-30T04:00:00Z");
        broker.register("BTCUSDT", MarketCandleInterval.ONE_MINUTE, completedEmitter);
        broker.register("BTCUSDT", MarketCandleInterval.ONE_MINUTE, healthyEmitter);
        store.acceptCandle(candle(open));

        broker.onCandleUpdated(new MarketCandleUpdatedEvent("BTCUSDT"));
        broker.onCandleUpdated(new MarketCandleUpdatedEvent("BTCUSDT"));

        assertThat(completedEmitter.sendAttempts()).isEqualTo(1);
        assertThat(healthyEmitter.events()).hasSize(2);
        assertThat(telemetry.events()).contains(
                "send:market_candle:failure",
                "closed:market_candle:send_failure"
        );
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

    @Test
    void deliversHistoryFinalizedMessageToAffectedMinuteDerivedSubscribers() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        MarketCandleRealtimeSseBroker broker = new MarketCandleRealtimeSseBroker(
                Runnable::run,
                new RealtimeMarketCandleProjector(store)
        );
        CapturingSseEmitter oneMinuteEmitter = new CapturingSseEmitter();
        CapturingSseEmitter threeMinuteEmitter = new CapturingSseEmitter();
        CapturingSseEmitter otherSymbolEmitter = new CapturingSseEmitter();
        broker.register("BTCUSDT", MarketCandleInterval.ONE_MINUTE, oneMinuteEmitter);
        broker.register("BTCUSDT", MarketCandleInterval.THREE_MINUTES, threeMinuteEmitter);
        broker.register("ETHUSDT", MarketCandleInterval.ONE_MINUTE, otherSymbolEmitter);

        broker.onHistoryFinalized(new MarketHistoryFinalizedEvent(
                "BTCUSDT",
                Instant.parse("2026-04-30T04:00:00Z"),
                Instant.parse("2026-04-30T04:01:00Z")
        ));

        assertThat(oneMinuteEmitter.events()).singleElement()
                .isInstanceOf(MarketCandleHistoryFinalizedResponse.class);
        MarketCandleHistoryFinalizedResponse response =
                (MarketCandleHistoryFinalizedResponse) oneMinuteEmitter.events().get(0);
        assertThat(response.type()).isEqualTo("historyFinalized");
        assertThat(response.affectedIntervals()).contains("1m", "3m", "5m", "15m");
        assertThat(threeMinuteEmitter.events()).hasSize(1);
        assertThat(otherSymbolEmitter.events()).isEmpty();
    }

    @Test
    void includesOneHourOnlyWhenCompletedHourIsVisible() {
        RealtimeMarketDataStore store = new RealtimeMarketDataStore();
        RecordingMarketHistoryRepository repository = new RecordingMarketHistoryRepository();
        MarketCandleRealtimeSseBroker broker = new MarketCandleRealtimeSseBroker(
                Runnable::run,
                new RealtimeMarketCandleProjector(store),
                repository,
                NoopSseTelemetry.INSTANCE
        );
        CapturingSseEmitter oneHourEmitter = new CapturingSseEmitter();
        broker.register("BTCUSDT", MarketCandleInterval.ONE_HOUR, oneHourEmitter);
        Instant openTime = Instant.parse("2026-04-30T04:59:00Z");

        broker.onHistoryFinalized(new MarketHistoryFinalizedEvent(
                "BTCUSDT",
                openTime,
                openTime.plusSeconds(60)
        ));
        repository.completedHourlyCandles.add(hourly(Instant.parse("2026-04-30T04:00:00Z")));
        broker.onHistoryFinalized(new MarketHistoryFinalizedEvent(
                "BTCUSDT",
                openTime,
                openTime.plusSeconds(60)
        ));

        assertThat(oneHourEmitter.events()).singleElement()
                .isInstanceOf(MarketCandleHistoryFinalizedResponse.class);
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

    private static HourlyMarketCandle hourly(Instant openTime) {
        return new HourlyMarketCandle(
                1L,
                openTime,
                openTime.plusSeconds(3600),
                100,
                101,
                99,
                100.5,
                10,
                1005,
                openTime,
                openTime.plusSeconds(3600)
        );
    }

    private static class RecordingMarketHistoryRepository extends coin.coinzzickmock.testsupport.TestMarketHistoryRepository {
        private final List<HourlyMarketCandle> completedHourlyCandles = new ArrayList<>();

        @Override
        public Map<String, Long> findSymbolIdsBySymbols(List<String> symbols) {
            return Map.of("BTCUSDT", 1L);
        }

        @Override
        public List<StartupBackfillCursor> findStartupBackfillCursors() {
            return List.of();
        }

        @Override
        public Optional<Instant> findLatestMinuteCandleOpenTime(long symbolId) {
            return Optional.empty();
        }

        @Override
        public Optional<Instant> findLatestMinuteCandleOpenTimeBefore(long symbolId, Instant beforeExclusive) {
            return Optional.empty();
        }

        @Override
        public Optional<Instant> findLatestHourlyCandleOpenTime(long symbolId) {
            return Optional.empty();
        }

        @Override
        public Optional<Instant> findLatestHourlyCandleOpenTimeBefore(long symbolId, Instant beforeExclusive) {
            return Optional.empty();
        }

        @Override
        public Optional<MarketHistoryCandle> findMinuteCandle(long symbolId, Instant openTime) {
            return Optional.empty();
        }

        @Override
        public List<MarketHistoryCandle> findMinuteCandles(long symbolId, Instant fromInclusive, Instant toExclusive) {
            return List.of();
        }

        @Override
        public Optional<HourlyMarketCandle> findHourlyCandle(long symbolId, Instant openTime) {
            return Optional.empty();
        }

        @Override
        public List<HourlyMarketCandle> findHourlyCandles(long symbolId, Instant fromInclusive, Instant toExclusive) {
            return List.of();
        }

        @Override
        public List<HourlyMarketCandle> findCompletedHourlyCandles(
                long symbolId,
                Instant fromInclusive,
                Instant toExclusive
        ) {
            return completedHourlyCandles.stream()
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .toList();
        }

        @Override
        public void saveMinuteCandle(MarketHistoryCandle candle) {
        }

        @Override
        public void saveHourlyCandle(HourlyMarketCandle candle) {
        }
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

    private static class AlreadyCompletedSseEmitter extends SseEmitter {
        private int sendAttempts;

        @Override
        public void send(Object object) throws IOException {
            sendAttempts++;
            throw new IllegalStateException("ResponseBodyEmitter has already completed");
        }

        private int sendAttempts() {
            return sendAttempts;
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
