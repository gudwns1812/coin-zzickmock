package coin.coinzzickmock.feature.account.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TradingAccountSpringDataRepository extends JpaRepository<TradingAccountEntity, String> {
}
