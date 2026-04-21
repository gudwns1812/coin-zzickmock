package coin.coinzzickmock.feature.market.infrastructure.persistence;

import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class MarketHistoryPersistenceRepository implements MarketHistoryRepository {
    private final MarketSymbolEntityRepository marketSymbolEntityRepository;
    private final MarketCandle1mEntityRepository marketCandle1mEntityRepository;
    private final MarketCandle1hEntityRepository marketCandle1hEntityRepository;

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
    @Transactional
    public void saveMinuteCandle(MarketHistoryCandle candle) {
        MarketCandle1mEntity entity = marketCandle1mEntityRepository
                .findBySymbolIdAndOpenTime(candle.symbolId(), candle.openTime())
                .map(existing -> {
                    existing.apply(candle);
                    return existing;
                })
                .orElseGet(() -> MarketCandle1mEntity.from(candle));

        marketCandle1mEntityRepository.save(entity);
    }

    @Override
    @Transactional
    public void saveHourlyCandle(HourlyMarketCandle candle) {
        MarketCandle1hEntity entity = marketCandle1hEntityRepository
                .findBySymbolIdAndOpenTime(candle.symbolId(), candle.openTime())
                .map(existing -> {
                    existing.apply(candle);
                    return existing;
                })
                .orElseGet(() -> MarketCandle1hEntity.from(candle));

        marketCandle1hEntityRepository.save(entity);
    }
}
