package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.feature.reward.application.repository.RewardPointRepository;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RewardPointPersistenceRepository implements RewardPointRepository {
    private final RewardPointWalletEntityRepository rewardPointWalletEntityRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<RewardPointWallet> findByMemberId(Long memberId) {
        return rewardPointWalletEntityRepository.findById(memberId)
                .map(RewardPointWalletEntity::toDomain);
    }

    @Override
    @Transactional
    public Optional<RewardPointWallet> findByMemberIdForUpdate(Long memberId) {
        return rewardPointWalletEntityRepository.findWithLockingByMemberId(memberId)
                .map(RewardPointWalletEntity::toDomain);
    }

    @Override
    @Transactional
    public RewardPointWallet save(RewardPointWallet rewardPointWallet) {
        RewardPointWalletEntity entity = rewardPointWalletEntityRepository.findById(rewardPointWallet.memberId())
                .map(existing -> {
                    existing.apply(rewardPointWallet);
                    return existing;
                })
                .orElseGet(() -> RewardPointWalletEntity.from(rewardPointWallet));
        return rewardPointWalletEntityRepository.save(entity).toDomain();
    }
}
