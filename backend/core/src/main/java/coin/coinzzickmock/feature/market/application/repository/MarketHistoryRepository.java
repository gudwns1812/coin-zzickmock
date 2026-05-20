package coin.coinzzickmock.feature.market.application.repository;

import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MarketHistoryRepository {
    record StartupBackfillCursor(
            long symbolId,
            String symbol,
            Instant latestPersistedMinuteOpenTime
    ) {
    }

    Map<String, Long> findSymbolIdsBySymbols(List<String> symbols);

    Optional<String> findSymbolById(long symbolId);

    List<StartupBackfillCursor> findStartupBackfillCursors();

    Optional<Instant> findLatestMinuteCandleOpenTime(long symbolId);

    Optional<Instant> findLatestMinuteCandleOpenTimeBefore(long symbolId, Instant beforeExclusive);

    Optional<Instant> findLatestHourlyCandleOpenTime(long symbolId);

    Optional<Instant> findLatestHourlyCandleOpenTimeBefore(long symbolId, Instant beforeExclusive);

    /**
     * Returns the latest REST-visible completed hourly candle open time.
     * Implementations must read the persisted hourly projection directly and must not rescan
     * minute-candle coverage on this read path. Hourly row completeness is guaranteed by the
     * write/rebuild path that creates {@link HourlyMarketCandle} rows.
     */
    Optional<Instant> findLatestCompletedHourlyCandleOpenTime(long symbolId);

    /**
     * Returns the latest REST-visible completed hourly candle open time before the exclusive cursor.
     * The same persisted-hourly contract as {@link #findLatestCompletedHourlyCandleOpenTime(long)}
     * applies.
     */
    Optional<Instant> findLatestCompletedHourlyCandleOpenTimeBefore(long symbolId, Instant beforeExclusive);

    Optional<MarketHistoryCandle> findMinuteCandle(long symbolId, Instant openTime);

    List<MarketHistoryCandle> findMinuteCandles(long symbolId, Instant fromInclusive, Instant toExclusive);

    Optional<HourlyMarketCandle> findHourlyCandle(long symbolId, Instant openTime);

    List<HourlyMarketCandle> findHourlyCandles(long symbolId, Instant fromInclusive, Instant toExclusive);

    /**
     * Reads REST-visible completed hourly candles from the persisted hourly projection.
     * This method is the source for direct {@code 1h} REST history and for {@code 4h+}
     * persisted rollups, so implementations must not include provisional/live buckets or
     * perform request-time {@code 1m} coverage scans.
     */
    List<HourlyMarketCandle> findCompletedHourlyCandles(
            long symbolId,
            Instant fromInclusive,
            Instant toExclusive
    );

    void saveMinuteCandle(MarketHistoryCandle candle);

    boolean createMinuteCandleIfAbsent(MarketHistoryCandle candle);

    void saveHourlyCandle(HourlyMarketCandle candle);

}
