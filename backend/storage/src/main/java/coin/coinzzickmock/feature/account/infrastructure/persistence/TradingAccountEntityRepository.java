package coin.coinzzickmock.feature.account.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Optional;

public interface TradingAccountEntityRepository extends JpaRepository<TradingAccountEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TradingAccountEntity> findWithLockingByMemberId(Long memberId);

    @Query("""
            select account.walletBalance
            from TradingAccountEntity account
            where account.memberId = :memberId
            """)
    Optional<BigDecimal> findWalletBalanceByMemberId(@Param("memberId") Long memberId);

    void deleteAllByMemberId(Long memberId);
}
