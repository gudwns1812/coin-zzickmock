package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.feature.reward.application.repository.RewardShopHistoryRepository;
import coin.coinzzickmock.feature.reward.application.result.RewardShopHistoryResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetRewardShopHistoryService {
    private final RewardShopHistoryRepository rewardShopHistoryRepository;

    @Transactional(readOnly = true)
    public List<RewardShopHistoryResult> get(Long memberId) {
        return rewardShopHistoryRepository.findByMemberId(memberId);
    }
}
