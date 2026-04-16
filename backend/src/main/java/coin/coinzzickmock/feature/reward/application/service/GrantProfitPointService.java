package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.feature.reward.application.command.GrantProfitPointCommand;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.result.RewardPointResult;
import coin.coinzzickmock.feature.reward.domain.RewardPointPolicy;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrantProfitPointService {
    private final RewardPointPolicy rewardPointPolicy = new RewardPointPolicy();
    private final RewardPointRepository rewardPointRepository;

    public GrantProfitPointService(RewardPointRepository rewardPointRepository) {
        this.rewardPointRepository = rewardPointRepository;
    }

    @Transactional
    public RewardPointResult grant(GrantProfitPointCommand command) {
        int grantedPoint = rewardPointPolicy.pointsFor(command.realizedProfit());
        RewardPointWallet current = rewardPointRepository.findByMemberId(command.memberId())
                .orElse(new RewardPointWallet(command.memberId(), 0));
        RewardPointWallet updated = rewardPointRepository.save(
                new RewardPointWallet(command.memberId(), current.rewardPoint() + grantedPoint)
        );
        return new RewardPointResult(updated.rewardPoint(), rewardPointPolicy.tierLabel(grantedPoint));
    }
}
