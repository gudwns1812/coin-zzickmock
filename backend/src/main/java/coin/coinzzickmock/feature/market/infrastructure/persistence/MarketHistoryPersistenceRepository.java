package coin.coinzzickmock.feature.market.infrastructure.persistence;

import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class MarketHistoryPersistenceRepository implements MarketHistoryRepository {
    private final MarketSymbolEntityRepository marketSymbolEntityRepository;
    private final MarketCandle1mEntityRepository marketCandle1mEntityRepository;
    private final MarketCandle1hEntityRepository marketCandle1hEntityRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> findSymbolIdsBySymbols(List<String> symbols) {
        Map<String, Long> symbolIds = new LinkedHashMap<>();
        marketSymbolEntityRepository.findAllBySymbolIn(symbols)
                .forEach(entity -> symbolIds.put(entity.symbol(), entity.id()));
        return symbolIds;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StartupBackfillCursor> findStartupBackfillCursors() {
        return marketSymbolEntityRepository.findAllByOrderByIdAsc().stream()
                .map(entity -> new StartupBackfillCursor(
                        entity.id(),
                        entity.symbol(),
                        marketCandle1mEntityRepository.findTopBySymbolIdOrderByOpenTimeDesc(entity.id())
                                .map(MarketCandle1mEntity::toDomain)
                                .map(MarketHistoryCandle::openTime)
                                .orElse(null)
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestMinuteCandleOpenTime(long symbolId) {
        return marketCandle1mEntityRepository.findTopBySymbolIdOrderByOpenTimeDesc(symbolId)
                .map(MarketCandle1mEntity::toDomain)
                .map(MarketHistoryCandle::openTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestMinuteCandleOpenTimeBefore(long symbolId, Instant beforeExclusive) {
        return marketCandle1mEntityRepository.findTopBySymbolIdAndOpenTimeLessThanOrderByOpenTimeDesc(symbolId, beforeExclusive)
                .map(MarketCandle1mEntity::toDomain)
                .map(MarketHistoryCandle::openTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestHourlyCandleOpenTime(long symbolId) {
        return marketCandle1hEntityRepository.findTopBySymbolIdOrderByOpenTimeDesc(symbolId)
                .map(MarketCandle1hEntity::toDomain)
                .map(HourlyMarketCandle::openTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestHourlyCandleOpenTimeBefore(long symbolId, Instant beforeExclusive) {
        return marketCandle1hEntityRepository.findTopBySymbolIdAndOpenTimeLessThanOrderByOpenTimeDesc(symbolId, beforeExclusive)
                .map(MarketCandle1hEntity::toDomain)
                .map(HourlyMarketCandle::openTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MarketHistoryCandle> findMinuteCandle(long symbolId, Instant openTime) {
        return marketCandle1mEntityRepository.findBySymbolIdAndOpenTime(symbolId, openTime)
                .map(MarketCandle1mEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketHistoryCandle> findMinuteCandles(long symbolId, Instant fromInclusive, Instant toExclusive) {
        return marketCandle1mEntityRepository
                .findAllBySymbolIdAndOpenTimeGreaterThanEqualAndOpenTimeLessThanOrderByOpenTimeAsc(
                        Long.valueOf(symbolId),
                        fromInclusive,
                        toExclusive
                )
                .stream()
                .map(MarketCandle1mEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HourlyMarketCandle> findHourlyCandle(long symbolId, Instant openTime) {
        return marketCandle1hEntityRepository.findBySymbolIdAndOpenTime(symbolId, openTime)
                .map(MarketCandle1hEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HourlyMarketCandle> findHourlyCandles(long symbolId, Instant fromInclusive, Instant toExclusive) {
        return marketCandle1hEntityRepository
                .findAllBySymbolIdAndOpenTimeGreaterThanEqualAndOpenTimeLessThanOrderByOpenTimeAsc(
                        Long.valueOf(symbolId),
                        fromInclusive,
                        toExclusive
                )
                .stream()
                .map(MarketCandle1hEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void saveMinuteCandle(MarketHistoryCandle candle) {
        jdbcTemplate.update(
                """
                        INSERT INTO market_candles_1m (
                            symbol_id, open_time, close_time, open_price, high_price, low_price,
                            close_price, volume, quote_volume, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                        ON DUPLICATE KEY UPDATE
                            close_time = VALUES(close_time),
                            open_price = VALUES(open_price),
                            high_price = VALUES(high_price),
                            low_price = VALUES(low_price),
                            close_price = VALUES(close_price),
                            volume = VALUES(volume),
                            quote_volume = VALUES(quote_volume),
                            updated_at = CURRENT_TIMESTAMP(6)
                        """,
                candle.symbolId(),
                candle.openTime(),
                candle.closeTime(),
                decimal(candle.openPrice()),
                decimal(candle.highPrice()),
                decimal(candle.lowPrice()),
                decimal(candle.closePrice()),
                decimal(candle.volume()),
                decimal(candle.quoteVolume())
        );
    }

    @Override
    @Transactional
    public void saveHourlyCandle(HourlyMarketCandle candle) {
        jdbcTemplate.update(
                """
                        INSERT INTO market_candles_1h (
                            symbol_id, open_time, close_time, open_price, high_price, low_price,
                            close_price, volume, quote_volume, source_minute_open_time,
                            source_minute_close_time, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                        ON DUPLICATE KEY UPDATE
                            close_time = VALUES(close_time),
                            open_price = VALUES(open_price),
                            high_price = VALUES(high_price),
                            low_price = VALUES(low_price),
                            close_price = VALUES(close_price),
                            volume = VALUES(volume),
                            quote_volume = VALUES(quote_volume),
                            source_minute_open_time = VALUES(source_minute_open_time),
                            source_minute_close_time = VALUES(source_minute_close_time),
                            updated_at = CURRENT_TIMESTAMP(6)
                        """,
                candle.symbolId(),
                candle.openTime(),
                candle.closeTime(),
                decimal(candle.openPrice()),
                decimal(candle.highPrice()),
                decimal(candle.lowPrice()),
                decimal(candle.closePrice()),
                decimal(candle.volume()),
                decimal(candle.quoteVolume()),
                candle.sourceMinuteOpenTime(),
                candle.sourceMinuteCloseTime()
        );
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
