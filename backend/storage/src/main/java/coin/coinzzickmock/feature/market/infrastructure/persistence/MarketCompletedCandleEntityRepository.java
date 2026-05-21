package coin.coinzzickmock.feature.market.infrastructure.persistence;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketCompletedCandleEntityRepository extends JpaRepository<MarketCompletedCandleEntity, Long> {
    Optional<MarketCompletedCandleEntity> findBySymbolIdAndIntervalAndOpenTime(
            Long symbolId,
            MarketCandleInterval interval,
            Instant openTime
    );

    Optional<MarketCompletedCandleEntity> findTopBySymbolIdAndIntervalOrderByOpenTimeDesc(
            Long symbolId,
            MarketCandleInterval interval
    );

    Optional<MarketCompletedCandleEntity> findTopBySymbolIdAndIntervalAndOpenTimeLessThanOrderByOpenTimeDesc(
            Long symbolId,
            MarketCandleInterval interval,
            Instant beforeExclusive
    );

    boolean existsBySymbolIdAndInterval(Long symbolId, MarketCandleInterval interval);

    List<MarketCompletedCandleEntity> findAllBySymbolIdAndIntervalAndOpenTimeGreaterThanEqualAndOpenTimeLessThanOrderByOpenTimeAsc(
            Long symbolId,
            MarketCandleInterval interval,
            Instant fromInclusive,
            Instant toExclusive
    );
}
