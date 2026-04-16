package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.feature.reward.application.command.GrantProfitPointCommand;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.result.RewardPointResult;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrantProfitPointService {
    private final RewardPointRepository rewardPointRepository;

    public GrantProfitPointService(RewardPointRepository rewardPointRepository) {
        this.rewardPointRepository = rewardPointRepository;
    }

    @Transactional
    public RewardPointResult grant(GrantProfitPointCommand command) {
        int grantedPoint = pointsFor(command.realizedProfit());
        RewardPointWallet current = rewardPointRepository.findByMemberId(command.memberId())
                .orElse(new RewardPointWallet(command.memberId(), 0));
        RewardPointWallet updated = rewardPointRepository.save(
                new RewardPointWallet(command.memberId(), current.rewardPoint() + grantedPoint)
        );
        return new RewardPointResult(updated.rewardPoint(), tierLabel(grantedPoint));
    }

    public int pointsFor(double realizedProfit) {
        if (realizedProfit <= 0) {
            return 0;
        }
        if (realizedProfit < 100) {
            return 1;
        }
        if (realizedProfit < 500) {
            return 5;
        }
        if (realizedProfit < 1_000) {
            return 20;
        }
        return 50;
    }

    private String tierLabel(int grantedPoint) {
        return switch (grantedPoint) {
            case 0 -> "NONE";
            case 1 -> "SMALL_PROFIT";
            case 5 -> "STEADY_PROFIT";
            case 20 -> "STRONG_PROFIT";
            default -> "HIGH_PROFIT";
        };
    }
}
