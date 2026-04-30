package coin.coinzzickmock.feature.account.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TradingAccountEntityRepository extends JpaRepository<TradingAccountEntity, Long> {
    void deleteAllByMemberId(Long memberId);
}
