package coin.coinzzickmock.feature.reward.domain;

public record RewardPointHistory(
        String memberId,
        RewardPointHistoryType historyType,
        int amount,
        int balanceAfter,
        String sourceType,
        String sourceReference
) {
    public RewardPointHistory {
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }
        if (historyType == null) {
            throw new IllegalArgumentException("포인트 이력 타입은 필수입니다.");
        }
        if (amount == 0) {
            throw new IllegalArgumentException("포인트 이력 금액은 0일 수 없습니다.");
        }
        if (balanceAfter < 0) {
            throw new IllegalArgumentException("포인트 잔액은 음수일 수 없습니다.");
        }
    }

    public static RewardPointHistory grant(String memberId, int amount, int balanceAfter, String sourceReference) {
        if (amount <= 0) {
            throw new IllegalArgumentException("적립 포인트는 0보다 커야 합니다.");
        }
        return new RewardPointHistory(
                memberId,
                RewardPointHistoryType.GRANT,
                amount,
                balanceAfter,
                "POSITION_CLOSE_PROFIT",
                sourceReference
        );
    }
}
