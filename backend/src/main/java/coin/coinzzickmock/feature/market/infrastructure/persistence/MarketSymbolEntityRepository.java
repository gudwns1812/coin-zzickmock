package coin.coinzzickmock.feature.market.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketSymbolEntityRepository extends JpaRepository<MarketSymbolEntity, Long> {
    List<MarketSymbolEntity> findAllBySymbolIn(Collection<String> symbols);
}
