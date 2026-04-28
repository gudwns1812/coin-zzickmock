package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.common.persistence.AuditableEntity;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionRequest;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionStatus;
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

import java.time.Instant;

@Entity
@Table(name = "reward_redemption_requests")
public class RewardRedemptionRequestEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, length = 64)
    private String requestId;

    @Column(name = "member_id", nullable = false, length = 64)
    private String memberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_item_id", nullable = false)
    private RewardShopItemEntity shopItem;

    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "item_price", nullable = false)
    private int itemPrice;

    @Column(name = "point_amount", nullable = false)
    private int pointAmount;

    @Column(name = "submitted_phone_number", nullable = false, length = 30)
    private String submittedPhoneNumber;

    @Column(name = "normalized_phone_number", nullable = false, length = 11)
    private String normalizedPhoneNumber;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "admin_member_id", length = 64)
    private String adminMemberId;

    @Column(name = "admin_memo", length = 500)
    private String adminMemo;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected RewardRedemptionRequestEntity() {
    }

    public static RewardRedemptionRequestEntity from(RewardRedemptionRequest request, RewardShopItemEntity shopItem) {
        RewardRedemptionRequestEntity entity = new RewardRedemptionRequestEntity();
        entity.apply(request, shopItem);
        return entity;
    }

    public void apply(RewardRedemptionRequest request, RewardShopItemEntity shopItem) {
        this.requestId = request.requestId();
        this.memberId = request.memberId();
        this.shopItem = shopItem;
        this.itemCode = request.itemCode();
        this.itemName = request.itemName();
        this.itemPrice = request.itemPrice();
        this.pointAmount = request.pointAmount();
        this.submittedPhoneNumber = request.submittedPhoneNumber();
        this.normalizedPhoneNumber = request.normalizedPhoneNumber();
        this.status = request.status().name();
        this.requestedAt = request.requestedAt();
        this.sentAt = request.sentAt();
        this.cancelledAt = request.cancelledAt();
        this.adminMemberId = request.adminMemberId();
        this.adminMemo = request.adminMemo();
    }

    public RewardRedemptionRequest toDomain() {
        return new RewardRedemptionRequest(
                requestId,
                memberId,
                shopItem.getId(),
                itemCode,
                itemName,
                itemPrice,
                pointAmount,
                submittedPhoneNumber,
                normalizedPhoneNumber,
                RewardRedemptionStatus.valueOf(status),
                requestedAt,
                sentAt,
                cancelledAt,
                adminMemberId,
                adminMemo
        );
    }

    public Long getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getMemberId() {
        return memberId;
    }

    public RewardShopItemEntity getShopItem() {
        return shopItem;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public int getItemPrice() {
        return itemPrice;
    }

    public int getPointAmount() {
        return pointAmount;
    }

    public String getSubmittedPhoneNumber() {
        return submittedPhoneNumber;
    }

    public String getNormalizedPhoneNumber() {
        return normalizedPhoneNumber;
    }

    public String getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public String getAdminMemberId() {
        return adminMemberId;
    }

    public String getAdminMemo() {
        return adminMemo;
    }

    public long getVersion() {
        return version;
    }
}
