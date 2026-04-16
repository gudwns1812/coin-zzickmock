package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.result.RewardPointResult;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetRewardPointService {
    private final RewardPointRepository rewardPointRepository;

    public GetRewardPointService(RewardPointRepository rewardPointRepository) {
        this.rewardPointRepository = rewardPointRepository;
    }

    @Transactional(readOnly = true)
    public RewardPointResult get(String memberId) {
        RewardPointWallet wallet = rewardPointRepository.findByMemberId(memberId)
                .orElse(new RewardPointWallet(memberId, 0));
        return new RewardPointResult(wallet.rewardPoint(), "POINT_WALLET");
    }
}
