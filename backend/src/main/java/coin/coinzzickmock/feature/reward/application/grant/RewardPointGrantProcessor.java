package coin.coinzzickmock.feature.reward.application.grant;

import coin.coinzzickmock.feature.reward.application.command.GrantProfitPointCommand;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.result.RewardPointResult;
import coin.coinzzickmock.feature.reward.domain.RewardPointPolicy;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class RewardPointGrantProcessor {
    private final RewardPointPolicy rewardPointPolicy;
    private final RewardPointRepository rewardPointRepository;


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
