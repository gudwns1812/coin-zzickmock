package coin.coinzzickmock.feature.market.infrastructure.persistence;

import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.domain.CompletedMarketCandle;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private static final String ONE_MINUTE_SOURCE_INTERVAL = "ONE_MINUTE";

    private final MarketSymbolEntityRepository marketSymbolEntityRepository;
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
    public Optional<String> findSymbolById(long symbolId) {
        return marketSymbolEntityRepository.findById(symbolId).map(MarketSymbolEntity::symbol);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StartupBackfillCursor> findStartupBackfillCursors() {
        return marketSymbolEntityRepository.findAllByOrderByIdAsc().stream()
                .map(entity -> new StartupBackfillCursor(
                        entity.id(),
                        entity.symbol(),
                        findLatestMinuteCandleOpenTime(entity.id()).orElse(null)
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestMinuteCandleOpenTime(long symbolId) {
        return findLatestOpenTime(
                "SELECT open_time FROM market_candles_1m WHERE symbol_id = ? ORDER BY open_time DESC LIMIT 1",
                symbolId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestMinuteCandleOpenTimeBefore(long symbolId, Instant beforeExclusive) {
        return findLatestOpenTime(
                """
                        SELECT open_time
                        FROM market_candles_1m
                        WHERE symbol_id = ? AND open_time < ?
                        ORDER BY open_time DESC
                        LIMIT 1
                        """,
                symbolId,
                databaseDateTime(beforeExclusive)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestHourlyCandleOpenTime(long symbolId) {
        return findLatestOpenTime(
                """
                        SELECT open_time
                        FROM market_completed_candles
                        WHERE symbol_id = ? AND candle_interval = ?
                        ORDER BY open_time DESC
                        LIMIT 1
                        """,
                symbolId,
                MarketCandleInterval.ONE_HOUR.completedCandleDbToken()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestHourlyCandleOpenTimeBefore(long symbolId, Instant beforeExclusive) {
        return findLatestOpenTime(
                """
                        SELECT open_time
                        FROM market_completed_candles
                        WHERE symbol_id = ? AND candle_interval = ? AND open_time < ?
                        ORDER BY open_time DESC
                        LIMIT 1
                """,
                symbolId,
                MarketCandleInterval.ONE_HOUR.completedCandleDbToken(),
                databaseDateTime(beforeExclusive)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestCompletedHourlyCandleOpenTime(long symbolId) {
        return findLatestHourlyCandleOpenTime(symbolId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestCompletedHourlyCandleOpenTimeBefore(long symbolId, Instant beforeExclusive) {
        return findLatestHourlyCandleOpenTimeBefore(symbolId, beforeExclusive);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MarketHistoryCandle> findMinuteCandle(long symbolId, Instant openTime) {
        return jdbcTemplate.query(
                        """
                                SELECT symbol_id, open_time, close_time, open_price, high_price, low_price,
                                       close_price, volume, quote_volume
                                FROM market_candles_1m
                                WHERE symbol_id = ? AND open_time = ?
                                """,
                        this::minuteCandle,
                        symbolId,
                        databaseDateTime(openTime)
                )
                .stream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketHistoryCandle> findMinuteCandles(long symbolId, Instant fromInclusive, Instant toExclusive) {
        return jdbcTemplate.query(
                """
                        SELECT symbol_id, open_time, close_time, open_price, high_price, low_price,
                               close_price, volume, quote_volume
                        FROM market_candles_1m
                        WHERE symbol_id = ? AND open_time >= ? AND open_time < ?
                        ORDER BY open_time ASC
                        """,
                this::minuteCandle,
                symbolId,
                databaseDateTime(fromInclusive),
                databaseDateTime(toExclusive)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HourlyMarketCandle> findHourlyCandle(long symbolId, Instant openTime) {
        return jdbcTemplate.query(
                        """
                                SELECT symbol_id, open_time, close_time, open_price, high_price, low_price,
                                       close_price, volume, quote_volume, source_open_time AS source_minute_open_time,
                                       source_close_time AS source_minute_close_time
                                FROM market_completed_candles
                                WHERE symbol_id = ? AND candle_interval = ? AND open_time = ?
                                """,
                        this::hourlyCandle,
                        symbolId,
                        MarketCandleInterval.ONE_HOUR.completedCandleDbToken(),
                        databaseDateTime(openTime)
                )
                .stream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HourlyMarketCandle> findHourlyCandles(long symbolId, Instant fromInclusive, Instant toExclusive) {
        return jdbcTemplate.query(
                """
                        SELECT symbol_id, open_time, close_time, open_price, high_price, low_price,
                               close_price, volume, quote_volume, source_open_time AS source_minute_open_time,
                               source_close_time AS source_minute_close_time
                        FROM market_completed_candles
                        WHERE symbol_id = ? AND candle_interval = ? AND open_time >= ? AND open_time < ?
                        ORDER BY open_time ASC
                        """,
                this::hourlyCandle,
                symbolId,
                MarketCandleInterval.ONE_HOUR.completedCandleDbToken(),
                databaseDateTime(fromInclusive),
                databaseDateTime(toExclusive)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<HourlyMarketCandle> findCompletedHourlyCandles(
            long symbolId,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        return findHourlyCandles(symbolId, fromInclusive, toExclusive);
    }

    @Override
    @Transactional
    public void saveMinuteCandle(MarketHistoryCandle candle) {
        LocalDateTime now = databaseDateTime(Instant.now());
        jdbcTemplate.update(
                """
                        INSERT INTO market_candles_1m (
                            symbol_id, open_time, close_time, open_price, high_price, low_price,
                            close_price, volume, quote_volume, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            close_time = VALUES(close_time),
                            open_price = VALUES(open_price),
                            high_price = VALUES(high_price),
                            low_price = VALUES(low_price),
                            close_price = VALUES(close_price),
                            volume = VALUES(volume),
                            quote_volume = VALUES(quote_volume),
                            updated_at = ?
                """,
                candle.symbolId(),
                databaseDateTime(candle.openTime()),
                databaseDateTime(candle.closeTime()),
                decimal(candle.openPrice()),
                decimal(candle.highPrice()),
                decimal(candle.lowPrice()),
                decimal(candle.closePrice()),
                decimal(candle.volume()),
                decimal(candle.quoteVolume()),
                now,
                now,
                now
        );
    }

    @Override
    @Transactional
    public void saveHourlyCandle(HourlyMarketCandle candle) {
        saveCompletedCandle(CompletedMarketCandle.fromHourly(candle));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestCompletedCandleOpenTime(long symbolId, MarketCandleInterval interval) {
        return findLatestOpenTime(
                """
                        SELECT open_time
                        FROM market_completed_candles
                        WHERE symbol_id = ? AND candle_interval = ?
                        ORDER BY open_time DESC
                        LIMIT 1
                        """,
                symbolId,
                interval.completedCandleDbToken()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestCompletedCandleOpenTimeBefore(
            long symbolId,
            MarketCandleInterval interval,
            Instant beforeExclusive
    ) {
        return findLatestOpenTime(
                """
                        SELECT open_time
                        FROM market_completed_candles
                        WHERE symbol_id = ? AND candle_interval = ? AND open_time < ?
                        ORDER BY open_time DESC
                        LIMIT 1
                        """,
                symbolId,
                interval.completedCandleDbToken(),
                databaseDateTime(beforeExclusive)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsCompletedCandle(long symbolId, MarketCandleInterval interval) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM market_completed_candles
                        WHERE symbol_id = ? AND candle_interval = ?
                        """,
                Integer.class,
                symbolId,
                interval.completedCandleDbToken()
        );
        return count != null && count > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompletedMarketCandle> findCompletedCandles(
            long symbolId,
            MarketCandleInterval interval,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        return jdbcTemplate.query(
                """
                        SELECT symbol_id, candle_interval, open_time, close_time, open_price, high_price, low_price,
                               close_price, volume, quote_volume, source_interval, source_open_time,
                               source_close_time, source_candle_count
                        FROM market_completed_candles
                        WHERE symbol_id = ? AND candle_interval = ? AND open_time >= ? AND open_time < ?
                        ORDER BY open_time ASC
                        """,
                this::completedCandle,
                symbolId,
                interval.completedCandleDbToken(),
                databaseDateTime(fromInclusive),
                databaseDateTime(toExclusive)
        );
    }

    @Override
    @Transactional
    public void saveCompletedCandle(CompletedMarketCandle candle) {
        LocalDateTime now = databaseDateTime(Instant.now());
        jdbcTemplate.update(
                """
                        INSERT INTO market_completed_candles (
                            symbol_id, candle_interval, open_time, close_time, open_price, high_price,
                            low_price, close_price, volume, quote_volume, source_interval,
                            source_open_time, source_close_time, source_candle_count, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            close_time = VALUES(close_time),
                            open_price = VALUES(open_price),
                            high_price = VALUES(high_price),
                            low_price = VALUES(low_price),
                            close_price = VALUES(close_price),
                            volume = VALUES(volume),
                            quote_volume = VALUES(quote_volume),
                            source_interval = VALUES(source_interval),
                            source_open_time = VALUES(source_open_time),
                            source_close_time = VALUES(source_close_time),
                            source_candle_count = VALUES(source_candle_count),
                            updated_at = ?
                """,
                candle.symbolId(),
                candle.interval().completedCandleDbToken(),
                databaseDateTime(candle.openTime()),
                databaseDateTime(candle.closeTime()),
                decimal(candle.openPrice()),
                decimal(candle.highPrice()),
                decimal(candle.lowPrice()),
                decimal(candle.closePrice()),
                decimal(candle.volume()),
                decimal(candle.quoteVolume()),
                candle.sourceInterval() == MarketCandleInterval.ONE_MINUTE
                        ? ONE_MINUTE_SOURCE_INTERVAL
                        : candle.sourceInterval().completedCandleDbToken(),
                databaseDateTime(candle.sourceOpenTime()),
                databaseDateTime(candle.sourceCloseTime()),
                candle.sourceCandleCount(),
                now,
                now,
                now
        );
    }

    private Optional<Instant> findLatestOpenTime(String sql, Object... args) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> databaseInstant(rs, "open_time"), args)
                .stream()
                .findFirst();
    }

    private MarketHistoryCandle minuteCandle(ResultSet resultSet, int rowNumber) throws SQLException {
        BigDecimal quoteVolume = resultSet.getBigDecimal("quote_volume");
        return new MarketHistoryCandle(
                resultSet.getLong("symbol_id"),
                databaseInstant(resultSet, "open_time"),
                databaseInstant(resultSet, "close_time"),
                requiredDecimal(resultSet, "open_price").doubleValue(),
                requiredDecimal(resultSet, "high_price").doubleValue(),
                requiredDecimal(resultSet, "low_price").doubleValue(),
                requiredDecimal(resultSet, "close_price").doubleValue(),
                requiredDecimal(resultSet, "volume").doubleValue(),
                quoteVolume == null ? 0.0 : quoteVolume.doubleValue()
        );
    }

    private CompletedMarketCandle completedCandle(ResultSet resultSet, int rowNumber) throws SQLException {
        BigDecimal quoteVolume = resultSet.getBigDecimal("quote_volume");
        return new CompletedMarketCandle(
                resultSet.getLong("symbol_id"),
                completedInterval(resultSet.getString("candle_interval")),
                databaseInstant(resultSet, "open_time"),
                databaseInstant(resultSet, "close_time"),
                requiredDecimal(resultSet, "open_price").doubleValue(),
                requiredDecimal(resultSet, "high_price").doubleValue(),
                requiredDecimal(resultSet, "low_price").doubleValue(),
                requiredDecimal(resultSet, "close_price").doubleValue(),
                requiredDecimal(resultSet, "volume").doubleValue(),
                quoteVolume == null ? 0.0 : quoteVolume.doubleValue(),
                sourceInterval(resultSet.getString("source_interval")),
                databaseInstant(resultSet, "source_open_time"),
                databaseInstant(resultSet, "source_close_time"),
                resultSet.getInt("source_candle_count")
        );
    }

    private MarketCandleInterval completedInterval(String token) {
        return switch (token) {
            case "ONE_HOUR" -> MarketCandleInterval.ONE_HOUR;
            case "ONE_DAY" -> MarketCandleInterval.ONE_DAY;
            case "ONE_MONTH" -> MarketCandleInterval.ONE_MONTH;
            default -> throw new IllegalArgumentException("Unsupported completed candle interval: " + token);
        };
    }

    private MarketCandleInterval sourceInterval(String token) {
        if ("ONE_MINUTE".equals(token)) {
            return MarketCandleInterval.ONE_MINUTE;
        }
        return completedInterval(token);
    }

    private HourlyMarketCandle hourlyCandle(ResultSet resultSet, int rowNumber) throws SQLException {
        BigDecimal quoteVolume = resultSet.getBigDecimal("quote_volume");
        return new HourlyMarketCandle(
                resultSet.getLong("symbol_id"),
                databaseInstant(resultSet, "open_time"),
                databaseInstant(resultSet, "close_time"),
                requiredDecimal(resultSet, "open_price").doubleValue(),
                requiredDecimal(resultSet, "high_price").doubleValue(),
                requiredDecimal(resultSet, "low_price").doubleValue(),
                requiredDecimal(resultSet, "close_price").doubleValue(),
                requiredDecimal(resultSet, "volume").doubleValue(),
                quoteVolume == null ? 0.0 : quoteVolume.doubleValue(),
                databaseInstant(resultSet, "source_minute_open_time"),
                databaseInstant(resultSet, "source_minute_close_time")
        );
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }

    private static Instant databaseInstant(ResultSet resultSet, String columnName) throws SQLException {
        LocalDateTime value = resultSet.getObject(columnName, LocalDateTime.class);
        if (value == null) {
            throw new SQLException("Market candle column must not be null: " + columnName);
        }
        return value.toInstant(ZoneOffset.UTC);
    }

    private static BigDecimal requiredDecimal(ResultSet resultSet, String columnName) throws SQLException {
        BigDecimal value = resultSet.getBigDecimal(columnName);
        if (value == null) {
            throw new SQLException("Market candle column must not be null: " + columnName);
        }
        return value;
    }

    private static LocalDateTime databaseDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
