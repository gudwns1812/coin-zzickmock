package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.reward.domain.RewardShopPurchase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "reward_shop_purchases")
public class RewardShopPurchaseEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_id", nullable = false, unique = true, length = 64)
    private String purchaseId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_item_id", nullable = false)
    private RewardShopItemEntity shopItem;

    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType;

    @Column(name = "item_price", nullable = false)
    private int itemPrice;

    @Column(name = "point_amount", nullable = false)
    private int pointAmount;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "purchased_at", nullable = false)
    private Instant purchasedAt;

    protected RewardShopPurchaseEntity() {
    }

    public static RewardShopPurchaseEntity from(RewardShopPurchase purchase, RewardShopItemEntity shopItem) {
        RewardShopPurchaseEntity entity = new RewardShopPurchaseEntity();
        entity.purchaseId = purchase.purchaseId();
        entity.memberId = purchase.memberId();
        entity.shopItem = shopItem;
        entity.itemCode = purchase.itemCode();
        entity.itemName = purchase.itemName();
        entity.itemType = purchase.itemType();
        entity.itemPrice = purchase.itemPrice();
        entity.pointAmount = purchase.pointAmount();
        entity.quantity = purchase.quantity();
        entity.purchasedAt = purchase.purchasedAt();
        return entity;
    }

    public RewardShopPurchase toDomain() {
        return new RewardShopPurchase(
                purchaseId,
                memberId,
                shopItem.getId(),
                itemCode,
                itemName,
                itemType,
                itemPrice,
                pointAmount,
                quantity,
                purchasedAt
        );
    }
}
