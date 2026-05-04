package coin.coinzzickmock.feature.account.web;

import java.time.Instant;

public record AccountRefillStatusResponse(
        int remainingCount,
        boolean refillable,
        String disabledReason,
        double targetWalletBalance,
        double targetAvailableMargin,
        Instant nextResetAt
) {
}
