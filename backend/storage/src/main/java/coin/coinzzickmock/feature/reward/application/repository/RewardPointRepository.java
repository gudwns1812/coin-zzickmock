package coin.coinzzickmock.feature.reward.application.repository;

import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;

import java.util.Optional;

public interface RewardPointRepository {
    Optional<RewardPointWallet> findByMemberId(Long memberId);

    Optional<RewardPointWallet> findByMemberIdForUpdate(Long memberId);

    RewardPointWallet save(RewardPointWallet rewardPointWallet);
}
