package coin.coinzzickmock.feature.reward.domain;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;

public record RewardPhoneNumber(
        String submitted,
        String normalized
) {
    public static RewardPhoneNumber from(String input) {
        if (input == null || input.isBlank()) {
            throw invalid();
        }
        String trimmed = input.trim();
        if (!trimmed.matches("[0-9\\-]+")) {
            throw invalid();
        }
        String normalized = trimmed.replace("-", "");
        if (normalized.length() < 10 || normalized.length() > 11) {
            throw invalid();
        }
        return new RewardPhoneNumber(trimmed, normalized);
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
