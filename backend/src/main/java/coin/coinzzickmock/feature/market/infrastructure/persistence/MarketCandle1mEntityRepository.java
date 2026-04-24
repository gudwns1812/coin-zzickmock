package coin.coinzzickmock.feature.market.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketCandle1mEntityRepository extends JpaRepository<MarketCandle1mEntity, Long> {
    Optional<MarketCandle1mEntity> findBySymbolIdAndOpenTime(Long symbolId, Instant openTime);

    Optional<MarketCandle1mEntity> findTopBySymbolIdOrderByOpenTimeDesc(Long symbolId);

    Optional<MarketCandle1mEntity> findTopBySymbolIdAndOpenTimeLessThanOrderByOpenTimeDesc(Long symbolId, Instant beforeExclusive);

    List<MarketCandle1mEntity> findAllBySymbolIdAndOpenTimeGreaterThanEqualAndOpenTimeLessThanOrderByOpenTimeAsc(
            Long symbolId,
            Instant fromInclusive,
            Instant toExclusive
    );
}
