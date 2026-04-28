package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
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
@Table(name = "reward_shop_member_item_usages")
public class RewardShopMemberItemUsageEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, length = 64)
    private String memberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_item_id", nullable = false)
    private RewardShopItemEntity shopItem;

    @Column(name = "purchase_count", nullable = false)
    private int purchaseCount;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected RewardShopMemberItemUsageEntity() {
    }

    public RewardShopMemberItemUsageEntity(String memberId, RewardShopItemEntity shopItem, int purchaseCount) {
        if (purchaseCount < 0) {
            throw new IllegalArgumentException("구매 수량은 음수일 수 없습니다.");
        }
        this.memberId = memberId;
        this.shopItem = shopItem;
        this.purchaseCount = purchaseCount;
    }

    public void incrementPurchaseCount() {
        purchaseCount++;
    }

    public void decrementPurchaseCount() {
        if (purchaseCount == 0) {
            throw new IllegalStateException("구매 수량은 음수로 복구할 수 없습니다.");
        }
        purchaseCount--;
    }

    public Long getId() {
        return id;
    }

    public String getMemberId() {
        return memberId;
    }

    public RewardShopItemEntity getShopItem() {
        return shopItem;
    }

    public int getPurchaseCount() {
        return purchaseCount;
    }

    public long getVersion() {
        return version;
    }
}
