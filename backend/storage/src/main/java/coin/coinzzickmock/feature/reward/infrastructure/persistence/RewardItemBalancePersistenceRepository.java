package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.feature.reward.application.repository.RewardItemBalanceRepository;
import coin.coinzzickmock.feature.reward.domain.RewardItemBalance;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class RewardItemBalancePersistenceRepository implements RewardItemBalanceRepository {
    private final RewardItemBalanceEntityRepository rewardItemBalanceEntityRepository;
    private final RewardShopItemEntityRepository rewardShopItemEntityRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<RewardItemBalance> findByMemberIdAndShopItemId(Long memberId, Long shopItemId) {
        return rewardItemBalanceEntityRepository.findByMemberIdAndShopItem_Id(memberId, shopItemId)
                .map(RewardItemBalanceEntity::toDomain);
    }

    @Override
    @Transactional
    public Optional<RewardItemBalance> findByMemberIdAndShopItemIdForUpdate(Long memberId, Long shopItemId) {
        return rewardItemBalanceEntityRepository.findWithLockingByMemberIdAndShopItem_Id(memberId, shopItemId)
                .map(RewardItemBalanceEntity::toDomain);
    }

    @Override
    @Transactional
    public RewardItemBalance save(RewardItemBalance balance) {
        RewardShopItemEntity shopItem = rewardShopItemEntityRepository.findById(balance.shopItemId()).orElseThrow();
        RewardItemBalanceEntity entity = balance.id() == null
                ? RewardItemBalanceEntity.from(balance, shopItem)
                : rewardItemBalanceEntityRepository.findById(balance.id())
                .map(existing -> {
                    existing.apply(balance, shopItem);
                    return existing;
                })
                .orElseGet(() -> RewardItemBalanceEntity.from(balance, shopItem));
        return rewardItemBalanceEntityRepository.save(entity).toDomain();
    }
}
