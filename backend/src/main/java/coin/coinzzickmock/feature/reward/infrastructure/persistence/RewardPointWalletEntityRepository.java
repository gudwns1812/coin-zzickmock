package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardPointWalletEntityRepository extends JpaRepository<RewardPointWalletEntity, String> {
    void deleteAllByMemberId(String memberId);
}
