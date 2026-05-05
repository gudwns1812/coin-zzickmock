package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringJUnitConfig(classes = {
        MarketHistoryRecorder.class,
        MarketHistoryRecorderTransactionTest.RollbackHarness.class,
        MarketHistoryRecorderTransactionTest.TransactionTestConfiguration.class
})
class MarketHistoryRecorderTransactionTest {
    private static final Instant OPEN_TIME = Instant.parse("2026-04-17T06:00:00Z");

    @Autowired
    private MarketHistoryRecorder recorder;

    @Autowired
    private RecordingMarketHistoryRepository repository;

    @Autowired
    private CapturingEventPublisher eventPublisher;

    @Autowired
    private RollbackHarness rollbackHarness;

    @BeforeEach
    void setUp() {
        repository.reset();
        eventPublisher.reset();
    }

    @Test
    void recordsCandlesBySymbolInsideTransaction() {
        repository.replaceSymbolIds(Map.of("BTCUSDT", 1L));

        recorder.recordHistoricalMinuteCandlesBySymbol(Map.of("BTCUSDT", List.of(minuteCandle())));

        assertThat(repository.observedTransactionStates()).isNotEmpty().containsOnly(true);
    }

    @Test
    void recordsDirectMinuteCandlesInsideTransaction() {
        recorder.recordHistoricalMinuteCandles(1L, List.of(minuteCandle()));

        assertThat(repository.observedTransactionStates()).isNotEmpty().containsOnly(true);
    }

    @Test
    void publishesClosedMinuteFinalizedEventAfterCommit() {
        repository.replaceSymbolIds(Map.of("BTCUSDT", 1L));

        recorder.recordClosedMinuteCandlesBySymbol(
                Map.of("BTCUSDT", List.of(minuteCandle())),
                OPEN_TIME,
                OPEN_TIME.plus(1, ChronoUnit.MINUTES)
        );

        assertThat(eventPublisher.events())
                .containsExactly(new MarketHistoryFinalizedEvent(
                        "BTCUSDT",
                        OPEN_TIME,
                        OPEN_TIME.plus(1, ChronoUnit.MINUTES)
                ));
    }

    @Test
    void doesNotPublishClosedMinuteFinalizedEventAfterRollback() {
        repository.replaceSymbolIds(Map.of("BTCUSDT", 1L));

        assertThatThrownBy(() -> rollbackHarness.recordAndRollback(Map.of("BTCUSDT", List.of(minuteCandle()))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("rollback");

        assertThat(eventPublisher.events()).isEmpty();
    }

    private static MarketMinuteCandleSnapshot minuteCandle() {
        return new MarketMinuteCandleSnapshot(
                OPEN_TIME,
                OPEN_TIME.plus(1, ChronoUnit.MINUTES),
                101000.0,
                101500.0,
                100500.0,
                101250.0,
                10.0,
                1012500.0
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class TransactionTestConfiguration {
        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl("jdbc:h2:mem:market_history_recorder_tx;DB_CLOSE_DELAY=-1");
            return dataSource;
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        RecordingMarketHistoryRepository marketHistoryRepository() {
            return new RecordingMarketHistoryRepository();
        }

        @Bean
        CapturingEventPublisher capturingEventPublisher() {
            return new CapturingEventPublisher();
        }

        @Bean
        AfterCommitEventPublisher afterCommitEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
            return new AfterCommitEventPublisher(applicationEventPublisher);
        }
    }

    @Component
    static class RollbackHarness {
        private final MarketHistoryRecorder recorder;

        RollbackHarness(MarketHistoryRecorder recorder) {
            this.recorder = recorder;
        }

        @Transactional
        void recordAndRollback(Map<String, List<MarketMinuteCandleSnapshot>> minuteCandlesBySymbol) {
            recorder.recordClosedMinuteCandlesBySymbol(
                    minuteCandlesBySymbol,
                    OPEN_TIME,
                    OPEN_TIME.plus(1, ChronoUnit.MINUTES)
            );
            throw new IllegalStateException("rollback");
        }
    }

    static class CapturingEventPublisher {
        private final List<MarketHistoryFinalizedEvent> events = new ArrayList<>();

        @org.springframework.context.event.EventListener
        void onEvent(MarketHistoryFinalizedEvent event) {
            events.add(event);
        }

        void reset() {
            events.clear();
        }

        List<MarketHistoryFinalizedEvent> events() {
            return List.copyOf(events);
        }
    }

    static class RecordingMarketHistoryRepository extends coin.coinzzickmock.testsupport.TestMarketHistoryRepository {
        private final List<Boolean> observedTransactionStates = new ArrayList<>();
        private Map<String, Long> symbolIds = new LinkedHashMap<>();
        private MarketHistoryCandle lastSavedMinuteCandle;

        @Override
        public Map<String, Long> findSymbolIdsBySymbols(List<String> symbols) {
            recordTransactionState();
            Map<String, Long> results = new LinkedHashMap<>();
            symbols.forEach(symbol -> {
                Long symbolId = symbolIds.get(symbol);
                if (symbolId != null) {
                    results.put(symbol, symbolId);
                }
            });
            return results;
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
            recordTransactionState();
            if (lastSavedMinuteCandle == null) {
                return List.of();
            }
            return List.of(lastSavedMinuteCandle);
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
        public void saveMinuteCandle(MarketHistoryCandle candle) {
            recordTransactionState();
            lastSavedMinuteCandle = candle;
        }

        @Override
        public void saveHourlyCandle(HourlyMarketCandle candle) {
            recordTransactionState();
        }

        void reset() {
            observedTransactionStates.clear();
            symbolIds = new LinkedHashMap<>();
            lastSavedMinuteCandle = null;
        }

        void replaceSymbolIds(Map<String, Long> symbolIds) {
            this.symbolIds = new LinkedHashMap<>(symbolIds);
        }

        List<Boolean> observedTransactionStates() {
            return List.copyOf(observedTransactionStates);
        }

        private void recordTransactionState() {
            observedTransactionStates.add(TransactionSynchronizationManager.isActualTransactionActive());
        }
    }
}
