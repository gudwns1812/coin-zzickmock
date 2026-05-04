package coin.coinzzickmock.feature.market.infrastructure.persistence;

import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class MarketHistoryPersistenceRepository implements MarketHistoryRepository {
    private static final DateTimeFormatter UTC_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
    private static final RowMapper<Instant> OPEN_TIME_MAPPER =
            (rs, rowNum) -> utcInstant(rs.getObject("open_time", LocalDateTime.class));

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
    public List<StartupBackfillCursor> findStartupBackfillCursors() {
        return marketSymbolEntityRepository.findAllByOrderByIdAsc().stream()
                .map(entity -> new StartupBackfillCursor(
                        entity.id(),
                        entity.symbol(),
                        findLatestMinuteCandleOpenTime(entity.id())
                                .orElse(null)
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestMinuteCandleOpenTime(long symbolId) {
        return jdbcTemplate.query(
                """
                        SELECT open_time
                        FROM market_candles_1m
                        WHERE symbol_id = ?
                        ORDER BY open_time DESC
                        LIMIT 1
                        """,
                OPEN_TIME_MAPPER,
                symbolId
        ).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestMinuteCandleOpenTimeBefore(long symbolId, Instant beforeExclusive) {
        return jdbcTemplate.query(
                """
                        SELECT open_time
                        FROM market_candles_1m
                        WHERE symbol_id = ? AND open_time < ?
                        ORDER BY open_time DESC
                        LIMIT 1
                        """,
                OPEN_TIME_MAPPER,
                symbolId,
                utcDateTimeText(beforeExclusive)
        ).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestHourlyCandleOpenTime(long symbolId) {
        return jdbcTemplate.query(
                """
                        SELECT open_time
                        FROM market_candles_1h
                        WHERE symbol_id = ?
                        ORDER BY open_time DESC
                        LIMIT 1
                        """,
                OPEN_TIME_MAPPER,
                symbolId
        ).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestHourlyCandleOpenTimeBefore(long symbolId, Instant beforeExclusive) {
        return jdbcTemplate.query(
                """
                        SELECT open_time
                        FROM market_candles_1h
                        WHERE symbol_id = ? AND open_time < ?
                        ORDER BY open_time DESC
                        LIMIT 1
                        """,
                OPEN_TIME_MAPPER,
                symbolId,
                utcDateTimeText(beforeExclusive)
        ).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestCompletedHourlyCandleOpenTime(long symbolId) {
        return findLatestCompletedHourlyCandleOpenTimeBefore(symbolId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestCompletedHourlyCandleOpenTimeBefore(long symbolId, Instant beforeExclusive) {
        List<HourlyMarketCandle> candidates;
        if (beforeExclusive == null) {
            candidates = jdbcTemplate.query(
                    """
                            SELECT symbol_id, open_time, close_time, open_price, high_price, low_price,
                                   close_price, volume, quote_volume, source_minute_open_time, source_minute_close_time
                            FROM market_candles_1h
                            WHERE symbol_id = ?
                            ORDER BY open_time DESC
                            """,
                    (rs, rowNum) -> toHourlyCandle(rs),
                    symbolId
            );
        } else {
            candidates = jdbcTemplate.query(
                    """
                            SELECT symbol_id, open_time, close_time, open_price, high_price, low_price,
                                   close_price, volume, quote_volume, source_minute_open_time, source_minute_close_time
                            FROM market_candles_1h
                            WHERE symbol_id = ? AND open_time < ?
                            ORDER BY open_time DESC
                            """,
                    (rs, rowNum) -> toHourlyCandle(rs),
                    symbolId,
                    utcDateTimeText(beforeExclusive)
            );
        }

        return candidates.stream()
                .filter(this::hasContiguousMinuteCoverage)
                .map(HourlyMarketCandle::openTime)
                .findFirst();
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
                (rs, rowNum) -> toMinuteCandle(rs),
                symbolId,
                utcDateTimeText(openTime)
        ).stream().findFirst();
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
                (rs, rowNum) -> toMinuteCandle(rs),
                symbolId,
                utcDateTimeText(fromInclusive),
                utcDateTimeText(toExclusive)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HourlyMarketCandle> findHourlyCandle(long symbolId, Instant openTime) {
        return jdbcTemplate.query(
                """
                        SELECT symbol_id, open_time, close_time, open_price, high_price, low_price,
                               close_price, volume, quote_volume, source_minute_open_time, source_minute_close_time
                        FROM market_candles_1h
                        WHERE symbol_id = ? AND open_time = ?
                        """,
                (rs, rowNum) -> toHourlyCandle(rs),
                symbolId,
                utcDateTimeText(openTime)
        ).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HourlyMarketCandle> findHourlyCandles(long symbolId, Instant fromInclusive, Instant toExclusive) {
        return jdbcTemplate.query(
                """
                        SELECT symbol_id, open_time, close_time, open_price, high_price, low_price,
                               close_price, volume, quote_volume, source_minute_open_time, source_minute_close_time
                        FROM market_candles_1h
                        WHERE symbol_id = ? AND open_time >= ? AND open_time < ?
                        ORDER BY open_time ASC
                        """,
                (rs, rowNum) -> toHourlyCandle(rs),
                symbolId,
                utcDateTimeText(fromInclusive),
                utcDateTimeText(toExclusive)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<HourlyMarketCandle> findCompletedHourlyCandles(
            long symbolId,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        return findHourlyCandles(symbolId, fromInclusive, toExclusive).stream()
                .filter(this::hasContiguousMinuteCoverage)
                .toList();
    }

    @Override
    @Transactional
    public void saveMinuteCandle(MarketHistoryCandle candle) {
        String now = utcDateTimeText(Instant.now());
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
                utcDateTimeText(candle.openTime()),
                utcDateTimeText(candle.closeTime()),
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
        String now = utcDateTimeText(Instant.now());
        jdbcTemplate.update(
                """
                        INSERT INTO market_candles_1h (
                            symbol_id, open_time, close_time, open_price, high_price, low_price,
                            close_price, volume, quote_volume, source_minute_open_time,
                            source_minute_close_time, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                            updated_at = ?
                        """,
                candle.symbolId(),
                utcDateTimeText(candle.openTime()),
                utcDateTimeText(candle.closeTime()),
                decimal(candle.openPrice()),
                decimal(candle.highPrice()),
                decimal(candle.lowPrice()),
                decimal(candle.closePrice()),
                decimal(candle.volume()),
                decimal(candle.quoteVolume()),
                utcDateTimeText(candle.sourceMinuteOpenTime()),
                utcDateTimeText(candle.sourceMinuteCloseTime()),
                now,
                now,
                now
        );
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }

    private static MarketHistoryCandle toMinuteCandle(ResultSet rs) throws SQLException {
        return new MarketHistoryCandle(
                rs.getLong("symbol_id"),
                utcInstant(rs.getObject("open_time", LocalDateTime.class)),
                utcInstant(rs.getObject("close_time", LocalDateTime.class)),
                rs.getBigDecimal("open_price").doubleValue(),
                rs.getBigDecimal("high_price").doubleValue(),
                rs.getBigDecimal("low_price").doubleValue(),
                rs.getBigDecimal("close_price").doubleValue(),
                rs.getBigDecimal("volume").doubleValue(),
                nullableDouble(rs.getBigDecimal("quote_volume"))
        );
    }

    private static HourlyMarketCandle toHourlyCandle(ResultSet rs) throws SQLException {
        return new HourlyMarketCandle(
                rs.getLong("symbol_id"),
                utcInstant(rs.getObject("open_time", LocalDateTime.class)),
                utcInstant(rs.getObject("close_time", LocalDateTime.class)),
                rs.getBigDecimal("open_price").doubleValue(),
                rs.getBigDecimal("high_price").doubleValue(),
                rs.getBigDecimal("low_price").doubleValue(),
                rs.getBigDecimal("close_price").doubleValue(),
                rs.getBigDecimal("volume").doubleValue(),
                nullableDouble(rs.getBigDecimal("quote_volume")),
                utcInstant(rs.getObject("source_minute_open_time", LocalDateTime.class)),
                utcInstant(rs.getObject("source_minute_close_time", LocalDateTime.class))
        );
    }

    private static LocalDateTime utcDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static String utcDateTimeText(Instant instant) {
        return UTC_DATETIME_FORMATTER.format(utcDateTime(instant));
    }

    private static Instant utcInstant(LocalDateTime dateTime) {
        return dateTime.toInstant(ZoneOffset.UTC);
    }

    private boolean hasContiguousMinuteCoverage(HourlyMarketCandle candle) {
        if (!candle.closeTime().equals(candle.openTime().plus(1, ChronoUnit.HOURS))) {
            return false;
        }

        List<Instant> storedMinuteOpenTimes = jdbcTemplate.query(
                """
                        SELECT open_time
                        FROM market_candles_1m
                        WHERE symbol_id = ? AND open_time >= ? AND open_time < ?
                        ORDER BY open_time ASC
                        """,
                OPEN_TIME_MAPPER,
                candle.symbolId(),
                utcDateTimeText(candle.openTime()),
                utcDateTimeText(candle.closeTime())
        );
        Set<Instant> storedMinuteOpenTimeSet = new HashSet<>(storedMinuteOpenTimes);

        Instant expectedMinuteOpenTime = candle.openTime();
        while (expectedMinuteOpenTime.isBefore(candle.closeTime())) {
            if (!storedMinuteOpenTimeSet.contains(expectedMinuteOpenTime)) {
                return false;
            }
            expectedMinuteOpenTime = expectedMinuteOpenTime.plus(1, ChronoUnit.MINUTES);
        }
        return expectedMinuteOpenTime.equals(candle.closeTime());
    }

    private static double nullableDouble(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }
}
