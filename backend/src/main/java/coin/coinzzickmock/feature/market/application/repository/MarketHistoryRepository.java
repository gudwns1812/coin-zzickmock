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

    Optional<Instant> findLatestCompletedHourlyCandleOpenTime(long symbolId);

    Optional<Instant> findLatestCompletedHourlyCandleOpenTimeBefore(long symbolId, Instant beforeExclusive);

    Optional<MarketHistoryCandle> findMinuteCandle(long symbolId, Instant openTime);

    List<MarketHistoryCandle> findMinuteCandles(long symbolId, Instant fromInclusive, Instant toExclusive);

    Optional<HourlyMarketCandle> findHourlyCandle(long symbolId, Instant openTime);

    List<HourlyMarketCandle> findHourlyCandles(long symbolId, Instant fromInclusive, Instant toExclusive);

    List<HourlyMarketCandle> findCompletedHourlyCandles(
            long symbolId,
            Instant fromInclusive,
            Instant toExclusive
    );

    void saveMinuteCandle(MarketHistoryCandle candle);

    void saveHourlyCandle(HourlyMarketCandle candle);

}
