package coin.coinzzickmock.feature.market.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketCandle1hEntityRepository extends JpaRepository<MarketCandle1hEntity, Long> {
    Optional<MarketCandle1hEntity> findBySymbolIdAndOpenTime(Long symbolId, Instant openTime);

    List<MarketCandle1hEntity> findAllBySymbolIdAndOpenTimeGreaterThanEqualAndOpenTimeLessThanOrderByOpenTimeAsc(
            Long symbolId,
            Instant fromInclusive,
            Instant toExclusive
    );
}
