package coin.coinzzickmock.feature.market.infrastructure.persistence;

import static coin.coinzzickmock.feature.market.infrastructure.persistence.QMarketCandle1hEntity.marketCandle1hEntity;
import static coin.coinzzickmock.feature.market.infrastructure.persistence.QMarketCandle1mEntity.marketCandle1mEntity;

import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final JPAQueryFactory jpaQueryFactory;
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
        return marketCandle1mEntityRepository
                .findTopBySymbolIdAndOpenTimeLessThanOrderByOpenTimeDesc(symbolId, jpaDateTime(beforeExclusive))
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
        return marketCandle1hEntityRepository
                .findTopBySymbolIdAndOpenTimeLessThanOrderByOpenTimeDesc(symbolId, jpaDateTime(beforeExclusive))
                .map(MarketCandle1hEntity::toDomain)
                .map(HourlyMarketCandle::openTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestCompletedHourlyCandleOpenTime(long symbolId) {
        return findLatestCompletedHourlyCandleOpenTimeBefore(symbolId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> findLatestCompletedHourlyCandleOpenTimeBefore(long symbolId, Instant beforeExclusive) {
        return findHourlyCandleCandidates(symbolId, beforeExclusive).stream()
                .map(MarketCandle1hEntity::toDomain)
                .filter(this::hasContiguousMinuteCoverage)
                .map(HourlyMarketCandle::openTime)
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MarketHistoryCandle> findMinuteCandle(long symbolId, Instant openTime) {
        return marketCandle1mEntityRepository.findBySymbolIdAndOpenTime(symbolId, jpaDateTime(openTime))
                .map(MarketCandle1mEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketHistoryCandle> findMinuteCandles(long symbolId, Instant fromInclusive, Instant toExclusive) {
        return marketCandle1mEntityRepository
                .findAllBySymbolIdAndOpenTimeGreaterThanEqualAndOpenTimeLessThanOrderByOpenTimeAsc(
                        symbolId,
                        jpaDateTime(fromInclusive),
                        jpaDateTime(toExclusive)
                )
                .stream()
                .map(MarketCandle1mEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HourlyMarketCandle> findHourlyCandle(long symbolId, Instant openTime) {
        return marketCandle1hEntityRepository.findBySymbolIdAndOpenTime(symbolId, jpaDateTime(openTime))
                .map(MarketCandle1hEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HourlyMarketCandle> findHourlyCandles(long symbolId, Instant fromInclusive, Instant toExclusive) {
        return marketCandle1hEntityRepository
                .findAllBySymbolIdAndOpenTimeGreaterThanEqualAndOpenTimeLessThanOrderByOpenTimeAsc(
                        symbolId,
                        jpaDateTime(fromInclusive),
                        jpaDateTime(toExclusive)
                )
                .stream()
                .map(MarketCandle1hEntity::toDomain)
                .toList();
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
        LocalDateTime now = utcDateTime(Instant.now());
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
                utcDateTime(candle.openTime()),
                utcDateTime(candle.closeTime()),
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
        LocalDateTime now = utcDateTime(Instant.now());
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
                utcDateTime(candle.openTime()),
                utcDateTime(candle.closeTime()),
                decimal(candle.openPrice()),
                decimal(candle.highPrice()),
                decimal(candle.lowPrice()),
                decimal(candle.closePrice()),
                decimal(candle.volume()),
                decimal(candle.quoteVolume()),
                utcDateTime(candle.sourceMinuteOpenTime()),
                utcDateTime(candle.sourceMinuteCloseTime()),
                now,
                now,
                now
        );
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }

    private static LocalDateTime utcDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static LocalDateTime jpaDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private boolean hasContiguousMinuteCoverage(HourlyMarketCandle candle) {
        if (!candle.closeTime().equals(candle.openTime().plus(1, ChronoUnit.HOURS))) {
            return false;
        }

        List<Instant> storedMinuteOpenTimes = jpaQueryFactory
                .select(marketCandle1mEntity.openTime)
                .from(marketCandle1mEntity)
                .where(
                        marketCandle1mEntity.symbolId.eq(candle.symbolId()),
                        marketCandle1mEntity.openTime.goe(jpaDateTime(candle.openTime())),
                        marketCandle1mEntity.openTime.lt(jpaDateTime(candle.closeTime()))
                )
                .orderBy(marketCandle1mEntity.openTime.asc())
                .fetch()
                .stream()
                .map(MarketHistoryPersistenceRepository::jpaInstant)
                .toList();
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

    private List<MarketCandle1hEntity> findHourlyCandleCandidates(long symbolId, Instant beforeExclusive) {
        if (beforeExclusive == null) {
            return jpaQueryFactory.selectFrom(marketCandle1hEntity)
                    .where(marketCandle1hEntity.symbolId.eq(symbolId))
                    .orderBy(marketCandle1hEntity.openTime.desc())
                    .fetch();
        }

        return jpaQueryFactory.selectFrom(marketCandle1hEntity)
                .where(
                        marketCandle1hEntity.symbolId.eq(symbolId),
                        marketCandle1hEntity.openTime.lt(jpaDateTime(beforeExclusive))
                )
                .orderBy(marketCandle1hEntity.openTime.desc())
                .fetch();
    }

    private static Instant jpaInstant(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
