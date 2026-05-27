package coin.coinzzickmock.testsupport;

import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryStartupBackfillCursor;
import coin.coinzzickmock.feature.market.domain.CompletedMarketCandle;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class TestMarketHistoryRepository implements MarketHistoryRepository {
    @Override
    public Map<String, Long> findSymbolIdsBySymbols(List<String> symbols) {
        return Map.of();
    }

    @Override
    public Optional<String> findSymbolById(long symbolId) {
        return Optional.empty();
    }

    @Override
    public List<MarketHistoryStartupBackfillCursor> findStartupBackfillCursors() {
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
    public Optional<Instant> findLatestCompletedHourlyCandleOpenTime(long symbolId) {
        return findLatestHourlyCandleOpenTime(symbolId);
    }

    @Override
    public Optional<Instant> findLatestCompletedHourlyCandleOpenTimeBefore(long symbolId, Instant beforeExclusive) {
        return findLatestHourlyCandleOpenTimeBefore(symbolId, beforeExclusive);
    }

    @Override
    public Optional<Instant> findLatestCompletedCandleOpenTime(long symbolId, MarketCandleInterval interval) {
        if (interval == MarketCandleInterval.ONE_HOUR) {
            return findLatestCompletedHourlyCandleOpenTime(symbolId);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Instant> findLatestCompletedCandleOpenTimeBefore(
            long symbolId,
            MarketCandleInterval interval,
            Instant beforeExclusive
    ) {
        if (interval == MarketCandleInterval.ONE_HOUR) {
            return findLatestCompletedHourlyCandleOpenTimeBefore(symbolId, beforeExclusive);
        }
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
        return findHourlyCandles(symbolId, fromInclusive, toExclusive);
    }

    @Override
    public boolean existsCompletedCandle(long symbolId, MarketCandleInterval interval) {
        return false;
    }

    @Override
    public List<CompletedMarketCandle> findCompletedCandles(
            long symbolId,
            MarketCandleInterval interval,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        return List.of();
    }

    @Override
    public void saveMinuteCandle(MarketHistoryCandle candle) {
        throw new UnsupportedOperationException("saveMinuteCandle");
    }

    @Override
    public void saveHourlyCandle(HourlyMarketCandle candle) {
        throw new UnsupportedOperationException("saveHourlyCandle");
    }

    @Override
    public void saveCompletedCandle(CompletedMarketCandle candle) {
        throw new UnsupportedOperationException("saveCompletedCandle");
    }
}
