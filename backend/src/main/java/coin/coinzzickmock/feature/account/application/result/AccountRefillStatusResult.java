package coin.coinzzickmock.feature.account.application.result;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountRefillStatusResult(
        int remainingCount,
        boolean refillable,
        String disabledReason,
        BigDecimal targetWalletBalance,
        BigDecimal targetAvailableMargin,
        Instant nextResetAt
) {
}
