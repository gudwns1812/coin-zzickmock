package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.reward.domain.RewardItemBalance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "reward_item_balances")
public class RewardItemBalanceEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_item_id", nullable = false)
    private RewardShopItemEntity shopItem;

    @Column(name = "remaining_quantity", nullable = false)
    private int remainingQuantity;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected RewardItemBalanceEntity() {
    }

    private RewardItemBalanceEntity(Long memberId, RewardShopItemEntity shopItem, int remainingQuantity) {
        applyValues(memberId, shopItem, remainingQuantity);
    }

    public static RewardItemBalanceEntity from(RewardItemBalance balance, RewardShopItemEntity shopItem) {
        return new RewardItemBalanceEntity(balance.memberId(), shopItem, balance.remainingQuantity());
    }

    public void apply(RewardItemBalance balance, RewardShopItemEntity shopItem) {
        applyValues(balance.memberId(), shopItem, balance.remainingQuantity());
    }

    private void applyValues(Long memberId, RewardShopItemEntity shopItem, int remainingQuantity) {
        if (memberId == null || shopItem == null || remainingQuantity < 0) {
            throw new IllegalArgumentException("item balance must have member, item, and non-negative quantity");
        }
        this.memberId = memberId;
        this.shopItem = shopItem;
        this.remainingQuantity = remainingQuantity;
    }

    public RewardItemBalance toDomain() {
        return new RewardItemBalance(id, memberId, shopItem.getId(), remainingQuantity);
    }
}
