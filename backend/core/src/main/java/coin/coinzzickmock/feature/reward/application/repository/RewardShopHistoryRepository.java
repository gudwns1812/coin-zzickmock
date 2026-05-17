package coin.coinzzickmock.feature.reward.application.repository;

import coin.coinzzickmock.feature.reward.application.dto.RewardShopHistoryResult;
import java.util.List;

public interface RewardShopHistoryRepository {
    List<RewardShopHistoryResult> findByMemberId(Long memberId);
}
