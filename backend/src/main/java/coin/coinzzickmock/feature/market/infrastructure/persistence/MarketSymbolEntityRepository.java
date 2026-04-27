package coin.coinzzickmock.feature.market.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketSymbolEntityRepository extends JpaRepository<MarketSymbolEntity, Long> {
    Optional<MarketSymbolEntity> findBySymbol(String symbol);

    List<MarketSymbolEntity> findAllBySymbolIn(Collection<String> symbols);

    List<MarketSymbolEntity> findAllByOrderByIdAsc();
}
