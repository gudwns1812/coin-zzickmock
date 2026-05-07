package coin.coinzzickmock.feature.market.application.repair;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.CoinZzickmockApplication;
import coin.coinzzickmock.feature.market.application.realtime.MarketHistoryPersistenceCoordinator;
import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = {CoinZzickmockApplication.class, MarketHistoryRepairFlowTest.Configuration.class},
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "spring.task.scheduling.enabled=false",
                "coin.market.history-repair.worker.enabled=false",
                "coin.market.history-repair.persistence-retry.max-attempts=2",
                "coin.market.history-repair.persistence-retry.delay-ms=1",
                "coin.market.history-repair.enqueue-retry.max-attempts=2",
                "coin.market.history-repair.enqueue-retry.delay-ms=1"
        }
)
@ActiveProfiles("test")
class MarketHistoryRepairFlowTest {
    private static final Instant OPEN_TIME = Instant.parse("2026-04-17T06:00:00Z");
    private static final Instant CLOSE_TIME = Instant.parse("2026-04-17T06:01:00Z");
    private static final Instant HOUR_OPEN_TIME = Instant.parse("2026-04-17T06:00:00Z");
    private static final Instant HOUR_CLOSE_TIME = Instant.parse("2026-04-17T07:00:00Z");

    @Autowired
    private MarketHistoryPersistenceCoordinator marketHistoryPersistenceCoordinator;

    @Autowired
    private MarketHistoryRepairRequestRecorder marketHistoryRepairRequestRecorder;

    @Autowired
    private MarketHistoryRepairProcessor marketHistoryRepairProcessor;

    @Autowired
    private coin.coinzzickmock.feature.market.application.realtime.MarketHistoryRecorder marketHistoryRecorder;

    @Autowired
    private MarketHistoryRepairEventRepository marketHistoryRepairEventRepository;

    @Autowired
    private MarketHistoryRepairQueuePublisher marketHistoryRepairQueuePublisher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FakeMarketDataGateway marketDataGateway;

