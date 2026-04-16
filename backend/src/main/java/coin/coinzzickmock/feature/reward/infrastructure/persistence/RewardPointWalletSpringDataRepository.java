package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardPointWalletSpringDataRepository extends JpaRepository<RewardPointWalletJpaEntity, String> {
}
