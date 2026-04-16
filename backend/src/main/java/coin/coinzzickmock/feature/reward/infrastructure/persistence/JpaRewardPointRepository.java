package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class JpaRewardPointRepository implements RewardPointRepository {
    private final RewardPointWalletSpringDataRepository rewardPointWalletSpringDataRepository;

    public JpaRewardPointRepository(RewardPointWalletSpringDataRepository rewardPointWalletSpringDataRepository) {
        this.rewardPointWalletSpringDataRepository = rewardPointWalletSpringDataRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RewardPointWallet> findByMemberId(String memberId) {
        return rewardPointWalletSpringDataRepository.findById(memberId)
                .map(RewardPointWalletJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public RewardPointWallet save(RewardPointWallet rewardPointWallet) {
        RewardPointWalletJpaEntity entity = rewardPointWalletSpringDataRepository.findById(rewardPointWallet.memberId())
                .map(existing -> {
                    existing.apply(rewardPointWallet);
                    return existing;
                })
                .orElseGet(() -> RewardPointWalletJpaEntity.from(rewardPointWallet));
        return rewardPointWalletSpringDataRepository.save(entity).toDomain();
    }
}
