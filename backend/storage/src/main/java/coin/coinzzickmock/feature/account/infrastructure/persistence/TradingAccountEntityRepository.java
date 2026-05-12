package coin.coinzzickmock.feature.account.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface TradingAccountEntityRepository extends JpaRepository<TradingAccountEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TradingAccountEntity> findWithLockingByMemberId(Long memberId);

    void deleteAllByMemberId(Long memberId);
}
