package coin.coinzzickmock.feature.account.domain;

import java.time.LocalDate;

public record AccountRefillState(
        Long memberId,
        LocalDate refillDate,
        int remainingCount,
        long version
) {
    public AccountRefillState {
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }
        if (refillDate == null) {
            throw new IllegalArgumentException("리필 기준일은 필수입니다.");
        }
        if (remainingCount < 0) {
            throw new IllegalArgumentException("리필 가능 횟수는 음수일 수 없습니다.");
        }
        if (version < 0) {
            throw new IllegalArgumentException("버전은 음수일 수 없습니다.");
        }
    }

    public static AccountRefillState weekly(Long memberId, LocalDate refillDate) {
        return new AccountRefillState(memberId, refillDate, 1, 0);
    }

    public AccountRefillState withVersion(long version) {
        return new AccountRefillState(memberId, refillDate, remainingCount, version);
    }

    public AccountRefillState addCount(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("추가 리필 횟수는 0보다 커야 합니다.");
        }
        int updatedCount;
        try {
            updatedCount = Math.addExact(remainingCount, count);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "리필 횟수 합계가 허용 범위를 초과합니다. memberId=%d, refillDate=%s"
                            .formatted(memberId, refillDate),
                    exception
            );
        }
        return new AccountRefillState(memberId, refillDate, updatedCount, version);
    }

    public AccountRefillState consumeOne() {
        if (remainingCount <= 0) {
            throw new IllegalStateException("사용 가능한 리필 횟수가 없습니다.");
        }
        return new AccountRefillState(memberId, refillDate, remainingCount - 1, version);
    }
}
