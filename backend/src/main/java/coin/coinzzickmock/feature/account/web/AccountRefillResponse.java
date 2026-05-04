package coin.coinzzickmock.feature.account.web;

import java.math.BigDecimal;

public record AccountRefillResponse(
        BigDecimal walletBalance,
        BigDecimal availableMargin,
        int remainingCount
) {
}
