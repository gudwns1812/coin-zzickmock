package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.reward.domain.RewardPointWallet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "reward_point_wallets")
public class RewardPointWalletEntity extends AuditableEntity {
    @Id
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "reward_point", nullable = false)
    private int rewardPoint;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected RewardPointWalletEntity() {
    }

    public static RewardPointWalletEntity from(RewardPointWallet rewardPointWallet) {
        RewardPointWalletEntity entity = new RewardPointWalletEntity();
        entity.memberId = rewardPointWallet.memberId();
        entity.rewardPoint = rewardPointWallet.rewardPoint();
        return entity;
    }

    public void apply(RewardPointWallet rewardPointWallet) {
        this.rewardPoint = rewardPointWallet.rewardPoint();
    }

    public RewardPointWallet toDomain() {
        return new RewardPointWallet(memberId, rewardPoint);
    }
}
