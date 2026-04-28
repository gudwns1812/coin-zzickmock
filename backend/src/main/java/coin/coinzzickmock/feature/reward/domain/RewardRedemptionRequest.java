package coin.coinzzickmock.feature.reward.domain;

import java.time.Instant;

public record RewardRedemptionRequest(
        String requestId,
        String memberId,
        Long shopItemId,
        String itemCode,
        String itemName,
        int itemPrice,
        int pointAmount,
        String submittedPhoneNumber,
        String normalizedPhoneNumber,
        RewardRedemptionStatus status,
        Instant requestedAt,
        Instant sentAt,
        Instant cancelledAt,
        String adminMemberId,
        String adminMemo
) {
    public static RewardRedemptionRequest pending(
            String requestId,
            String memberId,
            RewardShopItem item,
            RewardPhoneNumber phoneNumber,
            Instant requestedAt
    ) {
        return new RewardRedemptionRequest(
                requestId,
                memberId,
                item.id(),
                item.code(),
                item.name(),
                item.price(),
                item.price(),
                phoneNumber.submitted(),
                phoneNumber.normalized(),
                RewardRedemptionStatus.PENDING,
                requestedAt,
                null,
                null,
                null,
                null
        );
    }

    public RewardRedemptionRequest {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("요청 ID는 필수입니다.");
        }
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }
        if (shopItemId == null) {
            throw new IllegalArgumentException("상점 상품 ID는 필수입니다.");
        }
        if (pointAmount <= 0) {
            throw new IllegalArgumentException("요청 포인트는 0보다 커야 합니다.");
        }
        if (status == null) {
            throw new IllegalArgumentException("교환권 요청 상태는 필수입니다.");
        }
    }

    public RewardRedemptionRequest markSent(String adminMemberId, String adminMemo, Instant sentAt) {
        requirePending();
        return new RewardRedemptionRequest(
                requestId,
                memberId,
                shopItemId,
                itemCode,
                itemName,
                itemPrice,
                pointAmount,
                submittedPhoneNumber,
                normalizedPhoneNumber,
                RewardRedemptionStatus.SENT,
                requestedAt,
                sentAt,
                cancelledAt,
                adminMemberId,
                adminMemo
        );
    }

    public RewardRedemptionRequest cancelRefunded(String adminMemberId, String adminMemo, Instant cancelledAt) {
        requirePending();
        return new RewardRedemptionRequest(
                requestId,
                memberId,
                shopItemId,
                itemCode,
                itemName,
                itemPrice,
                pointAmount,
                submittedPhoneNumber,
                normalizedPhoneNumber,
                RewardRedemptionStatus.CANCELLED_REFUNDED,
                requestedAt,
                sentAt,
                cancelledAt,
                adminMemberId,
                adminMemo
        );
    }

    private void requirePending() {
        if (status != RewardRedemptionStatus.PENDING) {
            throw new IllegalStateException("대기 중인 교환권 요청만 처리할 수 있습니다.");
        }
    }
}
