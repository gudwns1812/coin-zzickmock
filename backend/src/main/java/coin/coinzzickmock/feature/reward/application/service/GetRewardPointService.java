package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.result.RewardPointResult;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetRewardPointService {
    private final RewardPointRepository rewardPointRepository;

    @Transactional(readOnly = true)
    public RewardPointResult get(String memberId) {
        RewardPointWallet wallet = rewardPointRepository.findByMemberId(memberId)
                .orElse(RewardPointWallet.empty(memberId));
        return new RewardPointResult(wallet.rewardPoint(), "POINT_WALLET");
    }
}
