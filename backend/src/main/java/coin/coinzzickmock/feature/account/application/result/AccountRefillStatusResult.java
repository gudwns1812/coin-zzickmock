package coin.coinzzickmock.feature.account.application.result;

import java.time.Instant;

public record AccountRefillStatusResult(
        int remainingCount,
        boolean refillable,
        String disabledReason,
        double targetWalletBalance,
        double targetAvailableMargin,
        Instant nextResetAt
) {
}
