package coin.coinzzickmock.feature.reward.application.repository;

import coin.coinzzickmock.feature.reward.domain.RewardItemBalance;
import java.util.Optional;

public interface RewardItemBalanceRepository {
    Optional<RewardItemBalance> findByMemberIdAndShopItemId(Long memberId, Long shopItemId);

    Optional<RewardItemBalance> findByMemberIdAndShopItemIdForUpdate(Long memberId, Long shopItemId);

    RewardItemBalance save(RewardItemBalance balance);
}
