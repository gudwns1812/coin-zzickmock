package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.feature.reward.application.repository.RewardPointHistoryRepository;
import coin.coinzzickmock.feature.reward.application.result.RewardPointHistoryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetRewardPointHistoryService {
    private final RewardPointHistoryRepository rewardPointHistoryRepository;

    @Transactional(readOnly = true)
    public List<RewardPointHistoryResult> get(Long memberId) {
        return rewardPointHistoryRepository.findByMemberId(memberId).stream()
                .map(RewardPointHistoryResult::from)
                .toList();
    }
}
