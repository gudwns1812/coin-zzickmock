package coin.coinzzickmock.feature.market.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketCandle1hEntityRepository extends JpaRepository<MarketCandle1hEntity, Long> {
    Optional<MarketCandle1hEntity> findBySymbolIdAndOpenTime(Long symbolId, LocalDateTime openTime);

    Optional<MarketCandle1hEntity> findTopBySymbolIdOrderByOpenTimeDesc(Long symbolId);

    Optional<MarketCandle1hEntity> findTopBySymbolIdAndOpenTimeLessThanOrderByOpenTimeDesc(Long symbolId, LocalDateTime beforeExclusive);

    List<MarketCandle1hEntity> findAllBySymbolIdAndOpenTimeGreaterThanEqualAndOpenTimeLessThanOrderByOpenTimeAsc(
            Long symbolId,
            LocalDateTime fromInclusive,
            LocalDateTime toExclusive
    );
}
