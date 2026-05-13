package coin.coinzzickmock.feature.account.application.result;

import coin.coinzzickmock.feature.account.domain.AccountRefillState;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
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
    public static AccountRefillStatusResult from(
            AccountRefillState state,
            String disabledReason,
            Instant nextResetAt
    ) {
        return new AccountRefillStatusResult(
                state.remainingCount(),
                disabledReason == null,
                disabledReason,
                BigDecimal.valueOf(TradingAccount.INITIAL_WALLET_BALANCE),
                BigDecimal.valueOf(TradingAccount.INITIAL_AVAILABLE_MARGIN),
                nextResetAt
        );
    }
}
