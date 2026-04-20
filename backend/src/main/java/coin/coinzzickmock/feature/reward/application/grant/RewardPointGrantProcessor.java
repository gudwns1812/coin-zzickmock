package coin.coinzzickmock.feature.reward.application.grant;

import coin.coinzzickmock.feature.reward.application.command.GrantProfitPointCommand;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.result.RewardPointResult;
import coin.coinzzickmock.feature.reward.domain.RewardPointPolicy;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class RewardPointGrantProcessor {
    private final RewardPointPolicy rewardPointPolicy;
    private final RewardPointRepository rewardPointRepository;

    @Transactional
    public RewardPointResult grant(GrantProfitPointCommand command) {
        int grantedPoint = rewardPointPolicy.pointsFor(command.realizedProfit());
        if (grantedPoint == 0) {
            RewardPointWallet current = rewardPointRepository.findByMemberId(command.memberId())
                    .orElse(RewardPointWallet.empty(command.memberId()));
            return new RewardPointResult(current.rewardPoint(), rewardPointPolicy.tierLabel(grantedPoint));
        }

        RewardPointWallet current = rewardPointRepository.findByMemberId(command.memberId())
                .orElse(RewardPointWallet.empty(command.memberId()));
        RewardPointWallet updated = rewardPointRepository.save(current.grant(grantedPoint));
        return new RewardPointResult(updated.rewardPoint(), rewardPointPolicy.tierLabel(grantedPoint));
    }
}
