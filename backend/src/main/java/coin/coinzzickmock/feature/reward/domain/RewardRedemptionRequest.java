package coin.coinzzickmock.feature.reward.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.time.Instant;

public record RewardRedemptionRequest(
        String requestId,
        Long memberId,
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
        Long adminMemberId,
        String adminMemo
) {
    public static RewardRedemptionRequest pending(
            String requestId,
            Long memberId,
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
            throw invalid("요청 ID는 필수입니다.");
        }
        if (memberId == null) {
            throw invalid("회원 ID는 필수입니다.");
        }
        if (shopItemId == null) {
            throw invalid("상점 상품 ID는 필수입니다.");
        }
        if (pointAmount <= 0) {
            throw invalid("요청 포인트는 0보다 커야 합니다.");
        }
        if (status == null) {
            throw invalid("교환권 요청 상태는 필수입니다.");
        }
    }

    public RewardRedemptionRequest markApproved(Long adminMemberId, String adminMemo, Instant approvedAt) {
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
                RewardRedemptionStatus.APPROVED,
                requestedAt,
                approvedAt,
                cancelledAt,
                adminMemberId,
                adminMemo
        );
    }

    public RewardRedemptionRequest rejectRefunded(Long adminMemberId, String adminMemo, Instant rejectedAt) {
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
                RewardRedemptionStatus.REJECTED,
                requestedAt,
                sentAt,
                rejectedAt,
                adminMemberId,
                adminMemo
        );
    }

    public RewardRedemptionRequest cancelRefunded(Instant cancelledAt) {
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
                RewardRedemptionStatus.CANCELLED,
                requestedAt,
                sentAt,
                cancelledAt,
                adminMemberId,
                adminMemo
        );
    }

    private void requirePending() {
        if (status != RewardRedemptionStatus.PENDING) {
            throw invalid("대기 중인 교환권 요청만 처리할 수 있습니다.");
        }
    }

    private static CoreException invalid(String message) {
        return new CoreException(ErrorCode.INVALID_REQUEST, message);
    }
}
