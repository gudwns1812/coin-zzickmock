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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reward_redemption_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RewardRedemptionRequestEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, length = 64)
    private String requestId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

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

    @Column(name = "admin_member_id")
    private Long adminMemberId;

    @Column(name = "admin_memo", length = 500)
    private String adminMemo;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

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

    public void approvePending(Long adminMemberId, String adminMemo, Instant approvedAt) {
        requirePendingTransition();
        this.status = RewardRedemptionStatus.APPROVED.name();
        this.adminMemberId = adminMemberId;
        this.adminMemo = adminMemo;
        this.sentAt = approvedAt;
    }

    public void rejectPending(Long adminMemberId, String adminMemo, Instant rejectedAt) {
        requirePendingTransition();
        this.status = RewardRedemptionStatus.REJECTED.name();
        this.adminMemberId = adminMemberId;
        this.adminMemo = adminMemo;
        this.cancelledAt = rejectedAt;
    }

    public void cancelPending(Instant cancelledAt) {
        requirePendingTransition();
        this.status = RewardRedemptionStatus.CANCELLED.name();
        this.cancelledAt = cancelledAt;
    }

    public boolean isPending() {
        return RewardRedemptionStatus.PENDING.name().equals(status);
    }

    private void requirePendingTransition() {
        if (!isPending()) {
            throw new IllegalStateException("대기 중인 교환권 요청만 처리할 수 있습니다. status=" + status);
        }
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

}
