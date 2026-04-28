package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface RewardPointWalletEntityRepository extends JpaRepository<RewardPointWalletEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RewardPointWalletEntity> findWithLockingByMemberId(String memberId);

    void deleteAllByMemberId(String memberId);
}
