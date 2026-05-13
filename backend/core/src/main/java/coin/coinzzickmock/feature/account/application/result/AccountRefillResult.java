package coin.coinzzickmock.feature.account.application.result;

import coin.coinzzickmock.feature.account.domain.TradingAccount;
import java.math.BigDecimal;

public record AccountRefillResult(
        BigDecimal walletBalance,
        BigDecimal availableMargin,
        int remainingCount
) {
    public static AccountRefillResult from(TradingAccount account, int remainingCount) {
        return new AccountRefillResult(
                BigDecimal.valueOf(account.walletBalance()),
                BigDecimal.valueOf(account.availableMargin()),
                remainingCount
        );
    }
}
