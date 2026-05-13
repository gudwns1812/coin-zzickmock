package coin.coinzzickmock.feature.reward.application.repository;

import coin.coinzzickmock.feature.reward.domain.RewardShopMemberItemUsage;

import java.util.Optional;

public interface RewardShopMemberItemUsageRepository {
    Optional<RewardShopMemberItemUsage> findByMemberIdAndShopItemId(Long memberId, Long shopItemId);

    Optional<RewardShopMemberItemUsage> findByMemberIdAndShopItemIdForUpdate(Long memberId, Long shopItemId);

    RewardShopMemberItemUsage save(RewardShopMemberItemUsage usage);
}
