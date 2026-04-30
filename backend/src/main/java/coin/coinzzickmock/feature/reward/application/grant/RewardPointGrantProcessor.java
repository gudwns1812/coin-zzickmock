package coin.coinzzickmock.feature.reward.application.grant;

import coin.coinzzickmock.feature.reward.application.command.GrantProfitPointCommand;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointHistoryRepository;
import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.application.result.RewardPointResult;
import coin.coinzzickmock.feature.reward.domain.RewardPointHistory;
import coin.coinzzickmock.feature.reward.domain.RewardPointPolicy;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class RewardPointGrantProcessor {
    private final RewardPointPolicy rewardPointPolicy;
    private final RewardPointRepository rewardPointRepository;
    private final RewardPointHistoryRepository rewardPointHistoryRepository;

    @Autowired
    public RewardPointGrantProcessor(
            RewardPointPolicy rewardPointPolicy,
            RewardPointRepository rewardPointRepository,
            RewardPointHistoryRepository rewardPointHistoryRepository
    ) {
        this.rewardPointPolicy = rewardPointPolicy;
        this.rewardPointRepository = rewardPointRepository;
        this.rewardPointHistoryRepository = rewardPointHistoryRepository;
    }

    /**
     * Test-only compatibility path for older unit tests that do not assert history persistence.
     * Production wiring must use the three-argument constructor.
     */
    @Deprecated
    public RewardPointGrantProcessor(
            RewardPointPolicy rewardPointPolicy,
            RewardPointRepository rewardPointRepository
    ) {
        this(rewardPointPolicy, rewardPointRepository, discardingHistoryRepository());
    }

    private static RewardPointHistoryRepository discardingHistoryRepository() {
        return new RewardPointHistoryRepository() {
            @Override
            public RewardPointHistory save(RewardPointHistory history) {
                return history;
            }

            @Override
            public List<RewardPointHistory> findByMemberId(Long memberId) {
                return List.of();
            }
        };
    }

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
        rewardPointHistoryRepository.save(RewardPointHistory.grant(
                command.memberId(),
                grantedPoint,
                updated.rewardPoint(),
                null
        ));
        return new RewardPointResult(updated.rewardPoint(), rewardPointPolicy.tierLabel(grantedPoint));
    }
}
