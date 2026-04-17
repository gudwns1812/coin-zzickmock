package coin.coinzzickmock.feature.market.infrastructure.persistence;

import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaMarketHistoryRepository implements MarketHistoryRepository {
    private final MarketSymbolSpringDataRepository marketSymbolSpringDataRepository;
    private final MarketCandle1mSpringDataRepository marketCandle1mSpringDataRepository;
    private final MarketCandle1hSpringDataRepository marketCandle1hSpringDataRepository;

    public JpaMarketHistoryRepository(
            MarketSymbolSpringDataRepository marketSymbolSpringDataRepository,
            MarketCandle1mSpringDataRepository marketCandle1mSpringDataRepository,
            MarketCandle1hSpringDataRepository marketCandle1hSpringDataRepository
    ) {
        this.marketSymbolSpringDataRepository = marketSymbolSpringDataRepository;
        this.marketCandle1mSpringDataRepository = marketCandle1mSpringDataRepository;
        this.marketCandle1hSpringDataRepository = marketCandle1hSpringDataRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> findSymbolIdsBySymbols(List<String> symbols) {
        Map<String, Long> symbolIds = new LinkedHashMap<>();
        marketSymbolSpringDataRepository.findAllBySymbolIn(symbols)
                .forEach(entity -> symbolIds.put(entity.symbol(), entity.id()));
        return symbolIds;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketHistoryCandle> findMinuteCandles(long symbolId, Instant fromInclusive, Instant toExclusive) {
        return marketCandle1mSpringDataRepository
                .findAllBySymbolIdAndOpenTimeGreaterThanEqualAndOpenTimeLessThanOrderByOpenTimeAsc(
                        symbolId,
                        fromInclusive,
                        toExclusive
                )
                .stream()
                .map(MarketCandle1mEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void saveMinuteCandle(MarketHistoryCandle candle) {
        MarketCandle1mEntity entity = marketCandle1mSpringDataRepository
                .findAllBySymbolIdAndOpenTimeGreaterThanEqualAndOpenTimeLessThanOrderByOpenTimeAsc(
                        candle.symbolId(),
                        candle.openTime(),
                        candle.closeTime()
                )
                .stream()
                .findFirst()
                .map(existing -> {
                    existing.apply(candle);
                    return existing;
                })
                .orElseGet(() -> MarketCandle1mEntity.from(candle));

        marketCandle1mSpringDataRepository.save(entity);
    }

    @Override
    @Transactional
    public void saveHourlyCandle(HourlyMarketCandle candle) {
        MarketCandle1hEntity entity = marketCandle1hSpringDataRepository
                .findAllBySymbolIdAndOpenTimeGreaterThanEqualAndOpenTimeLessThanOrderByOpenTimeAsc(
                        candle.symbolId(),
                        candle.openTime(),
                        candle.closeTime()
                )
                .stream()
                .findFirst()
                .map(existing -> {
                    existing.apply(candle);
                    return existing;
                })
                .orElseGet(() -> MarketCandle1hEntity.from(candle));

        marketCandle1hSpringDataRepository.save(entity);
    }
}
