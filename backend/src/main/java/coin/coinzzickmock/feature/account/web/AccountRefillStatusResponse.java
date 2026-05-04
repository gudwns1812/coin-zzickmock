package coin.coinzzickmock.feature.account.web;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountRefillStatusResponse(
        int remainingCount,
        boolean refillable,
        String disabledReason,
        BigDecimal targetWalletBalance,
        BigDecimal targetAvailableMargin,
        Instant nextResetAt
) {
}
