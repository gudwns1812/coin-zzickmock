package coin.coinzzickmock.feature.reward.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record RewardPhoneNumber(
        String submitted,
        String normalized
) {
    public static RewardPhoneNumber from(String input) {
        if (input == null || input.isBlank()) {
            throw invalid("휴대폰 번호는 필수입니다.");
        }
        String trimmed = input.trim();
        if (!trimmed.matches("[0-9\\-]+")) {
            throw invalid("휴대폰 번호는 숫자와 하이픈만 입력할 수 있습니다.");
        }
        String normalized = trimmed.replace("-", "");
        if (normalized.length() < 10 || normalized.length() > 11) {
            throw invalid("휴대폰 번호는 10~11자리여야 합니다.");
        }
        return new RewardPhoneNumber(trimmed, normalized);
    }

    private static CoreException invalid(String message) {
        return new CoreException(ErrorCode.INVALID_REQUEST, message);
    }
}