    @Autowired
    private RecordingRepairQueue repairQueue;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM market_history_repair_events");
        jdbcTemplate.update("DELETE FROM market_candles_1h");
        jdbcTemplate.update("DELETE FROM market_candles_1m");
        marketDataGateway.reset();
        repairQueue.clear();
    }

    @Test
    void retryRecoversTransientPersistenceFailureWithoutDurableRepairEvent() {
        marketDataGateway.failuresBeforeSuccess(1);

        marketHistoryPersistenceCoordinator.persistClosedMinuteCandles(List.of("BTCUSDT"), OPEN_TIME, CLOSE_TIME);

        assertThat(count("market_candles_1m")).isEqualTo(1);
        assertThat(count("market_history_repair_events")).isZero();
        assertThat(repairQueue.pushed()).isEmpty();
        assertThat(marketDataGateway.minuteCandleLoadCalls()).isEqualTo(2);
    }

    @Test
    void exhaustedMinutePersistenceFailureCreatesOneDurableRepairEventAndQueuesId() {
        marketDataGateway.alwaysEmpty();

        marketHistoryPersistenceCoordinator.persistClosedMinuteCandles(List.of("BTCUSDT"), OPEN_TIME, CLOSE_TIME);
        marketHistoryPersistenceCoordinator.persistClosedMinuteCandles(List.of("BTCUSDT"), OPEN_TIME, CLOSE_TIME);

        assertThat(count("market_history_repair_events")).isEqualTo(1);
        assertThat(singleStatus()).isEqualTo("QUEUED");
        assertThat(repairQueue.pushed()).hasSize(2).containsOnly(singleEventId());
    }

    @Test
    void workerConsumesQueuedMinuteRepairAndMarksSucceeded() {
        marketDataGateway.alwaysEmpty();
        MarketHistoryRepairEvent event = marketHistoryRepairRequestRecorder.recordOneMinuteFailure(
                "BTCUSDT",
                OPEN_TIME,
                CLOSE_TIME,
                new MarketHistoryPersistenceAttemptException("initial persistence failed")
        );
        marketDataGateway.reset();

        boolean processed = marketHistoryRepairProcessor.processNext(Duration.ofMillis(1));

        assertThat(processed).isTrue();
        assertThat(status(event.id())).isEqualTo("SUCCEEDED");
        assertThat(count("market_candles_1m")).isEqualTo(1);
    }

    @Test
    void workerMarksMinuteRepairFailedWhenRepairProcessingFails() {
        MarketHistoryRepairEvent event = marketHistoryRepairRequestRecorder.recordOneMinuteFailure(
                "BTCUSDT",
                OPEN_TIME,
                CLOSE_TIME,
                new MarketHistoryPersistenceAttemptException("initial persistence failed")
        );
        marketDataGateway.alwaysThrow();

        boolean processed = marketHistoryRepairProcessor.processNext(Duration.ofMillis(1));

        assertThat(processed).isTrue();
        assertThat(status(event.id())).isEqualTo("FAILED");
    }

    @Test
    void hourlyRepairWaitsForCompleteMinuteCoverageBeforeRebuild() {
        MarketHistoryRepairEvent event = marketHistoryRepairRequestRecorder.recordOneHourFailure(
                "BTCUSDT",
                HOUR_OPEN_TIME,
                HOUR_CLOSE_TIME,
                new MarketHistoryPersistenceAttemptException("hourly persistence failed")
        );

        boolean processed = marketHistoryRepairProcessor.processNext(Duration.ofMillis(1));

        assertThat(processed).isTrue();
        assertThat(status(event.id())).isEqualTo("WAITING_FOR_MINUTES");
        assertThat(count("market_candles_1h")).isZero();
    }

    @Test
    void waitingHourlyRepairCanBeRequeuedAfterMinuteRepairCompletesCoverage() {
        MarketHistoryRepairEvent hourlyEvent = marketHistoryRepairRequestRecorder.recordOneHourFailure(
                "BTCUSDT",
                HOUR_OPEN_TIME,
                HOUR_CLOSE_TIME,
                new MarketHistoryPersistenceAttemptException("hourly persistence failed")
        );
        assertThat(marketHistoryRepairProcessor.processNext(Duration.ofMillis(1))).isTrue();
        assertThat(status(hourlyEvent.id())).isEqualTo("WAITING_FOR_MINUTES");
        repairQueue.clear();

        marketHistoryRecorder.recordHistoricalMinuteCandlesBySymbol(Map.of(
                "BTCUSDT",
                minuteCandles(HOUR_OPEN_TIME.plus(1, ChronoUnit.MINUTES), HOUR_CLOSE_TIME)
        ));
        assertThat(count("market_candles_1h")).isZero();
        assertThat(status(hourlyEvent.id())).isEqualTo("WAITING_FOR_MINUTES");
        MarketHistoryRepairEvent minuteEvent = marketHistoryRepairRequestRecorder.recordOneMinuteFailure(
                "BTCUSDT",
                HOUR_OPEN_TIME,
                HOUR_OPEN_TIME.plus(1, ChronoUnit.MINUTES),
                new MarketHistoryPersistenceAttemptException("minute persistence failed")
        );

        assertThat(marketHistoryRepairProcessor.processNext(Duration.ofMillis(1))).isTrue();

        assertThat(status(minuteEvent.id())).isEqualTo("SUCCEEDED");
        assertThat(count("market_candles_1m")).isEqualTo(60);
        marketHistoryRepairEventRepository.queueWaitingHourlyRepairEvents("BTCUSDT", HOUR_OPEN_TIME)
                .forEach(marketHistoryRepairQueuePublisher::enqueue);
        assertThat(status(hourlyEvent.id())).isEqualTo("QUEUED");
        assertThat(repairQueue.pushed()).contains(hourlyEvent.id());
    }


    @Test
    void duplicateQueueMessagesDoNotOverwriteSucceededTerminalStatus() {
        MarketHistoryRepairEvent event = marketHistoryRepairRequestRecorder.recordOneMinuteFailure(
                "BTCUSDT",
                OPEN_TIME,
                CLOSE_TIME,
                new MarketHistoryPersistenceAttemptException("initial persistence failed")
        );
        repairQueue.push(event.id());

        assertThat(marketHistoryRepairProcessor.processNext(Duration.ofMillis(1))).isTrue();
        marketDataGateway.alwaysThrow();
        assertThat(marketHistoryRepairProcessor.processNext(Duration.ofMillis(1))).isTrue();

        assertThat(status(event.id())).isEqualTo("SUCCEEDED");
    }

    @Test
    void duplicateRepairRequestDoesNotRequeueProcessingEventOrCorruptTerminalStatus() {
        MarketHistoryRepairEvent event = marketHistoryRepairRequestRecorder.recordOneMinuteFailure(
                "BTCUSDT",
                OPEN_TIME,
                CLOSE_TIME,
                new MarketHistoryPersistenceAttemptException("initial persistence failed")
        );

        assertThat(marketHistoryRepairEventRepository.markProcessing(event.id())).isTrue();
        marketHistoryRepairRequestRecorder.recordOneMinuteFailure(
                "BTCUSDT",
                OPEN_TIME,
                CLOSE_TIME,
                new MarketHistoryPersistenceAttemptException("duplicate persistence failed")
        );
        assertThat(status(event.id())).isEqualTo("PROCESSING");

        marketHistoryRepairEventRepository.markSucceeded(event.id());
        marketHistoryRepairEventRepository.markFailed(event.id(), "late duplicate failure");

        assertThat(status(event.id())).isEqualTo("SUCCEEDED");
    }

    @Test
    void repairEventClaimIsSingleUse() {
        MarketHistoryRepairEvent event = marketHistoryRepairRequestRecorder.recordOneMinuteFailure(
                "BTCUSDT",
                OPEN_TIME,
                CLOSE_TIME,
                new MarketHistoryPersistenceAttemptException("initial persistence failed")
        );

        assertThat(marketHistoryRepairEventRepository.markProcessing(event.id())).isTrue();
        assertThat(marketHistoryRepairEventRepository.markProcessing(event.id())).isFalse();
    }

    private int count(String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
        return count == null ? 0 : count;
    }

    private long singleEventId() {
        Long id = jdbcTemplate.queryForObject("SELECT id FROM market_history_repair_events", Long.class);
        return id == null ? -1 : id;
    }

    private String singleStatus() {
        return status(singleEventId());
    }

    private String status(long eventId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM market_history_repair_events WHERE id = ?",
                String.class,
                eventId
        );
    }

    private static List<MarketMinuteCandleSnapshot> minuteCandles(Instant fromInclusive, Instant toExclusive) {
        List<MarketMinuteCandleSnapshot> candles = new ArrayList<>();
        Instant current = fromInclusive;
        while (current.isBefore(toExclusive)) {
            candles.add(new MarketMinuteCandleSnapshot(
                    current,
                    current.plus(1, ChronoUnit.MINUTES),
                    101000.0,
                    101500.0,
                    100500.0,
                    101250.0,
                    10.0,
                    1012500.0
            ));
            current = current.plus(1, ChronoUnit.MINUTES);
        }
        return candles;
    }

    @TestConfiguration
    static class Configuration {
        @Bean
        @Primary
        FakeMarketDataGateway fakeMarketDataGateway() {
            return new FakeMarketDataGateway();
        }

        @Bean
        @Primary
        RecordingRepairQueue recordingRepairQueue() {
            return new RecordingRepairQueue();
        }
    }

    static class RecordingRepairQueue implements MarketHistoryRepairQueue {
        private final ArrayDeque<Long> queue = new ArrayDeque<>();
        private final List<Long> pushed = new ArrayList<>();

        @Override
        public void push(long eventId) {
            pushed.add(eventId);
            queue.addFirst(eventId);
        }

        @Override
        public Optional<Long> pop(Duration timeout) {
            return Optional.ofNullable(queue.pollLast());
        }

        List<Long> pushed() {
            return List.copyOf(pushed);
        }

        void clear() {
            queue.clear();
            pushed.clear();
        }
    }

    static class FakeMarketDataGateway extends coin.coinzzickmock.testsupport.TestMarketDataGateway {
        private int failuresBeforeSuccess;
        private boolean alwaysEmpty;
        private boolean alwaysThrow;
        private int minuteCandleLoadCalls;

        @Override
        public List<MarketSnapshot> loadSupportedMarkets() {
            return List.of(new MarketSnapshot("BTCUSDT", "BTCUSDT Perpetual", 101000, 100950, 100900, 0.0001, 3.2));
        }

        @Override
        public MarketSnapshot loadMarket(String symbol) {
            return loadSupportedMarkets().stream()
                    .filter(snapshot -> snapshot.symbol().equals(symbol))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<MarketMinuteCandleSnapshot> loadMinuteCandles(
                String symbol,
                Instant fromInclusive,
                Instant toExclusive
        ) {
            minuteCandleLoadCalls++;
            if (alwaysThrow) {
                throw new IllegalStateException("provider unavailable");
            }
            if (alwaysEmpty) {
                return List.of();
            }
            if (failuresBeforeSuccess > 0) {
                failuresBeforeSuccess--;
                throw new IllegalStateException("transient provider failure");
            }
            return MarketHistoryRepairFlowTest.minuteCandles(fromInclusive, toExclusive);
        }

        void failuresBeforeSuccess(int failuresBeforeSuccess) {
            this.failuresBeforeSuccess = failuresBeforeSuccess;
        }

        void alwaysEmpty() {
            this.alwaysEmpty = true;
        }

        void alwaysThrow() {
            this.alwaysThrow = true;
        }

        void reset() {
            failuresBeforeSuccess = 0;
            alwaysEmpty = false;
            alwaysThrow = false;
            minuteCandleLoadCalls = 0;
        }

        int minuteCandleLoadCalls() {
            return minuteCandleLoadCalls;
        }

    }
}
