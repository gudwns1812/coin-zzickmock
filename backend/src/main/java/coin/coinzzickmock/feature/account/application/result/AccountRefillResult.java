package coin.coinzzickmock.feature.account.application.result;

import java.math.BigDecimal;

public record AccountRefillResult(
        BigDecimal walletBalance,
        BigDecimal availableMargin,
        int remainingCount
) {
}
