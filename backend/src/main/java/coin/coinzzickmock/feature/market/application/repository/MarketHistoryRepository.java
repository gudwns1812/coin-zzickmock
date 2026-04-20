package coin.coinzzickmock.feature.market.application.repository;

import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MarketHistoryRepository {
    Map<String, Long> findSymbolIdsBySymbols(List<String> symbols);

    default Optional<Instant> findLatestMinuteCandleOpenTime(long symbolId) {
        return Optional.empty();
    }

    Optional<MarketHistoryCandle> findMinuteCandle(long symbolId, Instant openTime);

    List<MarketHistoryCandle> findMinuteCandles(long symbolId, Instant fromInclusive, Instant toExclusive);

    Optional<HourlyMarketCandle> findHourlyCandle(long symbolId, Instant openTime);

    void saveMinuteCandle(MarketHistoryCandle candle);

    void saveHourlyCandle(HourlyMarketCandle candle);
}
