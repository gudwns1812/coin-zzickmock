package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "reward_point_wallets")
public class RewardPointWalletJpaEntity {
    @Id
    @Column(name = "member_id", nullable = false, length = 64)
    private String memberId;

    @Column(name = "reward_point", nullable = false, precision = 19, scale = 2)
    private BigDecimal rewardPoint;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RewardPointWalletJpaEntity() {
    }

    public static RewardPointWalletJpaEntity from(RewardPointWallet rewardPointWallet) {
        RewardPointWalletJpaEntity entity = new RewardPointWalletJpaEntity();
        entity.memberId = rewardPointWallet.memberId();
        entity.rewardPoint = BigDecimal.valueOf(rewardPointWallet.rewardPoint());
        return entity;
    }

    public void apply(RewardPointWallet rewardPointWallet) {
        this.rewardPoint = BigDecimal.valueOf(rewardPointWallet.rewardPoint());
    }

    public RewardPointWallet toDomain() {
        return new RewardPointWallet(memberId, rewardPoint.doubleValue());
    }
}
