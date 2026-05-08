package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.application.repair.MarketHistoryRepairRequestRecorder;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MarketHistoryRecorderTest {
    private static final Instant CLOSED_HOUR_NOW = Instant.parse("2026-04-17T08:00:00Z");

    @Test
    void savesHourlyCandleWhenRebuiltHourHasCompleteMinuteCoverage() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        MarketHistoryRecorder recorder = recorder(repository);
        Instant hourOpenTime = Instant.parse("2026-04-17T06:00:00Z");

        recorder.recordHistoricalMinuteCandles(1L, minuteSnapshots(hourOpenTime, -1));

        assertThat(repository.hourlyCandles).containsKey(key(1L, hourOpenTime));
    }

    @Test
    void keepsExistingHourlyCandleWhenRebuiltHourIsIncomplete() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        MarketHistoryRecorder recorder = recorder(repository);
        Instant hourOpenTime = Instant.parse("2026-04-17T06:00:00Z");
        HourlyMarketCandle original = hourly(1L, hourOpenTime);
        repository.saveHourlyCandle(original);

        recorder.recordHistoricalMinuteCandles(1L, minuteSnapshots(hourOpenTime, 30));

        assertThat(repository.hourlyCandles.get(key(1L, hourOpenTime))).isEqualTo(original);
    }

    @Test
    void doesNotSaveHourlyCandleWhenRebuiltHourIsIncomplete() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        MarketHistoryRecorder recorder = recorder(repository);
        Instant hourOpenTime = Instant.parse("2026-04-17T06:00:00Z");

        recorder.recordHistoricalMinuteCandles(1L, minuteSnapshots(hourOpenTime, 30));

        assertThat(repository.hourlyCandles).isEmpty();
    }

    @Test
    void skipsHourlyRebuildWhenAffectedHourIsStillOpen() {
        InMemoryMarketHistoryRepository repository = new InMemoryMarketHistoryRepository();
        Instant hourOpenTime = Instant.parse("2026-04-17T06:00:00Z");
        MarketHistoryRecorder recorder = recorder(
                repository,
                Instant.parse("2026-04-17T06:15:01Z")
        );

        recorder.recordHistoricalMinuteCandles(1L, minuteSnapshots(hourOpenTime, 30));

        assertThat(repository.hourlyCandles).isEmpty();
        assertThat(repository.findMinuteCandlesCalls).isZero();
    }

    private static MarketHistoryRecorder recorder(MarketHistoryRepository repository) {
        return recorder(repository, CLOSED_HOUR_NOW);
    }

    private static MarketHistoryRecorder recorder(MarketHistoryRepository repository, Instant now) {
        return new MarketHistoryRecorder(
                repository,
                new CompletedHourlyCandleBuilder(),
                mock(AfterCommitEventPublisher.class),
                mock(MarketHistoryRepairRequestRecorder.class),
                Clock.fixed(now, ZoneOffset.UTC)
        );
    }

    private static List<MarketMinuteCandleSnapshot> minuteSnapshots(Instant hourOpenTime, int missingMinuteIndex) {
        List<MarketMinuteCandleSnapshot> snapshots = new ArrayList<>();
        for (int minute = 0; minute < 60; minute++) {
            if (minute == missingMinuteIndex) {
                continue;
            }
            Instant openTime = hourOpenTime.plusSeconds(minute * 60L);
            snapshots.add(new MarketMinuteCandleSnapshot(
                    openTime,
                    openTime.plusSeconds(60),
                    100 + minute,
                    101 + minute,
                    99 + minute,
                    100.5 + minute,
                    10,
                    1005
            ));
        }
        return snapshots;
    }

    private static HourlyMarketCandle hourly(long symbolId, Instant openTime) {
        return new HourlyMarketCandle(
                symbolId,
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

    private static String key(long symbolId, Instant openTime) {
        return symbolId + ":" + openTime;
    }

    private static class InMemoryMarketHistoryRepository extends coin.coinzzickmock.testsupport.TestMarketHistoryRepository {
        private final Map<String, MarketHistoryCandle> minuteCandles = new LinkedHashMap<>();
        private final Map<String, HourlyMarketCandle> hourlyCandles = new LinkedHashMap<>();
        private int findMinuteCandlesCalls;

        @Override
        public List<MarketHistoryCandle> findMinuteCandles(long symbolId, Instant fromInclusive, Instant toExclusive) {
            findMinuteCandlesCalls++;
            return minuteCandles.values().stream()
                    .filter(candle -> candle.symbolId() == symbolId)
                    .filter(candle -> !candle.openTime().isBefore(fromInclusive))
                    .filter(candle -> candle.openTime().isBefore(toExclusive))
                    .sorted(java.util.Comparator.comparing(MarketHistoryCandle::openTime))
                    .toList();
        }

        @Override
        public Optional<HourlyMarketCandle> findHourlyCandle(long symbolId, Instant openTime) {
            return Optional.ofNullable(hourlyCandles.get(key(symbolId, openTime)));
        }

        @Override
        public void saveMinuteCandle(MarketHistoryCandle candle) {
            minuteCandles.put(key(candle.symbolId(), candle.openTime()), candle);
        }

        @Override
        public void saveHourlyCandle(HourlyMarketCandle candle) {
            hourlyCandles.put(key(candle.symbolId(), candle.openTime()), candle);
        }

    }
}
