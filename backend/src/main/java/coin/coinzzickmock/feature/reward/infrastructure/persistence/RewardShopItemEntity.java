package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.reward.domain.RewardShopItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "reward_shop_items")
public class RewardShopItemEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType;

    @Column(name = "price", nullable = false)
    private int price;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "total_stock")
    private Integer totalStock;

    @Column(name = "sold_quantity", nullable = false)
    private int soldQuantity;

    @Column(name = "per_member_purchase_limit")
    private Integer perMemberPurchaseLimit;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected RewardShopItemEntity() {
    }

    public static RewardShopItemEntity fromDomain(RewardShopItem item) {
        RewardShopItemEntity entity = new RewardShopItemEntity();
        entity.apply(item);
        return entity;
    }

    public void apply(RewardShopItem item) {
        this.code = item.code();
        this.name = item.name();
        this.description = item.description();
        this.itemType = item.itemType();
        this.price = item.price();
        this.active = item.active();
        this.totalStock = item.totalStock();
        this.soldQuantity = item.soldQuantity();
        this.perMemberPurchaseLimit = item.perMemberPurchaseLimit();
        this.sortOrder = item.sortOrder();
    }

    public void updateSoldQuantity(int soldQuantity) {
        RewardShopItem validated = new RewardShopItem(
                id,
                code,
                name,
                description,
                itemType,
                price,
                active,
                totalStock,
                soldQuantity,
                perMemberPurchaseLimit,
                sortOrder
        );
        this.soldQuantity = validated.soldQuantity();
    }

    public Long getId() {
        return id;
    }

    public RewardShopItem toDomain() {
        return new RewardShopItem(
                id,
                code,
                name,
                description,
                itemType,
                price,
                active,
                totalStock,
                soldQuantity,
                perMemberPurchaseLimit,
                sortOrder
        );
    }
}
