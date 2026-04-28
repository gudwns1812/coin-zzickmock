package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.feature.reward.application.repository.RewardRedemptionRequestRepository;
import coin.coinzzickmock.feature.reward.application.result.RewardRedemptionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetRewardRedemptionHistoryService {
    private final RewardRedemptionRequestRepository rewardRedemptionRequestRepository;

    @Transactional(readOnly = true)
    public List<RewardRedemptionResult> get(String memberId) {
        return rewardRedemptionRequestRepository.findByMemberId(memberId).stream()
                .map(RewardRedemptionResult::from)
                .toList();
    }
}
