package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface RewardShopItemEntityRepository extends JpaRepository<RewardShopItemEntity, Long> {
    List<RewardShopItemEntity> findAllByOrderBySortOrderAscCodeAsc();

    List<RewardShopItemEntity> findByActiveTrueOrderBySortOrderAscCodeAsc();

    Optional<RewardShopItemEntity> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RewardShopItemEntity> findWithLockingByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RewardShopItemEntity> findWithLockingById(Long id);
}
