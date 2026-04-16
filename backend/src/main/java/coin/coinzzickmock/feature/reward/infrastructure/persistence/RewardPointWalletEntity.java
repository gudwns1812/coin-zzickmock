package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "reward_point_wallets")
public class RewardPointWalletEntity extends AuditableEntity {
    @Id
    @Column(name = "member_id", nullable = false, length = 64)
    private String memberId;

    @Column(name = "reward_point", nullable = false, precision = 19, scale = 2)
    private BigDecimal rewardPoint;

    protected RewardPointWalletEntity() {
    }

    public static RewardPointWalletEntity from(RewardPointWallet rewardPointWallet) {
        RewardPointWalletEntity entity = new RewardPointWalletEntity();
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
